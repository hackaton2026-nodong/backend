package com.kworkerharmony.backend.document.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kworkerharmony.backend.document.Document;
import com.kworkerharmony.backend.document.DocumentExtraction;
import com.kworkerharmony.backend.document.port.DocumentAiAnalysisPort.AiAnalysisCommand;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DocumentAiPayloadBuilder {

    private static final Set<String> FORBIDDEN_KEYS = Set.of(
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
            "email",
            "storagekey",
            "layoutparsingresults",
            "parsing_res_list",
            "block_content",
            "markdown"
    );

    private final ObjectMapper objectMapper;

    public DocumentAiPayloadBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiAnalysisCommand build(Document document, DocumentExtraction extraction) {
        String payloadJson = extraction.getCorrectedPayload() == null || extraction.getCorrectedPayload().isBlank()
                ? extraction.getExtractedPayload()
                : extraction.getCorrectedPayload();
        JsonNode payload = readJson(payloadJson);
        validateSanitizedPayload(payload);
        String aiPayloadHash = DocumentCrypto.sha256Hex(canonicalJson(payload));
        return new AiAnalysisCommand(
                DocumentCrypto.sha256Hex("analysis-request|" + document.getId() + "|" + aiPayloadHash),
                document.getId(),
                document.getCaseId(),
                document.getSha256Hash(),
                document.getDocumentType(),
                extraction.getId(),
                extraction.getStatus().name(),
                extraction.getSchemaVersion(),
                extraction.getSourceEngine(),
                extraction.getSourceResultHash(),
                aiPayloadHash,
                payload
        );
    }

    private JsonNode readJson(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Extraction payload is required");
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException ex) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Invalid extraction payload JSON");
        }
    }

    private void validateSanitizedPayload(JsonNode root) {
        Queue<JsonNode> nodes = new ArrayDeque<>();
        nodes.add(root);
        while (!nodes.isEmpty()) {
            JsonNode node = nodes.remove();
            if (node.isObject()) {
                node.fields().forEachRemaining(entry -> {
                    String normalizedKey = entry.getKey().toLowerCase(Locale.ROOT);
                    if (FORBIDDEN_KEYS.contains(normalizedKey)) {
                        throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Raw OCR fields are not allowed");
                    }
                    nodes.add(entry.getValue());
                });
                continue;
            }
            if (node.isTextual() && containsSensitiveIdentifier(node.asText())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Sensitive identifiers are not allowed");
            }
            node.elements().forEachRemaining(nodes::add);
        }
    }

    private boolean containsSensitiveIdentifier(String value) {
        return value.matches(".*[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}.*")
                || value.matches(".*01[016789]-?\\d{3,4}-?\\d{4}.*")
                || value.matches(".*0\\d{1,2}-\\d{3,4}-\\d{4}.*")
                || value.matches(".*\\d{3}-\\d{2}-\\d{5}.*");
    }

    private String canonicalJson(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Invalid JSON payload");
        }
    }
}
