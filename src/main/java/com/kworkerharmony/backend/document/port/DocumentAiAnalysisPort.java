package com.kworkerharmony.backend.document.port;

import com.fasterxml.jackson.databind.JsonNode;

public interface DocumentAiAnalysisPort {

    AiAnalysisResult analyze(AiAnalysisCommand command);

    record AiAnalysisCommand(
            String requestId,
            String documentId,
            String caseId,
            String documentHash,
            String documentType,
            String extractionId,
            String extractionStatus,
            String schemaVersion,
            String sourceEngine,
            String sourceResultHash,
            String aiPayloadHash,
            JsonNode payload
    ) {
    }

    record AiAnalysisResult(
            String inputHash,
            String analysisResultHash,
            String summary,
            String riskFlags,
            String issueCandidates,
            String generatedAnalysis,
            String findings,
            String fieldFindings,
            String citations,
            String recommendedActions,
            String relatedInstitutions,
            String caseStatus,
            String detailJson,
            String failedReason
    ) {
    }
}
