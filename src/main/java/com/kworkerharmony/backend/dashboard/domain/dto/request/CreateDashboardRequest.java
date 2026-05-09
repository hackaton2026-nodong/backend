package com.kworkerharmony.backend.dashboard.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDashboardRequest(
        @NotNull(message = "User id is required")
        Long userId,

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Summary is required")
        String summary
) {
}
