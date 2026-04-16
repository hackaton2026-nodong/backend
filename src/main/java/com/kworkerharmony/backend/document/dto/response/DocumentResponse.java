package com.kworkerharmony.backend.document.dto.response;

import com.kworkerharmony.backend.document.Document;
import com.kworkerharmony.backend.document.DocumentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DocumentResponse(
        String id,
        String caseId,
        Long uploaderUserId,
        String documentType,
        String originalFileName,
        String storageKey,
        String mimeType,
        Long fileSize,
        String sha256Hash,
        String anchoredTxId,
        DocumentStatus status,
        LocalDate issuedAt,
        LocalDate expiresAt,
        LocalDateTime ocrCompletedAt,
        LocalDateTime analyzedAt
) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getCaseId(),
                document.getUploaderUserId(),
                document.getDocumentType(),
                document.getOriginalFileName(),
                document.getStorageKey(),
                document.getMimeType(),
                document.getFileSize(),
                document.getSha256Hash(),
                document.getAnchoredTxId(),
                document.getStatus(),
                document.getIssuedAt(),
                document.getExpiresAt(),
                document.getOcrCompletedAt(),
                document.getAnalyzedAt()
        );
    }
}
