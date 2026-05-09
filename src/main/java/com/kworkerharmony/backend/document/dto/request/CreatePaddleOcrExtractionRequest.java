package com.kworkerharmony.backend.document.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record CreatePaddleOcrExtractionRequest(
        @NotNull JsonNode ocrResult
) {
}
