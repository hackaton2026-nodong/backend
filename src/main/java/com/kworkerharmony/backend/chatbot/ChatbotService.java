package com.kworkerharmony.backend.chatbot;

import com.kworkerharmony.backend.chatbot.dto.request.CreateChatbotRequest;
import com.kworkerharmony.backend.chatbot.dto.response.ChatbotResponse;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import com.kworkerharmony.backend.user.User;
import com.kworkerharmony.backend.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatbotRepository chatbotRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ChatbotResponse> getChatbots() {
        return chatbotRepository.findAll().stream()
                .map(ChatbotResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatbotResponse getChatbot(Long chatbotId) {
        Chatbot chatbot = chatbotRepository.findById(chatbotId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Chatbot not found"));
        return ChatbotResponse.from(chatbot);
    }

    @Transactional
    public ChatbotResponse createChatbot(CreateChatbotRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));

        Chatbot chatbot = chatbotRepository.save(new Chatbot(request.diagnose(), user));
        return ChatbotResponse.from(chatbot);
    }
}
