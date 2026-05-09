package com.kworkerharmony.backend.document.dto.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.kworkerharmony.backend.document.Document;
import com.kworkerharmony.backend.document.DocumentAnalysisResult;
import com.kworkerharmony.backend.document.DocumentAnalysisStatus;
import java.time.LocalDateTime;
import java.util.List;

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
        LocalDateTime createdAt,
        List<AnalysisMessage> messages
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
                result.getCreatedAt(),
                List.of(
                        new AnalysisMessage("USER", "이 문서의 핵심 내용과 위험 요소를 분석해 주세요."),
                        new AnalysisMessage("ASSISTANT", result.getSummary() == null ? "분석 결과가 아직 없습니다." : result.getSummary()),
                        new AnalysisMessage("ASSISTANT", result.getRiskFlags() == null ? "확인된 위험 플래그가 없습니다." : result.getRiskFlags())
                )
        );
    }

    private static JsonNode parseJson(ObjectMapper objectMapper, String value, String defaultJson) {
        try {
            return objectMapper.readTree(value == null || value.isBlank() ? defaultJson : value);
        } catch (JsonProcessingException ex) {
            return JsonNodeFactory.instance.textNode(value);
        }
    }

    public record AnalysisMessage(
            String role,
            String content
    ) {
    }
}
