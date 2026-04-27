package com.kworkerharmony.backend.document.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kworkerharmony.backend.document.DocumentAnalysisStatus;
import com.kworkerharmony.backend.document.port.DocumentAnalysisPort;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.document.analysis.ai", name = "enabled", havingValue = "false", matchIfMissing = true)
public class StubDocumentAnalysisAdapter implements DocumentAnalysisPort {

    private final ObjectMapper objectMapper;

    public StubDocumentAnalysisAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public AnalysisResult analyze(AnalysisCommand command) {
        ObjectNode riskFlag = objectMapper.createObjectNode()
                .put("code", "AI_ANALYSIS_STUB")
                .put("severity", "LOW")
                .put("message", "실제 AI 분석 서비스가 연결되지 않아 문서 메타데이터 기준으로만 응답했습니다.");
        riskFlag.putArray("evidenceIds");

        ArrayNode riskFlags = objectMapper.createArrayNode().add(riskFlag);
        ObjectNode responseBody = objectMapper.createObjectNode();
        responseBody.put("requestId", command.requestId());
        responseBody.put("status", DocumentAnalysisStatus.COMPLETED.name());
        responseBody.put("summary", "문서 업로드와 해시는 완료되었습니다. 실제 AI 분석 서비스 연결 후 근로조건과 리스크 검토 결과가 이 응답으로 내려옵니다.");
        responseBody.set("riskFlags", riskFlags);

        return new AnalysisResult(
                DocumentAnalysisStatus.COMPLETED,
                responseBody.get("summary").asText(),
                toJson(riskFlags),
                toJson(responseBody)
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to serialize stub analysis payload");
        }
    }
}
