package com.kworkerharmony.backend.checklist.domain.dto.request;

import com.kworkerharmony.backend.checklist.domain.ChecklistStatus;
import jakarta.validation.constraints.NotNull;

public record CreateChecklistRequest(
        @NotNull(message = "Case id is required")
        String caseId,

        @NotNull(message = "Checklist item id is required")
        String checklistItemId,

        ChecklistStatus status,

        String note
) {
}
