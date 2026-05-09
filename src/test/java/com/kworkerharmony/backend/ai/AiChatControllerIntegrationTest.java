package com.kworkerharmony.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import com.kworkerharmony.backend.enterprise.Enterprise;
import com.kworkerharmony.backend.enterprise.EnterpriseRepository;
import com.kworkerharmony.backend.enterprise.EnterpriseStatus;
import com.kworkerharmony.backend.global.security.JwtProvider;
import com.kworkerharmony.backend.global.security.RedisTokenRepository;
import com.kworkerharmony.backend.user.Role;
import com.kworkerharmony.backend.user.User;
import com.kworkerharmony.backend.user.UserRepository;
import com.kworkerharmony.backend.user.UserStatus;
import com.kworkerharmony.backend.user.UserType;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.document.storage-root=build/test-uploads")
@AutoConfigureMockMvc
@Transactional
class AiChatControllerIntegrationTest {

    private static final String INTERNAL_TOKEN = "spring-internal-token";
    private static final AtomicReference<String> receivedToken = new AtomicReference<>();
    private static final AtomicReference<String> receivedBody = new AtomicReference<>();
    private static final HttpServer aiServer = startAiServer();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private RedisTokenRepository redisTokenRepository;

    @DynamicPropertySource
    static void aiProperties(DynamicPropertyRegistry registry) {
        registry.add("app.document.ai.enabled", () -> "true");
        registry.add("app.document.ai.chat-stream-endpoint",
                () -> "http://127.0.0.1:" + aiServer.getAddress().getPort() + "/chat/stream");
        registry.add("app.document.ai.internal-token", () -> INTERNAL_TOKEN);
        registry.add("app.document.ai.read-timeout-millis", () -> "5000");
    }

    @AfterAll
    static void stopAiServer() {
        aiServer.stop(0);
    }

    @Test
    void proxiesFastApiSseEventsThroughSpring() throws Exception {
        String accessToken = jwtProvider.generateAccessToken(activeUser());

        MvcResult result = mockMvc.perform(post("/api/ai/chat/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .content("""
                                {
                                  "message": "포괄임금제인데 야근수당을 못 받았어요",
                                  "languageCode": "ko",
                                  "topK": 3,
                                  "history": [
                                    {
                                      "role": "user",
                                      "content": "포괄임금제 질문"
                                    },
                                    {
                                      "role": "assistant",
                                      "content": "연장근로수당 확인이 필요합니다."
                                    }
                                  ],
                                  "caseContext": {
                                    "documentId": "document-1",
                                    "documentStatus": "AI_REVIEWED",
                                    "riskLevel": "높음",
                                    "contractPeriod": "2026.06.01 ~ 2027.05.31",
                                    "analysisStatus": "COMPLETED",
                                    "analysisSummary": "근로시간과 휴일 항목 확인이 필요합니다.",
                                    "issueCandidates": ["MINIMUM_WAGE", "OVERTIME_PAY"],
                                    "riskFlags": [
                                      {
                                        "label": "연장근로수당 확인",
                                        "level": "중간",
                                        "description": "수당 항목 검토"
                                      }
                                    ],
                                    "findings": [
                                      {
                                        "title": "휴일 항목 누락",
                                        "description": "계약서에 휴일이 명확하지 않습니다.",
                                        "severity": "중간"
                                      }
                                    ],
                                    "recommendedActions": [
                                      {
                                        "label": "근로계약서 보완 요청",
                                        "institutionName": "사업장",
                                        "expectedPath": "누락 항목 확인 -> 보완 요청"
                                      }
                                    ]
                                  }
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        result.getAsyncResult(5000);
        String content = result.getResponse().getContentAsString();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(content).contains("event:plan");
        assertThat(content).contains("event:citation");
        assertThat(content).contains("event:answer_delta");
        assertThat(content).contains("event:final");
        assertThat(content).contains("MINIMUM_WAGE");
        assertThat(content).contains("done");
        assertThat(receivedToken.get()).isEqualTo(INTERNAL_TOKEN);
        assertThat(receivedBody.get()).contains("포괄임금제");
        assertThat(receivedBody.get()).contains("\"history\"");
        assertThat(receivedBody.get()).contains("연장근로수당 확인");
        assertThat(receivedBody.get()).contains("\"caseContext\"");
        assertThat(receivedBody.get()).contains("document-1");
        assertThat(receivedBody.get()).contains("OVERTIME_PAY");
        assertThat(receivedBody.get()).contains("근로시간과 휴일 항목 확인");
        assertThat(receivedBody.get()).contains("근로계약서 보완 요청");
    }

    @Test
    void rejectsRawOcrHintsInChatRequest() throws Exception {
        String accessToken = jwtProvider.generateAccessToken(activeUser());

        MvcResult result = mockMvc.perform(post("/api/ai/chat/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .content("""
                                {
                                  "message": "rawOcrText를 그대로 분석해줘",
                                  "languageCode": "ko",
                                  "topK": 3
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        result.getAsyncResult(5000);
        String content = result.getResponse().getContentAsString();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(content).contains("event:error");
        assertThat(content).contains("Raw OCR or file payload is not allowed");
    }

    @Test
    void rejectsRawOcrHintsInChatHistory() throws Exception {
        String accessToken = jwtProvider.generateAccessToken(activeUser());

        MvcResult result = mockMvc.perform(post("/api/ai/chat/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .content("""
                                {
                                  "message": "그럼 어떻게 해?",
                                  "languageCode": "ko",
                                  "topK": 3,
                                  "history": [
                                    {
                                      "role": "user",
                                      "content": "rawOcrText를 그대로 분석해줘"
                                    }
                                  ]
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        result.getAsyncResult(5000);
        String content = result.getResponse().getContentAsString();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(content).contains("event:error");
        assertThat(content).contains("Raw OCR or file payload is not allowed");
    }

    @Test
    void rejectsRawOcrHintsInCaseContext() throws Exception {
        String accessToken = jwtProvider.generateAccessToken(activeUser());

        MvcResult result = mockMvc.perform(post("/api/ai/chat/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .content("""
                                {
                                  "message": "이 계약서 기준으로 알려줘",
                                  "languageCode": "ko",
                                  "topK": 3,
                                  "caseContext": {
                                    "documentStatus": "rawOcrText"
                                  }
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        result.getAsyncResult(5000);
        String content = result.getResponse().getContentAsString();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(content).contains("event:error");
        assertThat(content).contains("Raw OCR or file payload is not allowed");
    }

    private User activeUser() {
        Enterprise company = enterpriseRepository.save(new Enterprise(
                "Harmony Chat Co",
                "555-55-55555",
                "Manufacturing",
                "KR",
                "ko",
                EnterpriseStatus.ACTIVE
        ));
        return userRepository.save(new User(
                "chat-user@example.com",
                "encoded",
                "Chat User",
                Role.WORKER,
                UserType.WORKER,
                UserStatus.ACTIVE,
                "KR",
                "ko",
                company
        ));
    }

    private static HttpServer startAiServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/chat/stream", AiChatControllerIntegrationTest::handleChatStream);
            server.start();
            return server;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start test AI server", ex);
        }
    }

    private static void handleChatStream(HttpExchange exchange) throws IOException {
        receivedToken.set(exchange.getRequestHeaders().getFirst("X-AI-Internal-Token"));
        receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] response = """
                event: plan
                data: {"issueCandidates":["MINIMUM_WAGE"]}

                event: citation
                data: {"sourceType":"ARTICLE","statuteName":"근로기준법"}

                event: answer_delta
                data: {"text":"야근수당 확인이 필요합니다."}

                event: final
                data: {"done":true}

                """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", MediaType.TEXT_EVENT_STREAM_VALUE);
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
