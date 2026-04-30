package com.kworkerharmony.backend.document.dto.response;

import com.kworkerharmony.backend.document.DocumentAnalysisResult;
import com.kworkerharmony.backend.document.DocumentAnalysisStatus;
import java.time.LocalDateTime;
import java.util.List;

public record DocumentAnalysisResponse(
        String id,
        String documentId,
        DocumentAnalysisStatus status,
        String extractedTextHash,
        String analysisResultHash,
        String summary,
        String riskFlags,
        LocalDateTime analyzedAt,
        List<AnalysisMessage> messages
) {

    public static DocumentAnalysisResponse from(DocumentAnalysisResult result) {
        return new DocumentAnalysisResponse(
                result.getId(),
                result.getDocumentId(),
                result.getStatus(),
                result.getExtractedTextHash(),
                result.getAnalysisResultHash(),
                result.getSummary(),
                result.getRiskFlags(),
                result.getAnalyzedAt(),
                List.of(
                        new AnalysisMessage("USER", "이 문서의 핵심 내용과 위험 요소를 분석해 주세요."),
                        new AnalysisMessage("ASSISTANT", result.getSummary() == null ? "분석 결과가 아직 없습니다." : result.getSummary()),
                        new AnalysisMessage("ASSISTANT", result.getRiskFlags() == null ? "확인된 위험 플래그가 없습니다." : result.getRiskFlags())
                )
        );
    }

    public record AnalysisMessage(
            String role,
            String content
    ) {
    }
}
