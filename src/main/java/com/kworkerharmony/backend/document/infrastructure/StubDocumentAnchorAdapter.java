package com.kworkerharmony.backend.document.infrastructure;

import com.kworkerharmony.backend.document.port.DocumentAnchorPort;
import org.springframework.stereotype.Component;

@Component
public class StubDocumentAnchorAdapter implements DocumentAnchorPort {

    @Override
    public String anchor(String documentId, String sha256Hash) {
        return "stub-anchor-" + documentId;
    }
}
