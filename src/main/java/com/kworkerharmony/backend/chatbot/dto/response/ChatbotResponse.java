package com.kworkerharmony.backend.chatbot.dto.response;

import com.kworkerharmony.backend.chatbot.Chatbot;

public record ChatbotResponse(
        Long id,
        String diagnose,
        Long userId
) {

    public static ChatbotResponse from(Chatbot chatbot) {
        return new ChatbotResponse(
                chatbot.getId(),
                chatbot.getDiagnose(),
                chatbot.getUser().getId()
        );
    }
}
