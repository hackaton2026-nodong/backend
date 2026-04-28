package com.kworkerharmony.backend.document.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.kworkerharmony.backend.document.DocumentExtraction;
import com.kworkerharmony.backend.document.DocumentExtractionStatus;
import java.time.LocalDateTime;

public record DocumentExtractionResponse(
        String id,
        String documentId,
        DocumentExtractionStatus status,
        String schemaVersion,
        String sourceEngine,
        String sourceResultHash,
        JsonNode extractedPayload,
        JsonNode correctedPayload,
        String aiPayloadHash,
        String reviewRequiredReason,
        LocalDateTime extractedAt,
        LocalDateTime correctedAt
) {

    public static DocumentExtractionResponse from(DocumentExtraction extraction, ObjectMapper objectMapper) {
        return new DocumentExtractionResponse(
                extraction.getId(),
                extraction.getDocumentId(),
                extraction.getStatus(),
                extraction.getSchemaVersion(),
                extraction.getSourceEngine(),
                extraction.getSourceResultHash(),
                readJson(extraction.getExtractedPayload(), objectMapper),
                readJson(extraction.getCorrectedPayload(), objectMapper),
                extraction.getAiPayloadHash(),
                extraction.getReviewRequiredReason(),
                extraction.getExtractedAt(),
                extraction.getCorrectedAt()
        );
    }

    private static JsonNode readJson(String value, ObjectMapper objectMapper) {
        if (value == null || value.isBlank()) {
            return NullNode.getInstance();
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception ex) {
            return NullNode.getInstance();
        }
    }
}
