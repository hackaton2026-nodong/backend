package com.kworkerharmony.backend.ai.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AiChatStreamRequest(
        @NotBlank
        @Size(max = 4000)
        String message,
        @Size(max = 10)
        String languageCode,
        @Min(1)
        @Max(10)
        Integer topK,
        @Size(max = 12)
        List<@Valid ChatHistoryMessage> history,
        @Valid
        CaseContext caseContext,
        @Size(max = 3)
        List<@Valid AttachmentContext> attachments
) {
    public record ChatHistoryMessage(
            @NotBlank
            @Pattern(regexp = "user|assistant")
            String role,
            @NotBlank
            @Size(max = 3000)
            String content
    ) {
    }

    public record CaseContext(
            @Size(max = 120)
            String documentId,
            @Size(max = 80)
            String documentStatus,
            @Size(max = 40)
            String riskLevel,
            @Size(max = 120)
            String contractPeriod,
            @Size(max = 80)
            String analysisStatus,
            @Size(max = 1000)
            String analysisSummary,
            @Size(max = 1200)
            String generatedAnalysisText,
            @Size(max = 8)
            List<@Size(max = 80) String> issueCandidates,
            @Size(max = 6)
            List<@Valid RiskFlagContext> riskFlags,
            @Size(max = 6)
            List<@Valid FindingContext> findings,
            @Size(max = 5)
            List<@Valid RecommendedActionContext> recommendedActions
    ) {
    }

    public record RiskFlagContext(
            @Size(max = 80)
            String code,
            @Size(max = 120)
            String label,
            @Size(max = 40)
            String level,
            @Size(max = 500)
            String description
    ) {
    }

    public record FindingContext(
            @Size(max = 120)
            String id,
            @Size(max = 160)
            String title,
            @Size(max = 700)
            String description,
            @Size(max = 40)
            String severity,
            @Size(max = 120)
            String fieldName
    ) {
    }

    public record RecommendedActionContext(
            @Size(max = 160)
            String label,
            @Size(max = 700)
            String description,
            @Size(max = 40)
            String priority,
            @Size(max = 160)
            String institutionName,
            @Size(max = 700)
            String expectedPath
    ) {
    }

    public record AttachmentContext(
            @Size(max = 120)
            String attachmentId,
            @Size(max = 240)
            String fileName,
            @Size(max = 120)
            String mimeType,
            Long fileSize,
            @Size(max = 12000)
            String textPreview,
            @Size(max = 40)
            String status,
            @Size(max = 300)
            String warning
    ) {
    }
}
