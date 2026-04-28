package com.kworkerharmony.backend.document.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kworkerharmony.backend.document.config.DocumentOcrProperties;
import com.kworkerharmony.backend.document.port.DocumentOcrPort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpDocumentOcrAdapter implements DocumentOcrPort {

    private final DocumentOcrProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpDocumentOcrAdapter(
            DocumentOcrProperties properties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public void requestOcr(OcrCommand command) {
        if (!properties.enabled() || properties.endpoint().isBlank()) {
            return;
        }

        restClient.post()
                .uri(properties.endpoint())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-OCR-Callback-Token", properties.callbackToken())
                .body(jsonBody(command))
                .retrieve()
                .toBodilessEntity();
    }

    private String jsonBody(OcrCommand command) {
        try {
            return objectMapper.writeValueAsString(new OcrJobRequest(
                    command.documentId(),
                    command.caseId(),
                    command.documentType(),
                    command.storageKey(),
                    command.sha256Hash(),
                    command.callbackUrl()
            ));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize OCR request", ex);
        }
    }

    private record OcrJobRequest(
            String documentId,
            String caseId,
            String documentType,
            String storageKey,
            String sha256Hash,
            String callbackUrl
    ) {
    }
}
