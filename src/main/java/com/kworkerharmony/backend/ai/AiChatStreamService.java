package com.kworkerharmony.backend.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kworkerharmony.backend.ai.dto.request.AiChatStreamRequest;
import com.kworkerharmony.backend.document.config.DocumentAiProperties;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AiChatStreamService {

    private static final String INTERNAL_TOKEN_HEADER = "X-AI-Internal-Token";
    private static final Set<String> FORBIDDEN_MESSAGE_HINTS = Set.of(
            "rawocrtext",
            "raw_ocr_text",
            "layoutparsingresults",
            "parsing_res_list",
            "block_content",
            "base64",
            "imagebytes",
            "filebytes"
    );

    private final DocumentAiProperties properties;
    private final ObjectMapper objectMapper;

    public AiChatStreamService(DocumentAiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public SseEmitter stream(AiChatStreamRequest request) {
        SseEmitter emitter = new SseEmitter(properties.readTimeoutMillis());
        if (!isSafeRequest(request)) {
            sendErrorEvent(emitter, "Raw OCR or file payload is not allowed in chat.");
            return emitter;
        }
        CompletableFuture.runAsync(() -> proxy(request, emitter));
        return emitter;
    }

    private boolean isSafeRequest(AiChatStreamRequest request) {
        if (containsForbiddenHint(request.message())) {
            return false;
        }
        if (request.history() == null) {
            return true;
        }
        return request.history().stream()
                .map(AiChatStreamRequest.ChatHistoryMessage::content)
                .noneMatch(this::containsForbiddenHint);
    }

    private boolean containsForbiddenHint(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return FORBIDDEN_MESSAGE_HINTS.stream().anyMatch(normalized::contains);
    }

    private void proxy(AiChatStreamRequest request, SseEmitter emitter) {
        if (!properties.enabled() || properties.chatStreamEndpoint().isBlank()) {
            sendErrorEvent(emitter, "AI chat stream is disabled.");
            return;
        }
        try {
            HttpResponse<java.io.InputStream> response = httpClient().send(
                    httpRequest(request),
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                sendErrorEvent(emitter, "AI chat stream returned HTTP " + response.statusCode() + ".");
                return;
            }
            forwardSse(response, emitter);
            emitter.complete();
        } catch (IOException ex) {
            sendErrorEvent(emitter, "AI chat stream connection failed.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            sendErrorEvent(emitter, "AI chat stream was interrupted.");
        } catch (RuntimeException ex) {
            sendErrorEvent(emitter, "AI chat stream failed.");
        }
    }

    private HttpClient httpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(properties.connectTimeoutMillis()))
                .build();
    }

    private HttpRequest httpRequest(AiChatStreamRequest request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(properties.chatStreamEndpoint()))
                .timeout(Duration.ofMillis(properties.readTimeoutMillis()))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Accept", MediaType.TEXT_EVENT_STREAM_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody(request)));
        if (!properties.internalToken().isBlank()) {
            builder.header(INTERNAL_TOKEN_HEADER, properties.internalToken());
        }
        return builder.build();
    }

    private String requestBody(AiChatStreamRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("message", request.message());
        root.put("languageCode", request.languageCode() == null || request.languageCode().isBlank()
                ? "ko"
                : request.languageCode());
        root.put("topK", request.topK() == null ? 3 : request.topK());
        ArrayNode history = root.putArray("history");
        if (request.history() != null) {
            for (AiChatStreamRequest.ChatHistoryMessage message : request.history()) {
                ObjectNode item = history.addObject();
                item.put("role", message.role());
                item.put("content", message.content());
            }
        }
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid chat stream request", ex);
        }
    }

    private void forwardSse(HttpResponse<java.io.InputStream> response, SseEmitter emitter) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String eventName = "message";
            StringBuilder data = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    if (!data.isEmpty()) {
                        sendEvent(emitter, eventName, data.toString());
                    }
                    eventName = "message";
                    data.setLength(0);
                    continue;
                }
                if (line.startsWith("event:")) {
                    eventName = line.substring("event:".length()).trim();
                    continue;
                }
                if (line.startsWith("data:")) {
                    if (!data.isEmpty()) {
                        data.append('\n');
                    }
                    data.append(line.substring("data:".length()).trim());
                }
            }
            if (!data.isEmpty()) {
                sendEvent(emitter, eventName, data.toString());
            }
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, String data) throws IOException {
        emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data, MediaType.APPLICATION_JSON));
    }

    private void sendErrorEvent(SseEmitter emitter, String message) {
        try {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("message", message);
            sendEvent(emitter, "error", objectMapper.writeValueAsString(error));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
            return;
        }
        emitter.complete();
    }
}
