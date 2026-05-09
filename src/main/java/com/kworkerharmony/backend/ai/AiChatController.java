package com.kworkerharmony.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.kworkerharmony.backend.ai.dto.request.AiChatStreamRequest;
import com.kworkerharmony.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatStreamService aiChatStreamService;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody AiChatStreamRequest request) {
        return aiChatStreamService.stream(request);
    }

    @PostMapping(value = "/chat/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<JsonNode> uploadChatAttachment(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(aiChatStreamService.uploadAttachment(file));
    }
}
