package com.kworkerharmony.backend.document;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentAnalysisResultRepository extends JpaRepository<DocumentAnalysisResult, String> {

    Optional<DocumentAnalysisResult> findByDocumentId(String documentId);
}
