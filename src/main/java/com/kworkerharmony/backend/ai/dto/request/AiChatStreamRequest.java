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
        CaseContext caseContext
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
            @Size(max = 8)
            List<@Size(max = 80) String> issueCandidates
    ) {
    }
}
