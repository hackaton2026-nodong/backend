package com.kworkerharmony.backend.checklist.domain.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateChecklistItemRequest(
        @NotBlank(message = "Code is required")
        String code,

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        boolean required
) {
}
