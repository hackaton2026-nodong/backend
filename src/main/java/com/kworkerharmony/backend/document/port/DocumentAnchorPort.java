package com.kworkerharmony.backend.document.port;

public interface DocumentAnchorPort {

    String anchor(String documentId, String sha256Hash);
}
