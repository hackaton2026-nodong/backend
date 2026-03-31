package com.kworkerharmony.backend.document.dto.request;

import com.kworkerharmony.backend.document.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateDocumentRequest(
        @NotNull(message = "Issue date is required")
        LocalDate issueDate,

        @NotNull(message = "Expiry date is required")
        LocalDate expiryDate,

        @NotNull(message = "Document type is required")
        DocumentType documentType,

        @NotBlank(message = "Raw data is required")
        String rawData,

        @NotNull(message = "User id is required")
        Long userId
) {
}
