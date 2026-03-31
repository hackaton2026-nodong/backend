package com.kworkerharmony.backend.auth.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}
