package com.kworkerharmony.backend.consultation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateConsultationSessionRequest(
        @NotBlank
        @Size(max = 100)
        String title,

        String caseId,

        @Size(max = 4000)
        String initialMessage
) {
}
