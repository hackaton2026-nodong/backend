package com.kworkerharmony.backend.document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kworkerharmony.backend.cases.domain.CaseRepository;
import com.kworkerharmony.backend.cases.entity.Case;
import com.kworkerharmony.backend.document.config.DocumentBlockchainProperties;
import com.kworkerharmony.backend.document.dto.request.AnchorDocumentRequest;
import com.kworkerharmony.backend.document.dto.request.SubmitDocumentSignatureRequest;
import com.kworkerharmony.backend.document.dto.response.DocumentAnalysisResponse;
import com.kworkerharmony.backend.document.dto.response.DocumentAnchorResponse;
import com.kworkerharmony.backend.document.dto.response.DocumentResponse;
import com.kworkerharmony.backend.document.dto.response.DocumentSignatureRequestResponse;
import com.kworkerharmony.backend.document.dto.response.DocumentSignatureResponse;
import com.kworkerharmony.backend.document.port.DocumentAnchorRelayerPort;
import com.kworkerharmony.backend.document.port.DocumentAnchorRelayerPort.AnchorCommand;
import com.kworkerharmony.backend.document.port.DocumentAnchorRelayerPort.AnchorReceipt;
import com.kworkerharmony.backend.document.port.DocumentAnalysisPort;
import com.kworkerharmony.backend.document.port.DocumentAnalysisPort.AnalysisCommand;
import com.kworkerharmony.backend.document.port.DocumentAnalysisPort.CaseContextPayload;
import com.kworkerharmony.backend.document.port.DocumentAnalysisPort.ChecklistContextPayload;
import com.kworkerharmony.backend.document.port.DocumentAnalysisPort.DocumentPayload;
import com.kworkerharmony.backend.document.port.DocumentAnalysisPort.OutputRequestPayload;
import com.kworkerharmony.backend.document.port.DocumentHashPort;
import com.kworkerharmony.backend.document.port.FileStoragePort;
import com.kworkerharmony.backend.document.port.FileStoragePort.StoredFile;
import com.kworkerharmony.backend.document.support.DocumentCrypto;
import com.kworkerharmony.backend.document.support.DocumentTypedDataFactory;
import com.kworkerharmony.backend.enterprise.Enterprise;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import com.kworkerharmony.backend.global.security.UserPrincipal;
import com.kworkerharmony.backend.user.Role;
import com.kworkerharmony.backend.user.User;
import com.kworkerharmony.backend.user.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
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
    private final DocumentBlockchainProperties blockchainProperties;
    private final DocumentTypedDataFactory typedDataFactory;
    private final DocumentAnchorRelayerPort anchorRelayerPort;
    private final DocumentAnalysisPort documentAnalysisPort;
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
            return toResponse(document);
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
        validateDocumentCanBeAnalyzed(document);
        Case caseEntity = caseRepository.findById(document.getCaseId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Case not found"));
        User user = getUser(userPrincipal.getId());
        DocumentAnalysisResult result = documentAnalysisResultRepository.findByDocumentId(document.getId())
                .orElseGet(() -> documentAnalysisResultRepository.save(new DocumentAnalysisResult(document.getId())));

        AnalysisCommand command = buildAnalysisCommand(document, caseEntity, user);
        String requestHash = DocumentCrypto.sha256Hex(toJson(command));
        try {
            DocumentAnalysisPort.AnalysisResult analysis = documentAnalysisPort.analyze(command);
            String resultHash = DocumentCrypto.sha256Hex(analysis.responseBodyJson());
            if (analysis.status() == DocumentAnalysisStatus.COMPLETED) {
                result.markCompleted(
                        requestHash,
                        resultHash,
                        analysis.summary(),
                        analysis.riskFlagsJson()
                );
                document.markAnalyzed();
            } else {
                result.markFailed(analysis.summary());
            }
        } catch (RuntimeException ex) {
            result.markFailed(truncate(ex.getMessage(), 1000));
        }
        return DocumentAnalysisResponse.from(result);
    }

    @Transactional(readOnly = true)
    public DocumentAnalysisResponse getAnalysis(String documentId, UserPrincipal userPrincipal) {
        Document document = getAccessibleDocument(documentId, userPrincipal);
        DocumentAnalysisResult result = documentAnalysisResultRepository.findByDocumentId(document.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Document analysis not found"));
        return DocumentAnalysisResponse.from(result);
    }

    private DocumentResponse toResponse(Document document) {
        return DocumentResponse.from(document, fileStoragePort.exists(document.getStorageKey()));
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
        if (!List.of(DocumentStatus.HASHED, DocumentStatus.SIGNATURE_REQUESTED, DocumentStatus.SIGNED, DocumentStatus.ANALYZED)
                .contains(document.getStatus())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Document is not ready for signature");
        }
    }

    private void validateDocumentCanBeAnalyzed(Document document) {
        if (document.getSha256Hash() == null || document.getSha256Hash().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Document hash is required");
        }
    }

    private AnalysisCommand buildAnalysisCommand(Document document, Case caseEntity, User user) {
        String languageCode = user.getLanguageCode() == null || user.getLanguageCode().isBlank()
                ? "ko"
                : user.getLanguageCode();
        return new AnalysisCommand(
                UUID.randomUUID().toString(),
                new DocumentPayload(
                        document.getId(),
                        document.getCaseId(),
                        document.getSha256Hash(),
                        document.getDocumentType(),
                        document.getIssuedAt(),
                        document.getExpiresAt()
                ),
                new CaseContextPayload(
                        caseEntity.getIndustry(),
                        caseEntity.getRegion(),
                        languageCode,
                        "FOREIGN_WORKER"
                ),
                new ChecklistContextPayload(
                        "MOEL_FOREIGN_WORKER_EMPLOYMENT_MANAGEMENT",
                        List.of()
                ),
                new OutputRequestPayload(
                        languageCode,
                        true,
                        true
                )
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to serialize analysis payload");
        }
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
