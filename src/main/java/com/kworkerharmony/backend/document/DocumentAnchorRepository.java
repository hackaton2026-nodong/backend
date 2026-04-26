package com.kworkerharmony.backend.document;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentAnchorRepository extends JpaRepository<DocumentAnchor, String> {

    Optional<DocumentAnchor> findFirstByDocumentIdOrderByCreatedAtDesc(String documentId);

    Optional<DocumentAnchor> findFirstByDocumentIdAndStatusInOrderByCreatedAtDesc(
            String documentId,
            Collection<DocumentAnchorStatus> statuses
    );

    Optional<DocumentAnchor> findByDocumentIdAndSignatureId(String documentId, String signatureId);
}
