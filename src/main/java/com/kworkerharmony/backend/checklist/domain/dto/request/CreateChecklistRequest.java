package com.kworkerharmony.backend.checklist.domain.dto.request;

import com.kworkerharmony.backend.checklist.domain.ChecklistStatus;
import jakarta.validation.constraints.NotBlank;

public record CreateChecklistRequest(
        @NotBlank(message = "Case id is required")
        String caseId,

        @NotBlank(message = "Checklist item code is required")
        String checklistItemCode,

        ChecklistStatus status,

        String note
) {
}
