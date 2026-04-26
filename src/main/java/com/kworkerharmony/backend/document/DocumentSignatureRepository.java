package com.kworkerharmony.backend.document;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentSignatureRepository extends JpaRepository<DocumentSignature, String> {

    Optional<DocumentSignature> findByDocumentIdAndUserIdAndNonce(String documentId, Long userId, String nonce);

    Optional<DocumentSignature> findFirstByDocumentIdAndStatusOrderByCreatedAtDesc(
            String documentId,
            DocumentSignatureStatus status
    );
}
