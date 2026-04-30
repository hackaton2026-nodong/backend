package com.kworkerharmony.backend.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.document.ocr")
public record DocumentOcrProperties(
        boolean enabled,
        String endpoint,
        String callbackToken,
        String callbackBaseUrl
) {
    public DocumentOcrProperties {
        if (endpoint == null) {
            endpoint = "";
        }
        if (callbackToken == null) {
            callbackToken = "";
        }
        if (callbackBaseUrl == null) {
            callbackBaseUrl = "";
        }
    }
}
