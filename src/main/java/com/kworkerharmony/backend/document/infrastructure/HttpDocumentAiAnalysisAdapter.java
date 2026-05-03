package com.kworkerharmony.backend.document.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kworkerharmony.backend.document.config.DocumentAiProperties;
import com.kworkerharmony.backend.document.port.DocumentAiAnalysisPort;
import com.kworkerharmony.backend.document.support.DocumentCrypto;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HttpDocumentAiAnalysisAdapter implements DocumentAiAnalysisPort {

    private static final String INTERNAL_TOKEN_HEADER = "X-AI-Internal-Token";
    private static final Set<String> FORBIDDEN_PAYLOAD_KEYS = Set.of(
            "rawtext",
            "rawocrtext",
            "ocrtext",
            "plaintext",
            "documenttext",
            "file",
            "filebytes",
            "image",
            "imagebytes",
            "base64",
            "passportnumber",
            "alienregistrationnumber",
            "residentregistrationnumber",
            "phonenumber",
            "email"
    );

    private final DocumentAiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpDocumentAiAnalysisAdapter(
            DocumentAiProperties properties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .requestFactory(requestFactory(properties))
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public AiAnalysisResult analyze(AiAnalysisCommand command) {
        String requestJson = jsonBody(command);
        if (!properties.enabled() || properties.endpoint().isBlank()) {
            return placeholderResult(command, requestJson);
        }

        String responseJson = postWithRetry(requestJson);
        return parseResponse(command, requestJson, responseJson);
    }

    private SimpleClientHttpRequestFactory requestFactory(DocumentAiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMillis()));
        factory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMillis()));
        return factory;
    }

    private String postWithRetry(String requestJson) {
        RuntimeException lastFailure = null;
        int attempts = properties.maxRetries() + 1;
        for (int attempt = 1; attempt <= attempts; attempt += 1) {
            try {
                byte[] body = restClient.post()
                        .uri(properties.endpoint())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers -> {
                            if (!properties.internalToken().isBlank()) {
                                headers.set(INTERNAL_TOKEN_HEADER, properties.internalToken());
                            }
                        })
                        .body(requestJson)
                        .retrieve()
                        .body(byte[].class);
                return body == null ? "" : new String(body, StandardCharsets.UTF_8);
            } catch (RestClientResponseException ex) {
                if (!shouldRetry(ex.getStatusCode(), attempt, attempts)) {
                    throw new IllegalStateException("AI analysis request failed with HTTP " + ex.getStatusCode().value(), ex);
                }
                lastFailure = ex;
            } catch (ResourceAccessException ex) {
                if (attempt >= attempts) {
                    throw new IllegalStateException("AI analysis request failed because the AI server was unavailable", ex);
                }
                lastFailure = ex;
            }
        }
        throw new IllegalStateException("AI analysis request failed", lastFailure);
    }

    private boolean shouldRetry(HttpStatusCode statusCode, int attempt, int attempts) {
        return attempt < attempts && statusCode.is5xxServerError();
    }

    private AiAnalysisResult placeholderResult(AiAnalysisCommand command, String requestJson) {
        String inputHash = DocumentCrypto.sha256Hex(requestJson);
        String canonical = "placeholder-analysis|" + command.documentId() + "|" + inputHash;
        String summary = "Placeholder analysis result for document " + command.documentId();
        ObjectNode detail = objectMapper.createObjectNode();
        detail.put("status", "COMPLETED");
        detail.put("summary", summary);
        detail.putArray("riskFlags");
        return new AiAnalysisResult(
                inputHash,
                DocumentCrypto.sha256Hex(canonical),
                summary,
                "[]",
                "[]",
                "{\"status\":\"SKIPPED\",\"reason\":\"AI integration is disabled\"}",
                "[]",
                "[]",
                "[]",
                "[]",
                "[]",
                null,
                writeJson(detail),
                null
        );
    }

    private AiAnalysisResult parseResponse(AiAnalysisCommand command, String requestJson, String responseJson) {
        String inputHash = DocumentCrypto.sha256Hex(requestJson);
        if (responseJson == null || responseJson.isBlank()) {
            String summary = "AI analysis completed";
            return new AiAnalysisResult(
                    inputHash,
                    DocumentCrypto.sha256Hex("empty-ai-response|" + command.documentId() + "|" + inputHash),
                    summary,
                    "[]",
                    "[]",
                    "{}",
                    "[]",
                    "[]",
                    "[]",
                    "[]",
                    "[]",
                    null,
                    "{\"status\":\"COMPLETED\",\"summary\":\"AI analysis completed\"}",
                    null
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
            return new AiAnalysisResult(
                    inputHash,
                    analysisResultHash,
                    summary,
                    riskFlags,
                    jsonField(response, "issueCandidates", "[]"),
                    jsonField(response, "generatedAnalysis", "{}"),
                    jsonField(response, "findings", "[]"),
                    jsonField(response, "fieldFindings", "[]"),
                    jsonField(response, "citations", "[]"),
                    jsonField(response, "recommendedActions", "[]"),
                    jsonField(response, "relatedInstitutions", "[]"),
                    response.path("caseStatus").isMissingNode() ? null : response.path("caseStatus").asText(null),
                    objectMapper.writeValueAsString(response),
                    response.path("failedReason").asText(null)
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid AI analysis response", ex);
        }
    }

    private String jsonField(JsonNode node, String fieldName, String defaultJson) throws JsonProcessingException {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull()
                ? defaultJson
                : objectMapper.writeValueAsString(value);
    }

    private String writeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize AI analysis placeholder", ex);
        }
    }

    private String jsonBody(AiAnalysisCommand command) {
        validateNoForbiddenKeys(command.payload());
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
        ObjectNode outputRequest = root.putObject("outputRequest");
        outputRequest.put("languageCode", "ko");
        outputRequest.put("includeGeneratedAnalysis", false);
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize AI analysis request", ex);
        }
    }

    private void validateNoForbiddenKeys(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String normalizedKey = entry.getKey().toLowerCase(Locale.ROOT);
                if (FORBIDDEN_PAYLOAD_KEYS.contains(normalizedKey)) {
                    throw new IllegalArgumentException("AI analysis payload contains forbidden field: " + entry.getKey());
                }
                validateNoForbiddenKeys(entry.getValue());
            });
            return;
        }
        if (node.isArray()) {
            node.elements().forEachRemaining(this::validateNoForbiddenKeys);
        }
    }
}
