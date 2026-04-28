package com.kworkerharmony.backend.document;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentExtractionRepository extends JpaRepository<DocumentExtraction, String> {

    Optional<DocumentExtraction> findByDocumentId(String documentId);
}
