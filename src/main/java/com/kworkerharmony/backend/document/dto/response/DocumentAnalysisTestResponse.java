package com.kworkerharmony.backend.document.dto.response;

public record DocumentAnalysisTestResponse(
        String testCase,
        DocumentResponse document,
        DocumentExtractionResponse extraction,
        DocumentAnalysisResponse analysis
) {
}
