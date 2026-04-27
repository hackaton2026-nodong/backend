package com.kworkerharmony.backend.document.port;

import com.kworkerharmony.backend.document.DocumentAnalysisStatus;
import java.time.LocalDate;
import java.util.List;

public interface DocumentAnalysisPort {

    AnalysisResult analyze(AnalysisCommand command);

    record AnalysisCommand(
            String requestId,
            DocumentPayload document,
            CaseContextPayload caseContext,
            ChecklistContextPayload checklistContext,
            OutputRequestPayload outputRequest
    ) {
    }

    record DocumentPayload(
            String documentId,
            String caseId,
            String documentHash,
            String documentType,
            LocalDate issuedAt,
            LocalDate expiresAt
    ) {
    }

    record CaseContextPayload(
            String industry,
            String region,
            String languageCode,
            String workerStatusCategory
    ) {
    }

    record ChecklistContextPayload(
            String catalogCode,
            List<String> candidateItemCodes
    ) {
    }

    record OutputRequestPayload(
            String languageCode,
            boolean includeChecklistSuggestions,
            boolean includeEvidenceRefs
    ) {
    }

    record AnalysisResult(
            DocumentAnalysisStatus status,
            String summary,
            String riskFlagsJson,
            String responseBodyJson
    ) {
    }
}
