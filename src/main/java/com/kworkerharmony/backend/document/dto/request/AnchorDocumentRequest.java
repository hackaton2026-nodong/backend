package com.kworkerharmony.backend.document.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AnchorDocumentRequest(
        @NotBlank(message = "Signature id is required")
        String signatureId
) {
}
