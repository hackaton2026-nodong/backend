package com.kworkerharmony.backend.global.security;

public final class SecurityPaths {

    public static final String[] PUBLIC_URLS = {
            "/auth/**",
            "/api/auth/**",
            "/document-upload-test.html",
            "/dashboard-api-preview.html",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private SecurityPaths() {
    }
}
