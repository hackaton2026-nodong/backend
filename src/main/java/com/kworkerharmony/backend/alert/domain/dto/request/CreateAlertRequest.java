package com.kworkerharmony.backend.alert.domain.dto.request;

import com.kworkerharmony.backend.alert.domain.AlertType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAlertRequest(
        @NotNull(message = "User id is required")
        Long userId,

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Message is required")
        String message,

        AlertType type,
        Boolean isRead
) {
}
