package com.kworkerharmony.backend.checklist.domain.dto.request;

import com.kworkerharmony.backend.checklist.domain.ChecklistStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateChecklistRequest(
        @NotNull(message = "Case id is required")
        String caseId,

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        ChecklistStatus status
) {
}
