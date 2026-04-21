package com.kworkerharmony.backend.checklist.domain.dto.response;

import com.kworkerharmony.backend.reference.checklist.ChecklistItemDefinition;

public record ChecklistItemResponse(
        String sectionCode,
        String sectionTitle,
        String code,
        String title,
        String description,
        boolean required,
        int displayOrder
) {

    public static ChecklistItemResponse from(ChecklistItemDefinition checklistItem) {
        return new ChecklistItemResponse(
                checklistItem.sectionCode(),
                checklistItem.sectionTitle(),
                checklistItem.code(),
                checklistItem.title(),
                checklistItem.description(),
                checklistItem.required(),
                checklistItem.displayOrder()
        );
    }
}
