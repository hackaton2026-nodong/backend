package com.kworkerharmony.backend.document.port;

public interface DocumentOcrPort {

    void requestOcr(OcrCommand command);

    record OcrCommand(
            String documentId,
            String caseId,
            String documentType,
            String storageKey,
            String sha256Hash,
            String callbackUrl
    ) {
    }
}
