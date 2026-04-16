package com.kworkerharmony.backend.document;

import com.kworkerharmony.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "documents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    private String caseId;

    private Long uploaderUserId;

    @Column(length = 50)
    private String documentType;

    private String originalFileName;

    private String storageKey;

    private String mimeType;

    private Long fileSize;

    @Column(length = 64)
    private String sha256Hash;

    private String anchoredTxId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentStatus status;

    private LocalDate issuedAt;

    private LocalDate expiresAt;

    private LocalDateTime ocrCompletedAt;

    private LocalDateTime analyzedAt;

    public Document(
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
        this.caseId = caseId;
        this.uploaderUserId = uploaderUserId;
        this.documentType = documentType;
        this.originalFileName = originalFileName;
        this.storageKey = storageKey;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.sha256Hash = sha256Hash;
        this.anchoredTxId = anchoredTxId;
        this.status = status;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.ocrCompletedAt = ocrCompletedAt;
        this.analyzedAt = analyzedAt;
    }

    public static Document createUploaded(
            Long uploaderUserId,
            DocumentType documentType,
            LocalDate issuedAt,
            LocalDate expiresAt
    ) {
        return new Document(
                null,
                uploaderUserId,
                documentType.name(),
                null,
                null,
                null,
                null,
                null,
                null,
                DocumentStatus.UPLOADED,
                issuedAt,
                expiresAt,
                null,
                null
        );
    }

    public void assignToCase(String caseId) {
        this.caseId = caseId;
    }

    public void markStored(String originalFileName, String storageKey, String mimeType, Long fileSize) {
        this.originalFileName = originalFileName;
        this.storageKey = storageKey;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.status = DocumentStatus.STORED;
    }

    public void markHashed(String sha256Hash) {
        this.sha256Hash = sha256Hash;
        this.status = DocumentStatus.HASHED;
    }

    public void markAnchored(String anchoredTxId) {
        this.anchoredTxId = anchoredTxId;
        this.status = DocumentStatus.ANCHORED_ON_CHAIN;
    }

    public void markOcrProcessing() {
        this.status = DocumentStatus.OCR_PROCESSING;
    }

    public void markOcrCompleted() {
        this.status = DocumentStatus.OCR_COMPLETED;
        this.ocrCompletedAt = LocalDateTime.now();
    }

    public void markStructured() {
        this.status = DocumentStatus.STRUCTURED;
    }

    public void markAnalyzed() {
        this.status = DocumentStatus.ANALYZED;
        this.analyzedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = DocumentStatus.FAILED;
    }
}
