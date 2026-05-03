package com.kworkerharmony.backend.document.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kworkerharmony.backend.document.config.DocumentAiProperties;
import com.kworkerharmony.backend.document.port.DocumentAiAnalysisPort.AiAnalysisCommand;
import com.kworkerharmony.backend.document.port.DocumentAiAnalysisPort.AiAnalysisResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class HttpDocumentAiAnalysisAdapterTest {

    private static final String FIXTURE_ROOT = "/fixtures/spring/";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void postsAnalysisRequestWithInternalTokenAndOutputRequest() throws Exception {
        JsonNode request = fixture("document-analysis-request-employment-contract.json");
        String responseJson = fixtureText("document-analysis-response-completed-minimum.json");
        AtomicReference<String> receivedToken = new AtomicReference<>();
        AtomicReference<String> receivedBody = new AtomicReference<>();
        HttpServer server = startServer(exchange -> {
            receivedToken.set(exchange.getRequestHeaders().getFirst("X-AI-Internal-Token"));
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, responseJson);
        });

        try {
            HttpDocumentAiAnalysisAdapter adapter = adapter(properties(server, true, "spring-token", 1));
            AiAnalysisResult result = adapter.analyze(commandFrom(request));

            assertThat(receivedToken.get()).isEqualTo("spring-token");
            JsonNode body = objectMapper.readTree(receivedBody.get());
            assertThat(body.path("documentType").asText()).isEqualTo("EMPLOYMENT_CONTRACT");
            assertThat(body.path("payload").path("contractTerms").path("wage").path("amount").asInt()).isEqualTo(2_300_000);
            assertThat(body.path("outputRequest").path("languageCode").asText()).isEqualTo("ko");
            assertThat(body.path("outputRequest").path("includeGeneratedAnalysis").asBoolean()).isFalse();
            assertThat(result.summary()).contains("최저임금");
            assertThat(result.riskFlags()).contains("MINIMUM_WAGE_REVIEW_REQUIRED");
            assertThat(result.analysisResultHash()).hasSize(64);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void blocksRawOcrPayloadBeforeCallingAiServer() throws Exception {
        JsonNode request = fixture("document-analysis-request-blocked-raw-ocr.json");
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = startServer(exchange -> {
            calls.incrementAndGet();
            respond(exchange, 200, "{}");
        });

        try {
            HttpDocumentAiAnalysisAdapter adapter = adapter(properties(server, true, "", 0));

            assertThatThrownBy(() -> adapter.analyze(commandFrom(request)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden field")
                    .hasMessageContaining("rawOcrText");
            assertThat(calls).hasValue(0);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void retriesOnceWhenAiServerReturns5xx() throws Exception {
        JsonNode request = fixture("document-analysis-request-employment-contract.json");
        String responseJson = fixtureText("document-analysis-response-completed-minimum.json");
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = startServer(exchange -> {
            if (calls.incrementAndGet() == 1) {
                respond(exchange, 503, "{\"detail\":\"warming up\"}");
                return;
            }
            respond(exchange, 200, responseJson);
        });

        try {
            HttpDocumentAiAnalysisAdapter adapter = adapter(properties(server, true, "", 1));
            AiAnalysisResult result = adapter.analyze(commandFrom(request));

            assertThat(calls).hasValue(2);
            assertThat(result.summary()).contains("최저임금");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void acceptsJsonBodyEvenWhenAiServerUsesOctetStreamContentType() throws Exception {
        JsonNode request = fixture("document-analysis-request-employment-contract.json");
        String responseJson = fixtureText("document-analysis-response-completed-minimum.json");
        HttpServer server = startServer(exchange -> respond(exchange, 200, responseJson, MediaType.APPLICATION_OCTET_STREAM_VALUE));

        try {
            HttpDocumentAiAnalysisAdapter adapter = adapter(properties(server, true, "", 0));

            AiAnalysisResult result = adapter.analyze(commandFrom(request));

            assertThat(result.summary()).contains("최저임금");
            assertThat(result.riskFlags()).contains("MINIMUM_WAGE_REVIEW_REQUIRED");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void usesPlaceholderWhenAiIntegrationIsDisabled() throws Exception {
        JsonNode request = fixture("document-analysis-request-employment-contract.json");
        HttpDocumentAiAnalysisAdapter adapter = adapter(new DocumentAiProperties(
                false,
                "",
                "http://localhost:8000/health",
                2000L,
                "http://localhost:8000/chat/stream",
                "",
                3000L,
                35000L,
                1
        ));

        AiAnalysisResult result = adapter.analyze(commandFrom(request));

        assertThat(result.summary()).contains("Placeholder analysis result");
        assertThat(result.riskFlags()).isEqualTo("[]");
        assertThat(result.analysisResultHash()).hasSize(64);
    }

    private HttpDocumentAiAnalysisAdapter adapter(DocumentAiProperties properties) {
        return new HttpDocumentAiAnalysisAdapter(properties, RestClient.builder(), objectMapper);
    }

    private DocumentAiProperties properties(HttpServer server, boolean enabled, String token, int maxRetries) {
        return new DocumentAiProperties(
                enabled,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/document-analysis",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/health",
                2000L,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/chat/stream",
                token,
                1000L,
                1000L,
                maxRetries
        );
    }

    private AiAnalysisCommand commandFrom(JsonNode request) {
        return new AiAnalysisCommand(
                request.path("requestId").asText("analysis-request-hash"),
                request.path("documentId").asText("document-uuid"),
                request.path("caseId").asText("case-uuid"),
                request.path("documentHash").asText("sha256-hex"),
                request.path("documentType").asText("EMPLOYMENT_CONTRACT"),
                request.path("extractionId").asText("extraction-uuid"),
                request.path("extractionStatus").asText("EXTRACTED"),
                request.path("schemaVersion").asText("employment-contract-v1"),
                request.path("sourceEngine").asText("PADDLE_OCR"),
                request.path("sourceResultHash").asText("sha256-hex"),
                request.path("aiPayloadHash").asText("sha256-hex"),
                request.path("payload")
        );
    }

    private JsonNode fixture(String name) throws IOException {
        return objectMapper.readTree(fixtureText(name));
    }

    private String fixtureText(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream(FIXTURE_ROOT + name)) {
            return new String(Objects.requireNonNull(input, "Missing fixture " + name).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private HttpServer startServer(ExchangeHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/document-analysis", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
        return server;
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        respond(exchange, status, body, MediaType.APPLICATION_JSON_VALUE);
    }

    private void respond(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
