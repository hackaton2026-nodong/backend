package com.kworkerharmony.backend.document.support;

public record EmploymentContractExtractionPayload(
        String payloadJson,
        String aiPayloadHash,
        String reviewRequiredReason
) {
}
