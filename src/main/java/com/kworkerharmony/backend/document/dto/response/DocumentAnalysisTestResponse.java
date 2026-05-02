package com.kworkerharmony.backend.document.dto.response;

public record DocumentAnalysisTestResponse(
        String scenario,
        DocumentResponse document,
        DocumentExtractionResponse extraction,
        DocumentAnalysisResponse analysis
) {
}
