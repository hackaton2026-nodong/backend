package com.kworkerharmony.backend.document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kworkerharmony.backend.cases.domain.CaseRepository;
import com.kworkerharmony.backend.cases.entity.Case;
import com.kworkerharmony.backend.document.config.DocumentBlockchainProperties;
import com.kworkerharmony.backend.document.config.DocumentOcrProperties;
import com.kworkerharmony.backend.document.dto.request.AnchorDocumentRequest;
import com.kworkerharmony.backend.document.dto.request.CorrectDocumentExtractionRequest;
import com.kworkerharmony.backend.document.dto.request.CreatePaddleOcrExtractionRequest;
import com.kworkerharmony.backend.document.dto.request.ReceivePaddleOcrResultRequest;
import com.kworkerharmony.backend.document.dto.request.SubmitDocumentSignatureRequest;
import com.kworkerharmony.backend.document.dto.response.DocumentAnalysisResponse;
import com.kworkerharmony.backend.document.dto.response.DocumentAnchorResponse;
import com.kworkerharmony.backend.document.dto.response.DocumentExtractionResponse;
import com.kworkerharmony.backend.document.dto.response.DocumentResponse;
import com.kworkerharmony.backend.document.dto.response.DocumentSignatureRequestResponse;
import com.kworkerharmony.backend.document.dto.response.DocumentSignatureResponse;
import com.kworkerharmony.backend.document.port.DocumentAnchorRelayerPort;
import com.kworkerharmony.backend.document.port.DocumentAnchorRelayerPort.AnchorCommand;
import com.kworkerharmony.backend.document.port.DocumentAnchorRelayerPort.AnchorReceipt;
import com.kworkerharmony.backend.document.port.DocumentAiAnalysisPort;
import com.kworkerharmony.backend.document.port.DocumentAiAnalysisPort.AiAnalysisResult;
import com.kworkerharmony.backend.document.port.DocumentHashPort;
import com.kworkerharmony.backend.document.port.DocumentOcrPort;
import com.kworkerharmony.backend.document.port.DocumentOcrPort.OcrCommand;
import com.kworkerharmony.backend.document.port.FileStoragePort;
import com.kworkerharmony.backend.document.port.FileStoragePort.StoredFile;
import com.kworkerharmony.backend.document.support.DocumentAiPayloadBuilder;
import com.kworkerharmony.backend.document.support.DocumentCrypto;
import com.kworkerharmony.backend.document.support.DocumentTypedDataFactory;
import com.kworkerharmony.backend.document.support.EmploymentContractExtractionPayload;
import com.kworkerharmony.backend.document.support.PaddleOcrEmploymentContractExtractor;
import com.kworkerharmony.backend.enterprise.Enterprise;
import com.kworkerharmony.backend.enterprise.CompanyInviteCode;
import com.kworkerharmony.backend.enterprise.CompanyInviteCodeRepository;
import com.kworkerharmony.backend.enterprise.dto.response.CompanyInviteCodeResponse;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import com.kworkerharmony.backend.global.security.UserPrincipal;
import com.kworkerharmony.backend.user.Role;
import com.kworkerharmony.backend.user.User;
import com.kworkerharmony.backend.user.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final CaseRepository caseRepository;
    private final FileStoragePort fileStoragePort;
    private final DocumentHashPort documentHashPort;
    private final DocumentSignatureRepository documentSignatureRepository;
    private final DocumentAnchorRepository documentAnchorRepository;
    private final DocumentAnalysisResultRepository documentAnalysisResultRepository;
    private final DocumentExtractionRepository documentExtractionRepository;
    private final CompanyInviteCodeRepository companyInviteCodeRepository;
    private final DocumentBlockchainProperties blockchainProperties;
    private final DocumentOcrProperties ocrProperties;
    private final DocumentTypedDataFactory typedDataFactory;
    private final DocumentAnchorRelayerPort anchorRelayerPort;
    private final DocumentAiAnalysisPort documentAiAnalysisPort;
    private final DocumentOcrPort documentOcrPort;
    private final DocumentAiPayloadBuilder documentAiPayloadBuilder;
    private final PaddleOcrEmploymentContractExtractor paddleOcrEmploymentContractExtractor;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocuments(String caseId, UserPrincipal userPrincipal) {
        Case caseEntity = getAccessibleCase(caseId, userPrincipal);
        validateCasePartyAccess(caseEntity, getUser(userPrincipal.getId()));
        return documentRepository.findAllByCaseIdOrderByCreatedAtDesc(caseEntity.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(String documentId, UserPrincipal userPrincipal) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Document not found"));
        validateDocumentAccess(document, userPrincipal);
        return toResponse(document);
    }

    @Transactional
    public DocumentResponse uploadDocument(
            String caseId,
            MultipartFile file,
            DocumentType documentType,
            LocalDate issuedAt,
            LocalDate expiresAt,
            UserPrincipal userPrincipal
    ) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "File is required");
        }

        User user = getUser(userPrincipal.getId());
        Case caseEntity = getAccessibleCase(caseId, userPrincipal);
        validateCasePartyAccess(caseEntity, user);

        Document document = Document.createUploaded(
                user.getId(),
                documentType,
                issuedAt,
                expiresAt
        );
        document.assignToCase(caseEntity.getId());
        document = documentRepository.save(document);

        try {
            StoredFile storedFile = fileStoragePort.store(caseEntity.getId(), document.getId(), file);
            document.markStored(
                    storedFile.originalFileName(),
                    storedFile.storageKey(),
                    storedFile.mimeType(),
                    storedFile.fileSize()
            );
            document.markHashed(documentHashPort.hash(storedFile.absolutePath()));
            createPendingExtraction(document);
            requestOcr(document);
            CompanyInviteCode inviteCode = createWorkerInviteCodeIfNeeded(caseEntity, document);
            return toResponse(document, inviteCode);
        } catch (RuntimeException ex) {
            document.markFailed();
            throw ex;
        }
    }

    @Transactional
    public DocumentSignatureRequestResponse createSignatureRequest(String documentId, UserPrincipal userPrincipal) {
        Document document = getAccessibleDocument(documentId, userPrincipal);
        validateDocumentCanRequestSignature(document);

        User user = getUser(userPrincipal.getId());
        String nonce = DocumentCrypto.randomBytes32Hex();
        LocalDateTime deadline = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(blockchainProperties.signatureTtlSeconds());
        String typedDataHash = typedDataFactory.typedDataHash(blockchainProperties, document, user.getId(), nonce, deadline);

        DocumentSignature signature = documentSignatureRepository.save(new DocumentSignature(
                document.getId(),
                user.getId(),
                blockchainProperties.chainId(),
                blockchainProperties.contractAddress(),
                typedDataHash,
                nonce,
                deadline
        ));
        document.markSignatureRequested();

        return new DocumentSignatureRequestResponse(
                document.getId(),
                blockchainProperties.chainId(),
                typedDataFactory.domain(blockchainProperties),
                typedDataFactory.types(),
                typedDataFactory.message(document, user.getId(), signature.getNonce(), signature.getDeadline()),
                signature.getTypedDataHash()
        );
    }

    @Transactional
    public DocumentSignatureResponse submitSignature(
            String documentId,
            SubmitDocumentSignatureRequest request,
            UserPrincipal userPrincipal
    ) {
        Document document = getAccessibleDocument(documentId, userPrincipal);
        User user = getUser(userPrincipal.getId());
        DocumentSignature signature = documentSignatureRepository
                .findByDocumentIdAndUserIdAndNonce(document.getId(), user.getId(), normalizedHex(request.nonce()))
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Signature request not found"));

        if (signature.getStatus() != DocumentSignatureStatus.REQUESTED) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Signature request is not active");
        }
        if (signature.getDeadline().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            signature.markExpired();
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Signature request expired");
        }
        if (!request.chainId().equals(signature.getChainId())) {
            signature.markRejected();
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Unexpected chain id");
        }
        validateWalletAddress(request.walletAddress());
        validateHexSignature(request.signature());

        String recalculatedHash = typedDataFactory.typedDataHash(
                blockchainProperties,
                document,
                user.getId(),
                signature.getNonce(),
                signature.getDeadline()
        );
        if (!recalculatedHash.equalsIgnoreCase(signature.getTypedDataHash())) {
            signature.markRejected();
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Typed data payload mismatch");
        }

        String signatureHash = DocumentCrypto.bytes32HexFromSha256(normalizedHex(request.signature()));
        signature.markSigned(
                normalizedAddress(request.walletAddress()),
                normalizedHex(request.signature()),
                signatureHash,
                request.typedDataHash() == null ? null : normalizedHex(request.typedDataHash())
        );
        document.markSigned();
        return DocumentSignatureResponse.from(signature);
    }

    @Transactional
    public DocumentAnchorResponse anchorDocument(
            String documentId,
            AnchorDocumentRequest request,
            UserPrincipal userPrincipal
    ) {
        Document document = getAccessibleDocument(documentId, userPrincipal);
        DocumentSignature signature = documentSignatureRepository.findById(request.signatureId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Signature not found"));
        if (!signature.getDocumentId().equals(document.getId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Signature belongs to another document");
        }
        if (signature.getStatus() != DocumentSignatureStatus.SIGNED) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Document signature is not signed");
        }

        DocumentAnchor existing = documentAnchorRepository
                .findFirstByDocumentIdAndStatusInOrderByCreatedAtDesc(
                        document.getId(),
                        List.of(DocumentAnchorStatus.PENDING, DocumentAnchorStatus.ANCHORED)
                )
                .orElse(null);
        if (existing != null) {
            return DocumentAnchorResponse.from(existing);
        }

        DocumentAnchor anchor = documentAnchorRepository.findByDocumentIdAndSignatureId(document.getId(), signature.getId())
                .orElseGet(() -> documentAnchorRepository.save(new DocumentAnchor(
                        document.getId(),
                        signature.getId(),
                        signature.getChainId(),
                        signature.getVerifyingContract(),
                        anchorId(document, signature),
                        DocumentCrypto.ensureBytes32Hex(document.getSha256Hash()),
                        DocumentCrypto.bytes32HexFromSha256(document.getCaseId())
                )));

        document.markAnchorPending();
        try {
            AnchorReceipt receipt = anchorRelayerPort.anchor(new AnchorCommand(
                    anchor.getDocumentHash(),
                    anchor.getCaseIdHash(),
                    signature.getWalletAddress(),
                    signature.getTypedDataHash(),
                    signature.getSignature(),
                    signature.getNonce(),
                    signature.getDeadline().toEpochSecond(ZoneOffset.UTC),
                    signature.getChainId(),
                    signature.getVerifyingContract(),
                    anchor.getAnchorId()
            ));
            anchor.markAnchored(receipt.txHash(), receipt.blockNumber());
            document.markAnchored(receipt.txHash());
        } catch (RuntimeException ex) {
            anchor.markFailed(truncate(ex.getMessage(), 1000));
            document.markAnchorFailed();
        }

        return DocumentAnchorResponse.from(anchor);
    }

    @Transactional(readOnly = true)
    public DocumentAnchorResponse getAnchor(String documentId, UserPrincipal userPrincipal) {
        Document document = getAccessibleDocument(documentId, userPrincipal);
        DocumentAnchor anchor = documentAnchorRepository.findFirstByDocumentIdOrderByCreatedAtDesc(document.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Document anchor not found"));
        return DocumentAnchorResponse.from(anchor);
    }

    @Transactional
    public DocumentAnalysisResponse analyzeDocument(String documentId, UserPrincipal userPrincipal) {
        Document document = getAccessibleDocument(documentId, userPrincipal);
        validateDocumentReadyForExtraction(document);
        DocumentExtraction extraction = documentExtractionRepository.findByDocumentId(document.getId())
                .orElse(null);
        validateExtractionCanBeAnalyzed(extraction);
        DocumentAnalysisResult result = documentAnalysisResultRepository.findByDocumentId(document.getId())
                .orElseGet(() -> documentAnalysisResultRepository.save(new DocumentAnalysisResult(document.getId())));
        try {
            AiAnalysisResult analysisResult = documentAiAnalysisPort.analyze(documentAiPayloadBuilder.build(document, extraction));
            result.markCompleted(
                    analysisResult.inputHash(),
                    analysisResult.analysisResultHash(),
                    analysisResult.summary(),
                    analysisResult.riskFlags(),
                    analysisResult.issueCandidates(),
                    analysisResult.generatedAnalysis(),
                    analysisResult.findings(),
                    analysisResult.fieldFindings(),
                    analysisResult.citations(),
                    analysisResult.recommendedActions(),
                    analysisResult.relatedInstitutions(),
                    analysisResult.caseStatus(),
                    analysisResult.detailJson(),
                    analysisResult.failedReason()
            );
            markAnalyzedWithoutOverwritingOnchainTerminalState(document);
        } catch (RuntimeException ex) {
            result.markFailed(truncate(ex.getMessage(), 1000));
        }
        return DocumentAnalysisResponse.from(result, document, objectMapper);
    }

    @Transactional(readOnly = true)
    public DocumentAnalysisResponse getAnalysis(String documentId, UserPrincipal userPrincipal) {
        Document document = getAccessibleDocument(documentId, userPrincipal);
        DocumentAnalysisResult result = documentAnalysisResultRepository.findByDocumentId(document.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Document analysis not found"));
        return DocumentAnalysisResponse.from(result, document, objectMapper);
    }

    @Transactional
    public DocumentExtractionResponse createPaddleOcrExtraction(
            String documentId,
            CreatePaddleOcrExtractionRequest request,
            UserPrincipal userPrincipal
    ) {
        Document document = getAccessibleDocument(documentId, userPrincipal);
        validateDocumentReadyForExtraction(document);
        if (!DocumentType.EMPLOYMENT_CONTRACT.name().equals(document.getDocumentType())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Only employment contracts are supported");
        }

        DocumentExtraction extraction = applyPaddleOcrExtraction(document, request.ocrResult());
        return DocumentExtractionResponse.from(extraction, objectMapper);
    }

    @Transactional
    public DocumentExtractionResponse receivePaddleOcrResult(
            String documentId,
            ReceivePaddleOcrResultRequest request,
            String callbackToken
    ) {
        validateCallbackToken(callbackToken);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Document not found"));
        validateDocumentReadyForExtraction(document);
        if (!DocumentType.EMPLOYMENT_CONTRACT.name().equals(document.getDocumentType())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Only employment contracts are supported");
        }
        DocumentExtraction extraction = applyPaddleOcrExtraction(document, request.ocrResult());
        return DocumentExtractionResponse.from(extraction, objectMapper);
    }

    @Transactional(readOnly = true)
    public DocumentExtractionResponse getExtraction(String documentId, UserPrincipal userPrincipal) {
        Document document = getAccessibleDocument(documentId, userPrincipal);
        DocumentExtraction extraction = documentExtractionRepository.findByDocumentId(document.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Document extraction not found"));
        return DocumentExtractionResponse.from(extraction, objectMapper);
    }

    @Transactional
    public DocumentExtractionResponse correctExtraction(
            String documentId,
            CorrectDocumentExtractionRequest request,
            UserPrincipal userPrincipal
    ) {
        Document document = getAccessibleDocument(documentId, userPrincipal);
        DocumentExtraction extraction = documentExtractionRepository.findByDocumentId(document.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Document extraction not found"));
        String correctedPayload = canonicalJson(request.correctedPayload());
        validateSanitizedPayload(correctedPayload);
        extraction.markCorrected(correctedPayload, DocumentCrypto.sha256Hex(correctedPayload));
        return DocumentExtractionResponse.from(extraction, objectMapper);
    }

    private DocumentResponse toResponse(Document document) {
        return DocumentResponse.from(document, fileStoragePort.exists(document.getStorageKey()));
    }

    private DocumentResponse toResponse(Document document, CompanyInviteCode inviteCode) {
        return DocumentResponse.from(
                document,
                fileStoragePort.exists(document.getStorageKey()),
                inviteCode == null ? null : CompanyInviteCodeResponse.from(inviteCode)
        );
    }

    private void createPendingExtraction(Document document) {
        if (!DocumentType.EMPLOYMENT_CONTRACT.name().equals(document.getDocumentType())) {
            return;
        }
        documentExtractionRepository.findByDocumentId(document.getId())
                .orElseGet(() -> documentExtractionRepository.save(new DocumentExtraction(
                        document.getId(),
                        PaddleOcrEmploymentContractExtractor.SCHEMA_VERSION,
                        PaddleOcrEmploymentContractExtractor.SOURCE_ENGINE
                )));
    }

    private void requestOcr(Document document) {
        try {
            documentOcrPort.requestOcr(new OcrCommand(
                    document.getId(),
                    document.getCaseId(),
                    document.getDocumentType(),
                    document.getStorageKey(),
                    document.getSha256Hash(),
                    callbackUrl(document.getId())
            ));
        } catch (RuntimeException ex) {
            documentExtractionRepository.findByDocumentId(document.getId())
                    .ifPresent(extraction -> extraction.markFailed(truncate(ex.getMessage(), 1000)));
        }
    }

    private String callbackUrl(String documentId) {
        if (ocrProperties.callbackBaseUrl().isBlank()) {
            return "/api/internal/documents/" + documentId + "/ocr-result";
        }
        return ocrProperties.callbackBaseUrl().replaceAll("/+$", "")
                + "/api/internal/documents/" + documentId + "/ocr-result";
    }

    private CompanyInviteCode createWorkerInviteCodeIfNeeded(Case caseEntity, Document document) {
        if (document.getDocumentType() == null || !document.getDocumentType().equals(DocumentType.EMPLOYMENT_CONTRACT.name())) {
            return null;
        }
        return companyInviteCodeRepository
                .findFirstByCaseIdAndDefaultRoleAndActiveTrueOrderByCreatedAtDesc(caseEntity.getId(), Role.WORKER)
                .filter(inviteCode -> inviteCode.isUsableAt(LocalDateTime.now()))
                .orElseGet(() -> companyInviteCodeRepository.save(new CompanyInviteCode(
                        caseEntity.getEnterprise(),
                        caseEntity.getId(),
                        UUID.randomUUID().toString().replace("-", ""),
                        LocalDateTime.now().plusDays(14),
                        1,
                        0,
                        true,
                        Role.WORKER
                )));
    }

    private DocumentExtraction applyPaddleOcrExtraction(Document document, JsonNode ocrResult) {
        if (ocrResult.path("error").isObject()) {
            return markPaddleOcrFailed(document, ocrResult.path("error"));
        }
        String sourceResultHash = canonicalHash(ocrResult);
        EmploymentContractExtractionPayload payload = paddleOcrEmploymentContractExtractor.extract(ocrResult);
        validateSanitizedPayload(payload.payloadJson());

        DocumentExtraction extraction = documentExtractionRepository.findByDocumentId(document.getId())
                .orElseGet(() -> documentExtractionRepository.save(new DocumentExtraction(
                        document.getId(),
                        PaddleOcrEmploymentContractExtractor.SCHEMA_VERSION,
                        PaddleOcrEmploymentContractExtractor.SOURCE_ENGINE
                )));
        extraction.markExtracted(
                sourceResultHash,
                payload.payloadJson(),
                payload.aiPayloadHash(),
                payload.reviewRequiredReason()
        );
        document.markOcrCompleted();
        document.markStructured();
        return extraction;
    }

    private DocumentExtraction markPaddleOcrFailed(Document document, JsonNode error) {
        String code = error.path("code").asText("OCR_FAILED");
        String message = error.path("message").asText("OCR processing failed");
        DocumentExtraction extraction = documentExtractionRepository.findByDocumentId(document.getId())
                .orElseGet(() -> documentExtractionRepository.save(new DocumentExtraction(
                        document.getId(),
                        PaddleOcrEmploymentContractExtractor.SCHEMA_VERSION,
                        PaddleOcrEmploymentContractExtractor.SOURCE_ENGINE
                )));
        extraction.markFailed(truncate(code + ": " + message, 1000));
        return extraction;
    }

    private Document getAccessibleDocument(String documentId, UserPrincipal userPrincipal) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Document not found"));
        validateDocumentAccess(document, userPrincipal);
        return document;
    }

    private Case getAccessibleCase(String caseId, UserPrincipal userPrincipal) {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Case not found"));
        User user = getUser(userPrincipal.getId());
        validateCompanyAccess(caseEntity, user);
        return caseEntity;
    }

    private void validateDocumentAccess(Document document, UserPrincipal userPrincipal) {
        Case caseEntity = caseRepository.findById(document.getCaseId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Case not found"));
        User user = getUser(userPrincipal.getId());
        validateCompanyAccess(caseEntity, user);
        validateCasePartyAccess(caseEntity, user);
    }

    private void validateDocumentCanRequestSignature(Document document) {
        if (document.getSha256Hash() == null || document.getSha256Hash().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Document hash is required");
        }
        if (!List.of(
                        DocumentStatus.HASHED,
                        DocumentStatus.SIGNATURE_REQUESTED,
                        DocumentStatus.SIGNED,
                        DocumentStatus.OCR_COMPLETED,
                        DocumentStatus.STRUCTURED,
                        DocumentStatus.ANALYZED
                )
                .contains(document.getStatus())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Document is not ready for signature");
        }
    }

    private void validateDocumentReadyForExtraction(Document document) {
        if (document.getSha256Hash() == null || document.getSha256Hash().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Document hash is required");
        }
        if (document.getStorageKey() == null || document.getStorageKey().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Stored document is required");
        }
    }

    private void markAnalyzedWithoutOverwritingOnchainTerminalState(Document document) {
        if (document.getStatus() == DocumentStatus.ANCHORED_ON_CHAIN || document.getStatus() == DocumentStatus.ANCHOR_FAILED) {
            return;
        }
        document.markAnalyzed();
    }

    private void validateExtractionCanBeAnalyzed(DocumentExtraction extraction) {
        if (extraction == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Document extraction is required");
        }
        if (extraction.getStatus() == DocumentExtractionStatus.NEEDS_REVIEW
                || extraction.getStatus() == DocumentExtractionStatus.PENDING
                || extraction.getStatus() == DocumentExtractionStatus.FAILED
                || extraction.getAiPayloadHash() == null
                || extraction.getAiPayloadHash().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Document extraction requires correction");
        }
    }

    private void validateCallbackToken(String callbackToken) {
        if (ocrProperties.callbackToken().isBlank()) {
            return;
        }
        if (callbackToken == null || !ocrProperties.callbackToken().equals(callbackToken)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "Invalid OCR callback token");
        }
    }

    private String canonicalHash(JsonNode value) {
        return DocumentCrypto.sha256Hex(canonicalJson(value));
    }

    private String canonicalJson(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Invalid JSON payload");
        }
    }

    private void validateSanitizedPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Extraction payload is required");
        }
        String lower = payload.toLowerCase(Locale.ROOT);
        if (lower.contains("storagekey")
                || lower.contains("layoutparsingresults")
                || lower.contains("parsing_res_list")
                || lower.contains("block_content")
                || lower.contains("markdown")
                || lower.contains("address")
                || lower.contains("주소")) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Raw OCR fields are not allowed");
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (JsonProcessingException ex) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Invalid extraction payload JSON");
        }
        Queue<JsonNode> nodes = new ArrayDeque<>();
        nodes.add(root);
        while (!nodes.isEmpty()) {
            JsonNode node = nodes.remove();
            if (node.isTextual() && containsSensitiveIdentifier(node.asText())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Sensitive identifiers are not allowed");
            }
            node.elements().forEachRemaining(nodes::add);
        }
    }

    private boolean containsSensitiveIdentifier(String value) {
        return value.matches(".*[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}.*")
                || value.matches(".*01[016789]-?\\d{3,4}-?\\d{4}.*")
                || value.matches(".*0\\d{1,2}-\\d{3,4}-\\d{4}.*")
                || value.matches(".*\\d{3}-\\d{2}-\\d{5}.*");
    }

    private String anchorId(Document document, DocumentSignature signature) {
        String canonical = signature.getChainId()
                + "|" + signature.getVerifyingContract().toLowerCase(Locale.ROOT)
                + "|" + DocumentCrypto.ensureBytes32Hex(document.getSha256Hash()).toLowerCase(Locale.ROOT)
                + "|" + DocumentCrypto.bytes32HexFromSha256(document.getCaseId()).toLowerCase(Locale.ROOT)
                + "|" + signature.getWalletAddress().toLowerCase(Locale.ROOT)
                + "|" + signature.getNonce().toLowerCase(Locale.ROOT);
        return DocumentCrypto.bytes32HexFromSha256(canonical);
    }

    private void validateWalletAddress(String walletAddress) {
        if (walletAddress == null || !walletAddress.matches("^0x[0-9a-fA-F]{40}$")) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Invalid wallet address");
        }
    }

    private void validateHexSignature(String signature) {
        if (signature == null || !signature.matches("^0x[0-9a-fA-F]+$")) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Invalid signature");
        }
    }

    private String normalizedAddress(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private String normalizedHex(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private void validateCompanyAccess(Case caseEntity, User user) {
        Enterprise enterprise = user.getEnterprise();
        if (enterprise == null || !caseEntity.getEnterprise().getId().equals(enterprise.getId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "Case belongs to another company");
        }
    }

    private void validateCasePartyAccess(Case caseEntity, User user) {
        boolean isCaseParty = (caseEntity.getEmployer() != null && caseEntity.getEmployer().getId().equals(user.getId()))
                || (caseEntity.getWorker() != null && caseEntity.getWorker().getId().equals(user.getId()));
        if (!isCaseParty && user.getRole() != Role.ADMIN) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "Document access is limited to case parties");
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
    }
}
