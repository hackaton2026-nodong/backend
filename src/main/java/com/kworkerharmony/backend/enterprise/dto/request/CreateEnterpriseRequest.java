package com.kworkerharmony.backend.enterprise.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateEnterpriseRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Contact is required")
        String contact,

        @NotBlank(message = "Location is required")
        String location
) {
}
