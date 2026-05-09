package com.kworkerharmony.backend.alert.domain.dto.response;

import com.kworkerharmony.backend.alert.domain.AlertType;
import com.kworkerharmony.backend.alert.entity.Alert;
import java.time.LocalDateTime;

public record AlertResponse(
        String id,
        Long userId,
        String title,
        String message,
        AlertType type,
        boolean isRead,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getUser().getId(),
                alert.getTitle(),
                alert.getMessage(),
                alert.getType(),
                alert.isRead(),
                alert.getCreatedAt(),
                alert.getUpdatedAt()
        );
    }
}
