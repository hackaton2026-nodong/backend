package com.kworkerharmony.backend.reference.recommendation;

import java.util.List;

public record RecommendationCatalogResource(
        List<RecommendationItemDefinition> items
) {
}
