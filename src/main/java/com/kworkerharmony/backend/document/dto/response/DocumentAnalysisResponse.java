package com.kworkerharmony.backend.document.dto.response;

import com.kworkerharmony.backend.document.DocumentAnalysisResult;
import com.kworkerharmony.backend.document.DocumentAnalysisStatus;
import java.time.LocalDateTime;

public record DocumentAnalysisResponse(
        String id,
        String documentId,
        DocumentAnalysisStatus status,
        String extractedTextHash,
        String analysisResultHash,
        String summary,
        String riskFlags,
        LocalDateTime analyzedAt
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
                result.getAnalyzedAt()
        );
    }
}
