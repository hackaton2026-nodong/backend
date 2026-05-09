package com.kworkerharmony.backend.document.dto.response;

import com.kworkerharmony.backend.document.DocumentSignature;
import com.kworkerharmony.backend.document.DocumentSignatureStatus;
import java.time.LocalDateTime;

public record DocumentSignatureResponse(
        String signatureId,
        String documentId,
        String walletAddress,
        DocumentSignatureStatus status,
        LocalDateTime signedAt
) {

    public static DocumentSignatureResponse from(DocumentSignature signature) {
        return new DocumentSignatureResponse(
                signature.getId(),
                signature.getDocumentId(),
                signature.getWalletAddress(),
                signature.getStatus(),
                signature.getSignedAt()
        );
    }
}
