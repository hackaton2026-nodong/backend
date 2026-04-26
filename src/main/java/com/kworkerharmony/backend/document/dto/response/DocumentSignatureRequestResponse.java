package com.kworkerharmony.backend.document.dto.response;

import java.util.Map;

public record DocumentSignatureRequestResponse(
        String documentId,
        Long expectedChainId,
        Map<String, Object> domain,
        Map<String, Object> types,
        Map<String, Object> message,
        String typedDataHash
) {
}
