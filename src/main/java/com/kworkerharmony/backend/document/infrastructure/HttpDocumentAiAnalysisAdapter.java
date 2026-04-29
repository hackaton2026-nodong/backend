package com.kworkerharmony.backend.document.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kworkerharmony.backend.document.config.DocumentAiProperties;
import com.kworkerharmony.backend.document.port.DocumentAiAnalysisPort;
import com.kworkerharmony.backend.document.support.DocumentCrypto;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpDocumentAiAnalysisAdapter implements DocumentAiAnalysisPort {

    private final DocumentAiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpDocumentAiAnalysisAdapter(
            DocumentAiProperties properties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public AiAnalysisResult analyze(AiAnalysisCommand command) {
        String requestJson = jsonBody(command);
        if (!properties.enabled() || properties.endpoint().isBlank()) {
            return placeholderResult(command, requestJson);
        }

        String responseJson = restClient.post()
                .uri(properties.endpoint())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestJson)
                .retrieve()
                .body(String.class);

        return parseResponse(command, requestJson, responseJson);
    }

    private AiAnalysisResult placeholderResult(AiAnalysisCommand command, String requestJson) {
        String inputHash = DocumentCrypto.sha256Hex(requestJson);
        String canonical = "placeholder-analysis|" + command.documentId() + "|" + inputHash;
        return new AiAnalysisResult(
                inputHash,
                DocumentCrypto.sha256Hex(canonical),
                "Placeholder analysis result for document " + command.documentId(),
                "[]"
        );
    }

    private AiAnalysisResult parseResponse(AiAnalysisCommand command, String requestJson, String responseJson) {
        String inputHash = DocumentCrypto.sha256Hex(requestJson);
        if (responseJson == null || responseJson.isBlank()) {
            return new AiAnalysisResult(
                    inputHash,
                    DocumentCrypto.sha256Hex("empty-ai-response|" + command.documentId() + "|" + inputHash),
                    "AI analysis completed",
                    "[]"
            );
        }
        try {
            JsonNode response = objectMapper.readTree(responseJson);
            String status = response.path("status").asText("COMPLETED");
            if (!"COMPLETED".equalsIgnoreCase(status)) {
                throw new IllegalStateException("AI analysis failed with status " + status);
            }
            String summary = response.path("summary").asText("AI analysis completed");
            String riskFlags = "[]";
            if (response.has("riskFlags")) {
                JsonNode riskFlagsNode = response.path("riskFlags");
                riskFlags = riskFlagsNode.isTextual()
                        ? riskFlagsNode.asText()
                        : objectMapper.writeValueAsString(riskFlagsNode);
            }
            String analysisResultHash = response.path("analysisResultHash").asText("");
            if (analysisResultHash.isBlank()) {
                analysisResultHash = DocumentCrypto.sha256Hex(objectMapper.writeValueAsString(response));
            }
            return new AiAnalysisResult(inputHash, analysisResultHash, summary, riskFlags);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid AI analysis response", ex);
        }
    }

    private String jsonBody(AiAnalysisCommand command) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("requestId", command.requestId());
        root.put("documentId", command.documentId());
        root.put("caseId", command.caseId());
        root.put("documentHash", command.documentHash());
        root.put("documentType", command.documentType());
        root.put("extractionId", command.extractionId());
        root.put("extractionStatus", command.extractionStatus());
        root.put("schemaVersion", command.schemaVersion());
        root.put("sourceEngine", command.sourceEngine());
        root.put("sourceResultHash", command.sourceResultHash());
        root.put("aiPayloadHash", command.aiPayloadHash());
        root.set("payload", command.payload());
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize AI analysis request", ex);
        }
    }
}
