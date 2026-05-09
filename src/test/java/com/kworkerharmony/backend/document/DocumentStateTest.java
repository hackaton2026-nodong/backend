package com.kworkerharmony.backend.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DocumentStateTest {

    @Test
    void createUploadedStartsInUploadedStateWithNoStorageMetadata() {
        Document document = Document.createUploaded(
                10L,
                DocumentType.EMPLOYMENT_CONTRACT,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1)
        );

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(document.getUploaderUserId()).isEqualTo(10L);
        assertThat(document.getStorageKey()).isNull();
        assertThat(document.getSha256Hash()).isNull();
    }

    @Test
    void allowedStateTransitionsUpdateDocumentMetadata() {
        Document document = Document.createUploaded(
                10L,
                DocumentType.EMPLOYMENT_CONTRACT,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1)
        );

        document.assignToCase("case-1");
        document.markStored("contract.pdf", "docs/case-1/doc-1", "application/pdf", 128L);
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.STORED);
        assertThat(document.getStorageKey()).isEqualTo("docs/case-1/doc-1");
        assertThat(document.getOriginalFileName()).isEqualTo("contract.pdf");

        document.markHashed("abc123");
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.HASHED);
        assertThat(document.getSha256Hash()).isEqualTo("abc123");

        document.markOcrProcessing();
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.OCR_PROCESSING);

        document.markOcrCompleted();
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.OCR_COMPLETED);
        assertThat(document.getOcrCompletedAt()).isNotNull();

        document.markStructured();
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.STRUCTURED);

        document.markAnalyzed();
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.ANALYZED);
        assertThat(document.getAnalyzedAt()).isNotNull();
    }

    @Test
    void failedStateCanBeFollowedBySubsequentProcessingWithCurrentImplementation() {
        Document document = Document.createUploaded(
                10L,
                DocumentType.EMPLOYMENT_CONTRACT,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1)
        );

        document.markFailed();
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);

        document.markStored("contract.pdf", "docs/case-1/doc-1", "application/pdf", 128L);
        document.markHashed("abc123");
        document.markOcrProcessing();

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.OCR_PROCESSING);
    }
}
