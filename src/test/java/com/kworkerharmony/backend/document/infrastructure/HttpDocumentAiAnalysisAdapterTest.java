package com.kworkerharmony.backend.document.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kworkerharmony.backend.document.config.DocumentAiProperties;
import com.kworkerharmony.backend.document.port.DocumentAiAnalysisPort.AiAnalysisCommand;
import com.kworkerharmony.backend.document.port.DocumentAiAnalysisPort.AiAnalysisResult;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class HttpDocumentAiAnalysisAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void disabledAiDoesNotReturnCompletedPlaceholder() throws Exception {
        HttpDocumentAiAnalysisAdapter adapter = new HttpDocumentAiAnalysisAdapter(
                properties(false, ""),
                RestClient.builder(),
                objectMapper
        );

        assertThatThrownBy(() -> adapter.analyze(command()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI analysis is disabled");
    }

    @Test
    void postRequestOptsIntoGeneratedAnalysis() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>("");
        HttpServer server = startServer(requestBody);
        try {
            int port = server.getAddress().getPort();
            HttpDocumentAiAnalysisAdapter adapter = new HttpDocumentAiAnalysisAdapter(
                    properties(true, "http://127.0.0.1:" + port + "/document-analysis"),
                    RestClient.builder(),
                    objectMapper
            );

            AiAnalysisResult result = adapter.analyze(command());

            assertThat(result.summary()).isEqualTo("분석 완료");
            assertThat(requestBody.get()).contains("\"includeGeneratedAnalysis\":true");
            assertThat(requestBody.get()).doesNotContain("rawOcrText");
        } finally {
            server.stop(0);
        }
    }

    private HttpServer startServer(AtomicReference<String> requestBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/document-analysis", exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();
            requestBody.set(new String(body, StandardCharsets.UTF_8));
            byte[] response = """
                    {
                      "status": "COMPLETED",
                      "summary": "분석 완료",
                      "riskFlags": [],
                      "generatedAnalysis": {
                        "status": "COMPLETED",
                        "text": "생성 분석 완료"
                      }
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        return server;
    }

    private AiAnalysisCommand command() throws Exception {
        return new AiAnalysisCommand(
                "request-1",
                "document-1",
                "case-1",
                "a".repeat(64),
                "EMPLOYMENT_CONTRACT",
                "extraction-1",
                "EXTRACTED",
                "employment-contract-extraction.v1",
                "PADDLE_OCR",
                "b".repeat(64),
                "c".repeat(64),
                objectMapper.readTree("""
                        {
                          "contractTerms": {
                            "wage": {
                              "amount": 2500000,
                              "currency": "KRW"
                            }
                          },
                          "evidenceRefs": []
                        }
                        """)
        );
    }

    private DocumentAiProperties properties(boolean enabled, String endpoint) {
        return new DocumentAiProperties(
                enabled,
                endpoint,
                "http://127.0.0.1:8000/health",
                1000L,
                "http://127.0.0.1:8000/chat/stream",
                "",
                1000L,
                1000L,
                0
        );
    }
}
