package com.kworkerharmony.backend.document.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kworkerharmony.backend.document.Document;
import com.kworkerharmony.backend.document.DocumentExtraction;
import com.kworkerharmony.backend.document.DocumentStatus;
import com.kworkerharmony.backend.document.DocumentType;
import com.kworkerharmony.backend.document.port.DocumentAiAnalysisPort.AiAnalysisCommand;
import com.kworkerharmony.backend.global.exception.CustomException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DocumentAiPayloadBuilderTest {

    private static final String FIXTURE_ROOT = "/fixtures/spring/";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DocumentAiPayloadBuilder builder = new DocumentAiPayloadBuilder(objectMapper);

    @Test
    void buildsCommandFromSanitizedEmploymentContractPayload() throws Exception {
        JsonNode request = fixture("document-analysis-request-employment-contract.json");
        JsonNode payload = request.path("payload");
        Document document = document();
        DocumentExtraction extraction = extracted(payload);

        AiAnalysisCommand command = builder.build(document, extraction);

        assertThat(command.requestId()).hasSize(64);
        assertThat(command.documentId()).isEqualTo("document-uuid");
        assertThat(command.caseId()).isEqualTo("case-uuid");
        assertThat(command.documentHash()).isEqualTo("sha256-hex");
        assertThat(command.documentType()).isEqualTo("EMPLOYMENT_CONTRACT");
        assertThat(command.extractionId()).isEqualTo("extraction-uuid");
        assertThat(command.extractionStatus()).isEqualTo("EXTRACTED");
        assertThat(command.schemaVersion()).isEqualTo("employment-contract-v1");
        assertThat(command.sourceEngine()).isEqualTo("PADDLE_OCR");
        assertThat(command.sourceResultHash()).isEqualTo("source-result-hash");
        assertThat(command.aiPayloadHash()).isEqualTo(DocumentCrypto.sha256Hex(objectMapper.writeValueAsString(payload)));
        assertThat(command.payload().path("contractTerms").path("wage").path("amount").asInt()).isEqualTo(2_300_000);
        assertThat(command.payload().toString()).doesNotContain("rawOcrText", "layoutParsingResults", "storageKey");
    }

    @Test
    void correctedPayloadTakesPriorityOverExtractedPayload() throws Exception {
        JsonNode request = fixture("document-analysis-request-employment-contract.json");
        JsonNode payload = request.path("payload").deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) payload.path("contractTerms").path("wage")).put("amount", 2_500_000);
        DocumentExtraction extraction = extracted(request.path("payload"));
        extraction.markCorrected(objectMapper.writeValueAsString(payload), "corrected-hash");

        AiAnalysisCommand command = builder.build(document(), extraction);

        assertThat(command.extractionStatus()).isEqualTo("CORRECTED");
        assertThat(command.payload().path("contractTerms").path("wage").path("amount").asInt()).isEqualTo(2_500_000);
    }

    @Test
    void rejectsForbiddenRawOcrKeys() throws Exception {
        JsonNode request = fixture("document-analysis-request-blocked-raw-ocr.json");
        DocumentExtraction extraction = extracted(request.path("payload"));

        assertThatThrownBy(() -> builder.build(document(), extraction))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Raw OCR fields are not allowed");
    }

    private Document document() {
        Document document = new Document(
                "case-uuid",
                1L,
                DocumentType.EMPLOYMENT_CONTRACT.name(),
                "contract.pdf",
                "documents/document-uuid/contract.pdf",
                "application/pdf",
                1024L,
                "sha256-hex",
                null,
                DocumentStatus.STRUCTURED,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2027-05-31"),
                null,
                null
        );
        ReflectionTestUtils.setField(document, "id", "document-uuid");
        return document;
    }

    private DocumentExtraction extracted(JsonNode payload) throws IOException {
        DocumentExtraction extraction = new DocumentExtraction(
                "document-uuid",
                "employment-contract-v1",
                "PADDLE_OCR"
        );
        ReflectionTestUtils.setField(extraction, "id", "extraction-uuid");
        extraction.markExtracted(
                "source-result-hash",
                objectMapper.writeValueAsString(payload),
                "fixture-ai-payload-hash",
                null
        );
        return extraction;
    }

    private JsonNode fixture(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream(FIXTURE_ROOT + name)) {
            return objectMapper.readTree(Objects.requireNonNull(input, "Missing fixture " + name));
        }
    }
}
