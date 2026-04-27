package com.kworkerharmony.backend.document.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kworkerharmony.backend.document.DocumentAnalysisStatus;
import com.kworkerharmony.backend.document.config.DocumentAnalysisAiProperties;
import com.kworkerharmony.backend.document.port.DocumentAnalysisPort;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(prefix = "app.document.analysis.ai", name = "enabled", havingValue = "true")
public class HttpDocumentAnalysisAdapter implements DocumentAnalysisPort {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String endpointPath;

    public HttpDocumentAnalysisAdapter(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            DocumentAnalysisAiProperties properties
    ) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.objectMapper = objectMapper;
        this.endpointPath = properties.endpointPath();
    }

    @Override
    public AnalysisResult analyze(AnalysisCommand command) {
        try {
            JsonNode responseBody = restClient.post()
                    .uri(endpointPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .retrieve()
                    .body(JsonNode.class);

            if (responseBody == null || responseBody.isNull()) {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "AI analysis response is empty");
            }

            DocumentAnalysisStatus status = parseStatus(responseBody);
            String summary = textOrDefault(responseBody.get("summary"), defaultSummary(status));
            String riskFlagsJson = toJson(responseBody.has("riskFlags")
                    ? responseBody.get("riskFlags")
                    : objectMapper.createArrayNode());

            return new AnalysisResult(
                    status,
                    summary,
                    riskFlagsJson,
                    toJson(responseBody)
            );
        } catch (RestClientException ex) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "AI analysis request failed");
        }
    }

    private DocumentAnalysisStatus parseStatus(JsonNode responseBody) {
        String status = textOrDefault(responseBody.get("status"), "");
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return DocumentAnalysisStatus.COMPLETED;
        }
        if ("FAILED".equalsIgnoreCase(status)) {
            return DocumentAnalysisStatus.FAILED;
        }
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "AI analysis returned an unknown status");
    }

    private String textOrDefault(JsonNode node, String defaultValue) {
        if (node == null || node.isNull() || node.asText().isBlank()) {
            return defaultValue;
        }
        return node.asText();
    }

    private String defaultSummary(DocumentAnalysisStatus status) {
        if (status == DocumentAnalysisStatus.COMPLETED) {
            return "AI analysis completed.";
        }
        return "AI analysis failed.";
    }

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to serialize AI analysis payload");
        }
    }
}
