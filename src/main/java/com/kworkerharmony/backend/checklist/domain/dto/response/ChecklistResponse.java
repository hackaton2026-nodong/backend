package com.kworkerharmony.backend.checklist.domain.dto.response;

import com.kworkerharmony.backend.checklist.domain.ChecklistStatus;
import com.kworkerharmony.backend.checklist.entity.CaseChecklistItem;
import java.time.LocalDateTime;

public record ChecklistResponse(
        String id,
        String caseId,
        String checklistItemId,
        String code,
        String title,
        String description,
        boolean required,
        ChecklistStatus status,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ChecklistResponse from(CaseChecklistItem checklist) {
        return new ChecklistResponse(
                checklist.getId(),
                checklist.getCaseEntity().getId(),
                checklist.getChecklistItem().getId(),
                checklist.getChecklistItem().getCode(),
                checklist.getChecklistItem().getTitle(),
                checklist.getChecklistItem().getDescription(),
                checklist.getChecklistItem().isRequired(),
                checklist.getStatus(),
                checklist.getNote(),
                checklist.getCreatedAt(),
                checklist.getUpdatedAt()
        );
    }
}
