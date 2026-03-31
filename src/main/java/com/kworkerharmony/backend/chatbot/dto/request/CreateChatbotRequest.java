package com.kworkerharmony.backend.chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateChatbotRequest(
        @NotBlank(message = "Diagnose is required")
        String diagnose,

        @NotNull(message = "User id is required")
        Long userId
) {
}
