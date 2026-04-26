package com.kworkerharmony.backend.document;

public enum DocumentStatus {
    UPLOADED,
    STORED,
    HASHED,
    SIGNATURE_REQUESTED,
    SIGNED,
    ANCHOR_PENDING,
    ANCHORED_ON_CHAIN,
    ANCHOR_FAILED,
    OCR_PROCESSING,
    OCR_COMPLETED,
    STRUCTURED,
    ANALYZED,
    FAILED
}
