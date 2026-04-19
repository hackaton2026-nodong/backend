package com.kworkerharmony.backend.document.dto.request;

import com.kworkerharmony.backend.document.DocumentType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateDocumentRequest(
        @NotNull(message = "Issue date is required")
        LocalDate issueDate,

        @NotNull(message = "Expiry date is required")
        LocalDate expiryDate,

        @NotNull(message = "Document type is required")
        DocumentType documentType,

        @NotNull(message = "Case id is required")
        String caseId
) {
}
