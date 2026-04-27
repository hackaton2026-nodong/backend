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
        name = "document_analysis_results",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_document_analysis_results_document",
                columnNames = "document_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentAnalysisResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "document_id", nullable = false, length = 36)
    private String documentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentAnalysisStatus status;

    @Column(name = "extracted_text_hash", length = 64)
    private String extractedTextHash;

    @Column(name = "analysis_result_hash", length = 64)
    private String analysisResultHash;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "risk_flags", columnDefinition = "TEXT")
    private String riskFlags;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    public DocumentAnalysisResult(String documentId) {
        this.documentId = documentId;
        this.status = DocumentAnalysisStatus.PENDING;
    }

    public void markCompleted(String extractedTextHash, String analysisResultHash, String summary, String riskFlags) {
        this.status = DocumentAnalysisStatus.COMPLETED;
        this.extractedTextHash = extractedTextHash;
        this.analysisResultHash = analysisResultHash;
        this.summary = summary;
        this.riskFlags = riskFlags;
        this.analyzedAt = LocalDateTime.now();
    }

    public void markFailed(String summary) {
        this.status = DocumentAnalysisStatus.FAILED;
        this.extractedTextHash = null;
        this.analysisResultHash = null;
        this.summary = summary;
        this.riskFlags = "[]";
        this.analyzedAt = LocalDateTime.now();
    }
}
