package com.kworkerharmony.backend.document.dto.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.kworkerharmony.backend.document.Document;
import com.kworkerharmony.backend.document.DocumentAnalysisResult;
import com.kworkerharmony.backend.document.DocumentAnalysisStatus;
import java.time.LocalDateTime;

public record DocumentAnalysisResponse(
        String id,
        String analysisId,
        String documentId,
        String caseId,
        DocumentAnalysisStatus status,
        String extractedTextHash,
        String analysisResultHash,
        String summary,
        JsonNode riskFlags,
        JsonNode issueCandidates,
        JsonNode generatedAnalysis,
        JsonNode findings,
        JsonNode fieldFindings,
        JsonNode citations,
        JsonNode recommendedActions,
        JsonNode relatedInstitutions,
        String caseStatus,
        JsonNode detailJson,
        String failedReason,
        LocalDateTime analyzedAt,
        LocalDateTime createdAt
) {

    public static DocumentAnalysisResponse from(DocumentAnalysisResult result, Document document, ObjectMapper objectMapper) {
        return new DocumentAnalysisResponse(
                result.getId(),
                result.getId(),
                result.getDocumentId(),
                document.getCaseId(),
                result.getStatus(),
                result.getExtractedTextHash(),
                result.getAnalysisResultHash(),
                result.getSummary(),
                parseJson(objectMapper, result.getRiskFlags(), "[]"),
                parseJson(objectMapper, result.getIssueCandidates(), "[]"),
                parseJson(objectMapper, result.getGeneratedAnalysis(), "{}"),
                parseJson(objectMapper, result.getFindings(), "[]"),
                parseJson(objectMapper, result.getFieldFindings(), "[]"),
                parseJson(objectMapper, result.getCitations(), "[]"),
                parseJson(objectMapper, result.getRecommendedActions(), "[]"),
                parseJson(objectMapper, result.getRelatedInstitutions(), "[]"),
                result.getCaseStatus(),
                parseJson(objectMapper, result.getDetailJson(), "{}"),
                result.getFailedReason(),
                result.getAnalyzedAt(),
                result.getCreatedAt()
        );
    }

    private static JsonNode parseJson(ObjectMapper objectMapper, String value, String defaultJson) {
        try {
            return objectMapper.readTree(value == null || value.isBlank() ? defaultJson : value);
        } catch (JsonProcessingException ex) {
            return JsonNodeFactory.instance.textNode(value);
        }
    }
}
