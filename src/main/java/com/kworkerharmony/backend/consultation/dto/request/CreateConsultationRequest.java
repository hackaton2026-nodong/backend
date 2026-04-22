package com.kworkerharmony.backend.consultation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateConsultationRequest(
        @NotBlank(message = "Diagnose is required")
        String diagnose,

        @NotNull(message = "User id is required")
        Long userId
) {
}
