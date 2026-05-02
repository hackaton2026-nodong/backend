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

    @Column(name = "issue_candidates", columnDefinition = "TEXT")
    private String issueCandidates;

    @Column(name = "generated_analysis", columnDefinition = "TEXT")
    private String generatedAnalysis;

    @Column(columnDefinition = "TEXT")
    private String findings;

    @Column(name = "field_findings", columnDefinition = "TEXT")
    private String fieldFindings;

    @Column(columnDefinition = "TEXT")
    private String citations;

    @Column(name = "recommended_actions", columnDefinition = "TEXT")
    private String recommendedActions;

    @Column(name = "related_institutions", columnDefinition = "TEXT")
    private String relatedInstitutions;

    @Column(name = "case_status", columnDefinition = "TEXT")
    private String caseStatus;

    @Column(name = "detail_json", columnDefinition = "LONGTEXT")
    private String detailJson;

    @Column(name = "failed_reason", columnDefinition = "TEXT")
    private String failedReason;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    public DocumentAnalysisResult(String documentId) {
        this.documentId = documentId;
        this.status = DocumentAnalysisStatus.PENDING;
    }

    public void markCompleted(
            String extractedTextHash,
            String analysisResultHash,
            String summary,
            String riskFlags,
            String issueCandidates,
            String generatedAnalysis,
            String findings,
            String fieldFindings,
            String citations,
            String recommendedActions,
            String relatedInstitutions,
            String caseStatus,
            String detailJson,
            String failedReason
    ) {
        this.status = DocumentAnalysisStatus.COMPLETED;
        this.extractedTextHash = extractedTextHash;
        this.analysisResultHash = analysisResultHash;
        this.summary = summary;
        this.riskFlags = riskFlags;
        this.issueCandidates = issueCandidates;
        this.generatedAnalysis = generatedAnalysis;
        this.findings = findings;
        this.fieldFindings = fieldFindings;
        this.citations = citations;
        this.recommendedActions = recommendedActions;
        this.relatedInstitutions = relatedInstitutions;
        this.caseStatus = caseStatus;
        this.detailJson = detailJson;
        this.failedReason = failedReason;
        this.analyzedAt = LocalDateTime.now();
    }

    public void markFailed(String summary) {
        this.status = DocumentAnalysisStatus.FAILED;
        this.extractedTextHash = null;
        this.analysisResultHash = null;
        this.summary = summary;
        this.riskFlags = "[]";
        this.issueCandidates = "[]";
        this.generatedAnalysis = "{\"status\":\"FAILED\"}";
        this.findings = "[]";
        this.fieldFindings = "[]";
        this.citations = "[]";
        this.recommendedActions = "[]";
        this.relatedInstitutions = "[]";
        this.caseStatus = null;
        this.detailJson = null;
        this.failedReason = summary;
        this.analyzedAt = LocalDateTime.now();
    }
}
