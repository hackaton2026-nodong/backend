package com.kworkerharmony.backend.document.dto.response;

public record DocumentAnalysisUploadResponse(
        String scenario,
        String caseId,
        DocumentResponse document,
        DocumentExtractionResponse extraction,
        DocumentAnalysisResponse analysis
) {
}
