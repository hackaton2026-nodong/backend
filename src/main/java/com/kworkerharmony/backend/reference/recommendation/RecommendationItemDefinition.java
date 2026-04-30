package com.kworkerharmony.backend.reference.recommendation;

import java.util.List;

public record RecommendationItemDefinition(
        String category,
        String name,
        String description,
        String targetPath,
        String region,
        String languageCode,
        String industry,
        List<String> reasonTags
) {
}
