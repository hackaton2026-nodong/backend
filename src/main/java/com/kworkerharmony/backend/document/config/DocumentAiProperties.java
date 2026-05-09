package com.kworkerharmony.backend.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.document.ai")
public record DocumentAiProperties(
        boolean enabled,
        String endpoint,
        String healthEndpoint,
        long healthTimeoutMillis,
        String chatStreamEndpoint,
        String internalToken,
        long connectTimeoutMillis,
        long readTimeoutMillis,
        int maxRetries
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
        if (chatStreamEndpoint == null || chatStreamEndpoint.isBlank()) {
            chatStreamEndpoint = "http://localhost:8000/chat/stream";
        }
        if (internalToken == null) {
            internalToken = "";
        }
        if (connectTimeoutMillis <= 0) {
            connectTimeoutMillis = 3000L;
        }
        if (readTimeoutMillis <= 0) {
            readTimeoutMillis = 35000L;
        }
        if (maxRetries < 0) {
            maxRetries = 0;
        }
    }
}
