package com.kworkerharmony.backend.cases.domain.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCaseRequest(
        @NotBlank(message = "Industry is required")
        String industry,

        @NotBlank(message = "Region is required")
        String region
) {
}
