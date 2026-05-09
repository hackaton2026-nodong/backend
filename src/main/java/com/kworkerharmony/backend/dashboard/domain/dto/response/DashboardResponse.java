package com.kworkerharmony.backend.dashboard.domain.dto.response;

import com.kworkerharmony.backend.dashboard.entity.Dashboard;
import java.time.LocalDateTime;

public record DashboardResponse(
        String id,
        Long userId,
        String title,
        String summary,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DashboardResponse from(Dashboard dashboard) {
        return new DashboardResponse(
                dashboard.getId(),
                dashboard.getUser().getId(),
                dashboard.getTitle(),
                dashboard.getSummary(),
                dashboard.getCreatedAt(),
                dashboard.getUpdatedAt()
        );
    }
}
