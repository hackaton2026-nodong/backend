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

    @Column(name = "case_id", length = 36)
    private String caseId;

    @Column(name = "uploader_user_id")
    private Long uploaderUserId;

    @Column(name = "document_type", length = 50)
    private String documentType;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "storage_key")
    private String storageKey;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "sha256_hash", length = 64)
    private String sha256Hash;

    @Column(name = "anchored_tx_id")
    private String anchoredTxId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentStatus status;

    @Column(name = "issued_at")
    private LocalDate issuedAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "ocr_completed_at")
    private LocalDateTime ocrCompletedAt;

    @Column(name = "analyzed_at")
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
