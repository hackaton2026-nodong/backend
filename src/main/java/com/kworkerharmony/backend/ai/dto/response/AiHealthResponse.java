package com.kworkerharmony.backend.ai.dto.response;

import java.time.LocalDateTime;

public record AiHealthResponse(
        String mode,
        String status,
        boolean available,
        String healthPath,
        Integer upstreamStatusCode,
        String message,
        LocalDateTime checkedAt
) {
}
