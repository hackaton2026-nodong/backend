package com.kworkerharmony.backend.enterprise.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateEnterpriseRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Business number is required")
        String businessNumber,

        @NotBlank(message = "Industry is required")
        String industry,

        @NotBlank(message = "Country is required")
        String country
) {
}
