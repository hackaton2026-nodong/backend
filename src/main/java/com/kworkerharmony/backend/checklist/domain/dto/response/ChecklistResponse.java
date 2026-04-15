package com.kworkerharmony.backend.checklist.domain.dto.response;

import com.kworkerharmony.backend.checklist.domain.ChecklistStatus;
import com.kworkerharmony.backend.checklist.entity.Checklist;
import java.time.LocalDateTime;

public record ChecklistResponse(
        String id,
        String caseId,
        String title,
        String description,
        ChecklistStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ChecklistResponse from(Checklist checklist) {
        return new ChecklistResponse(
                checklist.getId(),
                checklist.getCaseEntity().getId(),
                checklist.getTitle(),
                checklist.getDescription(),
                checklist.getStatus(),
                checklist.getCreatedAt(),
                checklist.getUpdatedAt()
        );
    }
}
