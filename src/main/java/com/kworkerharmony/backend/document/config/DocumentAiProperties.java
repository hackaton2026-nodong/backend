package com.kworkerharmony.backend.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.document.ai")
public record DocumentAiProperties(
        boolean enabled,
        String endpoint,
        String healthEndpoint,
        long healthTimeoutMillis
) {

    public DocumentAiProperties {
        if (endpoint == null) {
            endpoint = "";
        }
        if (healthEndpoint == null || healthEndpoint.isBlank()) {
            healthEndpoint = "http://localhost:8000/health";
        }
        if (healthTimeoutMillis <= 0) {
            healthTimeoutMillis = 2000L;
        }
    }
}
