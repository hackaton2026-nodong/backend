package com.kworkerharmony.backend.document.dto.response;

import com.kworkerharmony.backend.document.Document;
import com.kworkerharmony.backend.document.DocumentType;
import java.time.LocalDate;

public record DocumentResponse(
        Long id,
        LocalDate issueDate,
        LocalDate expiryDate,
        DocumentType documentType,
        String rawData,
        Long userId
) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getIssueDate(),
                document.getExpiryDate(),
                document.getDocumentType(),
                document.getRawData(),
                document.getUser().getId()
        );
    }
}
