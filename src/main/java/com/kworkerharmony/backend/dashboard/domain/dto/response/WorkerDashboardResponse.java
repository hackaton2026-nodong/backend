package com.kworkerharmony.backend.dashboard.domain.dto.response;

import com.kworkerharmony.backend.user.UserType;
import java.time.LocalDate;
import java.util.List;

public record WorkerDashboardResponse(
        Header header,
        AgentCenter agentCenter,
        List<SummaryCard> summaryCards,
        List<TodayAction> todayActions,
        RecommendationSlot recommendationSlot,
        NoticePanel noticePanel
) {

    public record Header(
            Long userId,
            String userName,
            UserType userType,
            LocalDate baseDate,
            String caseId
    ) {
    }

    public record AgentCenter(
            String title,
            String description,
            List<ActionLink> actions,
            List<String> reasonTypes
    ) {
    }

    public record ActionLink(
            String label,
            String targetPath
    ) {
    }

    public record SummaryCard(
            String key,
            String title,
            String value,
            String subtitle,
            String severity
    ) {
    }

    public record TodayAction(
            String type,
            String title,
            String description,
            String ctaLabel,
            String targetPath,
            String priority
    ) {
    }

    public record RecommendationSlot(
            String title,
            String placeholderMessage,
            List<String> reasonTags,
            List<RecommendationItem> items
    ) {
    }

    public record RecommendationItem(
            String category,
            String name,
            String description,
            String targetPath
    ) {
    }

    public record NoticePanel(
            String title,
            String message,
            String severity,
            String ctaLabel,
            String targetPath
    ) {
    }
}
