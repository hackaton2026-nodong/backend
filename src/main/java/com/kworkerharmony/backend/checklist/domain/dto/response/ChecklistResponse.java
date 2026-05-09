package com.kworkerharmony.backend.checklist.domain.dto.response;

import com.kworkerharmony.backend.checklist.domain.ChecklistStatus;
import com.kworkerharmony.backend.checklist.entity.CaseChecklistStatus;
import com.kworkerharmony.backend.reference.checklist.ChecklistItemDefinition;
import com.kworkerharmony.backend.reference.checklist.ChecklistTriggerType;
import java.time.LocalDateTime;

public record ChecklistResponse(
        String id,
        String caseId,
        String checklistItemCode,
        String sectionCode,
        String sectionTitle,
        String code,
        String title,
        String description,
        boolean required,
        ChecklistTriggerType triggerType,
        ChecklistStatus status,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ChecklistResponse from(String caseId, ChecklistItemDefinition definition, CaseChecklistStatus checklistStatus) {
        return new ChecklistResponse(
                checklistStatus == null ? null : checklistStatus.getId(),
                caseId,
                definition.code(),
                definition.sectionCode(),
                definition.sectionTitle(),
                definition.code(),
                definition.title(),
                definition.description(),
                definition.required(),
                definition.triggerTypeOrDefault(),
                checklistStatus == null ? ChecklistStatus.NOT_STARTED : checklistStatus.getStatus(),
                checklistStatus == null ? null : checklistStatus.getNote(),
                checklistStatus == null ? null : checklistStatus.getCreatedAt(),
                checklistStatus == null ? null : checklistStatus.getUpdatedAt()
        );
    }

    public static ChecklistResponse from(CaseChecklistStatus checklistStatus, ChecklistItemDefinition definition) {
        return from(checklistStatus.getCaseEntity().getId(), definition, checklistStatus);
    }
}
