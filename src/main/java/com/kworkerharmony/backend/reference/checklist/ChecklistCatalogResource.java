package com.kworkerharmony.backend.reference.checklist;

import java.util.List;

public record ChecklistCatalogResource(
        String catalogCode,
        String version,
        String title,
        List<ChecklistSectionDefinition> sections
) {
}
