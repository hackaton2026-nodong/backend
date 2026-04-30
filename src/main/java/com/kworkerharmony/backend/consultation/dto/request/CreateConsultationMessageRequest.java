package com.kworkerharmony.backend.consultation.dto.request;

import com.kworkerharmony.backend.consultation.ConsultationMessageRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateConsultationMessageRequest(
        @NotNull
        ConsultationMessageRole role,

        @NotBlank
        @Size(max = 4000)
        String content
) {
}
