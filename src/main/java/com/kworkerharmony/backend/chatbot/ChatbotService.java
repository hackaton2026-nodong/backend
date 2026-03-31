package com.kworkerharmony.backend.chatbot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatbotRepository chatbotRepository;
}
