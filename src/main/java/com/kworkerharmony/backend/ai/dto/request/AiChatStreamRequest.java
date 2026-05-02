package com.kworkerharmony.backend.ai.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatStreamRequest(
        @NotBlank
        @Size(max = 4000)
        String message,
        @Size(max = 10)
        String languageCode,
        @Min(1)
        @Max(10)
        Integer topK
) {
}
