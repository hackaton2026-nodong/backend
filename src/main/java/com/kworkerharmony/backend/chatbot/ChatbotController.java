package com.kworkerharmony.backend.chatbot;

import com.kworkerharmony.backend.chatbot.dto.request.CreateChatbotRequest;
import com.kworkerharmony.backend.chatbot.dto.response.ChatbotResponse;
import com.kworkerharmony.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chatbots")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @GetMapping
    public ApiResponse<List<ChatbotResponse>> getChatbots() {
        return ApiResponse.success(chatbotService.getChatbots());
    }

    @GetMapping("/{chatbotId}")
    public ApiResponse<ChatbotResponse> getChatbot(@PathVariable Long chatbotId) {
        return ApiResponse.success(chatbotService.getChatbot(chatbotId));
    }

    @PostMapping
    public ApiResponse<ChatbotResponse> createChatbot(@Valid @RequestBody CreateChatbotRequest request) {
        return ApiResponse.success(chatbotService.createChatbot(request));
    }
}
