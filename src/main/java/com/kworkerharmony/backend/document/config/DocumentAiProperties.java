package com.kworkerharmony.backend.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.document.ai")
public record DocumentAiProperties(
        boolean enabled,
        String endpoint
) {

    public DocumentAiProperties {
        if (endpoint == null) {
            endpoint = "";
        }
    }
}
