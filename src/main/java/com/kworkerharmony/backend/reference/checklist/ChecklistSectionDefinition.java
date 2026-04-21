package com.kworkerharmony.backend.reference.checklist;

import java.util.List;

public record ChecklistSectionDefinition(
        String code,
        String title,
        int displayOrder,
        List<ChecklistItemDefinition> items
) {
}
