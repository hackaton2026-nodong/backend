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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "document_extractions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_document_extractions_document",
                columnNames = "document_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentExtraction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "document_id", nullable = false, length = 36)
    private String documentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentExtractionStatus status;

    @Column(name = "schema_version", nullable = false, length = 100)
    private String schemaVersion;

    @Column(name = "source_engine", nullable = false, length = 100)
    private String sourceEngine;

    @Column(name = "source_result_hash", length = 64)
    private String sourceResultHash;

    @Column(name = "extracted_payload", columnDefinition = "TEXT")
    private String extractedPayload;

    @Column(name = "corrected_payload", columnDefinition = "TEXT")
    private String correctedPayload;

    @Column(name = "ai_payload_hash", length = 64)
    private String aiPayloadHash;

    @Column(name = "review_required_reason", length = 1000)
    private String reviewRequiredReason;

    @Column(name = "extracted_at")
    private LocalDateTime extractedAt;

    @Column(name = "corrected_at")
    private LocalDateTime correctedAt;

    public DocumentExtraction(String documentId, String schemaVersion, String sourceEngine) {
        this.documentId = documentId;
        this.schemaVersion = schemaVersion;
        this.sourceEngine = sourceEngine;
        this.status = DocumentExtractionStatus.PENDING;
    }

    public void markExtracted(String sourceResultHash, String extractedPayload, String aiPayloadHash, String reviewRequiredReason) {
        this.status = reviewRequiredReason == null || reviewRequiredReason.isBlank()
                ? DocumentExtractionStatus.EXTRACTED
                : DocumentExtractionStatus.NEEDS_REVIEW;
        this.sourceResultHash = sourceResultHash;
        this.extractedPayload = extractedPayload;
        this.correctedPayload = null;
        this.aiPayloadHash = aiPayloadHash;
        this.reviewRequiredReason = reviewRequiredReason;
        this.extractedAt = LocalDateTime.now();
        this.correctedAt = null;
    }

    public void markCorrected(String correctedPayload, String aiPayloadHash) {
        this.status = DocumentExtractionStatus.CORRECTED;
        this.correctedPayload = correctedPayload;
        this.aiPayloadHash = aiPayloadHash;
        this.reviewRequiredReason = null;
        this.correctedAt = LocalDateTime.now();
    }

    public void markFailed(String reviewRequiredReason) {
        this.status = DocumentExtractionStatus.FAILED;
        this.reviewRequiredReason = reviewRequiredReason;
    }
}
