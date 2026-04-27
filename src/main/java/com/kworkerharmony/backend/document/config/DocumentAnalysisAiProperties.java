package com.kworkerharmony.backend.document.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.document.analysis.ai")
public record DocumentAnalysisAiProperties(
        boolean enabled,
        String baseUrl,
        String endpointPath,
        String healthPath,
        long healthTimeoutMillis
) {

    public DocumentAnalysisAiProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8000";
        }
        if (endpointPath == null || endpointPath.isBlank()) {
            endpointPath = "/document-analysis";
        }
        if (healthPath == null || healthPath.isBlank()) {
            healthPath = "/health";
        }
        if (healthTimeoutMillis <= 0) {
            healthTimeoutMillis = 2000L;
        }
    }

    public URI healthUri() {
        return URI.create(join(baseUrl, healthPath));
    }

    private String join(String baseUrl, String path) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBaseUrl + normalizedPath;
    }
}
