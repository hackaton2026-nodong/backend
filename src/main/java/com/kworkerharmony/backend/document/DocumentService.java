package com.kworkerharmony.backend.document;

import com.kworkerharmony.backend.cases.domain.CaseRepository;
import com.kworkerharmony.backend.cases.entity.Case;
import com.kworkerharmony.backend.document.dto.response.DocumentResponse;
import com.kworkerharmony.backend.document.port.DocumentHashPort;
import com.kworkerharmony.backend.document.port.FileStoragePort;
import com.kworkerharmony.backend.document.port.FileStoragePort.StoredFile;
import com.kworkerharmony.backend.enterprise.Enterprise;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import com.kworkerharmony.backend.global.security.UserPrincipal;
import com.kworkerharmony.backend.user.Role;
import com.kworkerharmony.backend.user.User;
import com.kworkerharmony.backend.user.UserRepository;
import java.time.LocalDate;
import java.util.List;
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

    private DocumentResponse toResponse(Document document) {
        return DocumentResponse.from(document, fileStoragePort.exists(document.getStorageKey()));
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
