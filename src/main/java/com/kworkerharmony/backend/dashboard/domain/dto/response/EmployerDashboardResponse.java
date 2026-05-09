package com.kworkerharmony.backend.dashboard.domain.dto.response;

import com.kworkerharmony.backend.user.UserType;

public record EmployerDashboardResponse(
        Long userId,
        UserType userType,
        long activeCaseCount,
        long totalChecklistCount,
        long completedChecklistCount,
        long unreadAlertCount
) {
}
