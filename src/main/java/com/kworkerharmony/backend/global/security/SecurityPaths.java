package com.kworkerharmony.backend.global.security;

public final class SecurityPaths {

    public static final String[] PUBLIC_URLS = {
            "/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private SecurityPaths() {
    }
}
