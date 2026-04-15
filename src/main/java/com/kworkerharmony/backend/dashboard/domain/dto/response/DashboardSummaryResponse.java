package com.kworkerharmony.backend.dashboard.domain.dto.response;

import com.kworkerharmony.backend.user.UserType;

public record DashboardSummaryResponse(
        Long userId,
        UserType userType,
        long activeCaseCount,
        long checklistCount,
        long completedChecklistCount,
        long unreadNotificationCount
) {
}
