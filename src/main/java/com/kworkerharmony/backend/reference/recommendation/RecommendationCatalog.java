package com.kworkerharmony.backend.reference.recommendation;

import com.kworkerharmony.backend.reference.ReferenceResourceReader;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RecommendationCatalog {

    private final List<RecommendationItemDefinition> items;

    public RecommendationCatalog(ReferenceResourceReader resourceReader) {
        this.items = resourceReader.read("reference/recommendations/worker-recommendations.json", RecommendationCatalogResource.class)
                .items();
    }

    public List<RecommendationItemDefinition> find(String region, String languageCode, String industry, int limit) {
        return items.stream()
                .filter(item -> matches(item.region(), region))
                .filter(item -> matches(item.languageCode(), languageCode))
                .filter(item -> matches(item.industry(), industry))
                .limit(limit)
                .toList();
    }

    private boolean matches(String expected, String actual) {
        return expected == null || expected.isBlank() || "ALL".equalsIgnoreCase(expected)
                || (actual != null && expected.equalsIgnoreCase(actual));
    }
}
