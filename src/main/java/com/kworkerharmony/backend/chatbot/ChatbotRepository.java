package com.kworkerharmony.backend.chatbot;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatbotRepository extends JpaRepository<Chatbot, Long> {
}
