package com.kworkerharmony.backend.checklist.domain.dto.response;

import com.kworkerharmony.backend.reference.checklist.ChecklistItemDefinition;
import com.kworkerharmony.backend.reference.checklist.ChecklistTriggerType;

public record ChecklistItemResponse(
        String sectionCode,
        String sectionTitle,
        String code,
        String title,
        String description,
        boolean required,
        int displayOrder,
        ChecklistTriggerType triggerType
) {

    public static ChecklistItemResponse from(ChecklistItemDefinition checklistItem) {
        return new ChecklistItemResponse(
                checklistItem.sectionCode(),
                checklistItem.sectionTitle(),
                checklistItem.code(),
                checklistItem.title(),
                checklistItem.description(),
                checklistItem.required(),
                checklistItem.displayOrder(),
                checklistItem.triggerTypeOrDefault()
        );
    }
}
