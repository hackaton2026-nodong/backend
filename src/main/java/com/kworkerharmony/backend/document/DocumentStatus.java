package com.kworkerharmony.backend.document;

public enum DocumentStatus {
    UPLOADED,
    STORED,
    HASHED,
    ANCHORED_ON_CHAIN,
    OCR_PROCESSING,
    OCR_COMPLETED,
    STRUCTURED,
    ANALYZED,
    FAILED
}
