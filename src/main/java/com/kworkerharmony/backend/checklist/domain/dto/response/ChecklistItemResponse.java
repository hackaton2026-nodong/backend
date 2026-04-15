package com.kworkerharmony.backend.checklist.domain.dto.response;

import com.kworkerharmony.backend.checklist.entity.ChecklistItem;
import java.time.LocalDateTime;

public record ChecklistItemResponse(
        String id,
        String code,
        String title,
        String description,
        boolean required,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ChecklistItemResponse from(ChecklistItem checklistItem) {
        return new ChecklistItemResponse(
                checklistItem.getId(),
                checklistItem.getCode(),
                checklistItem.getTitle(),
                checklistItem.getDescription(),
                checklistItem.isRequired(),
                checklistItem.getCreatedAt(),
                checklistItem.getUpdatedAt()
        );
    }
}
