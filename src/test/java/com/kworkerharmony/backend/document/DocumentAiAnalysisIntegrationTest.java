package com.kworkerharmony.backend.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kworkerharmony.backend.cases.domain.CaseRepository;
import com.kworkerharmony.backend.cases.domain.CaseStatus;
import com.kworkerharmony.backend.cases.entity.Case;
import com.kworkerharmony.backend.document.port.DocumentAiAnalysisPort;
import com.kworkerharmony.backend.document.port.DocumentOcrPort;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.document.storage-root=build/test-uploads")
@AutoConfigureMockMvc
@Transactional
class DocumentAiAnalysisIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaseRepository caseRepository;

    @MockitoBean
    private RedisTokenRepository redisTokenRepository;

    @MockitoBean
    private DocumentOcrPort documentOcrPort;

    @MockitoBean
    private DocumentAiAnalysisPort documentAiAnalysisPort;

    @BeforeEach
    void setUp() {
        when(redisTokenRepository.isBlacklisted(anyString())).thenReturn(false);
    }

    @Test
    void analyzeDocumentStoresAndReturnsFastApiResultFields() throws Exception {
        TestCase testCase = activeCase();
        String accessToken = jwtProvider.generateAccessToken(testCase.employer());

        String uploadResponse = mockMvc.perform(multipart("/api/cases/{caseId}/documents", testCase.caseEntity().getId())
                        .file(new MockMultipartFile("file", "contract.pdf", "application/pdf", "sample-pdf".getBytes()))
                        .param("documentType", DocumentType.EMPLOYMENT_CONTRACT.name())
                        .param("issuedAt", "2026-06-01")
                        .param("expiresAt", "2027-05-31")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String documentId = objectMapper.readTree(uploadResponse).path("data").path("id").asText();

        mockMvc.perform(post("/api/documents/{documentId}/extraction/paddle-ocr", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ocrResult": {
                                    "layoutParsingResults": [
                                      {
                                        "markdown": {
                                          "text": "표준근로계약서 26년 06월 01일 ~ 27년 05월 31일 07시 00분 ~ 20시 00분 5. 휴게시간 1일 30분 월 통상임금 ( 1,900,000 )원 기숙사비 매월 350,000원"
                                        }
                                      }
                                    ]
                                  }
                                }
                                """)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(DocumentExtractionStatus.EXTRACTED.name()));

        when(documentAiAnalysisPort.analyze(argThat(command ->
                command.documentId().equals(documentId)
                        && command.caseId().equals(testCase.caseEntity().getId())
                        && command.payload().has("contractTerms")
                        && !command.payload().has("layoutParsingResults")
                        && !command.payload().has("markdown")
        ))).thenReturn(aiResult(documentId));

        mockMvc.perform(post("/api/documents/{documentId}/analysis", documentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(DocumentAnalysisStatus.COMPLETED.name()))
                .andExpect(jsonPath("$.data.summary").value("최저임금과 숙식비 공제 확인이 필요합니다."))
                .andExpect(jsonPath("$.data.issueCandidates[0]").value("MINIMUM_WAGE"))
                .andExpect(jsonPath("$.data.riskFlags[0].code").value("MINIMUM_WAGE_REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.data.generatedAnalysis.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.findings[0].issueCode").value("MINIMUM_WAGE"))
                .andExpect(jsonPath("$.data.citations[0].excerpt").value("월급을 근로시간으로 환산해 최저임금 이상인지 확인해야 합니다."))
                .andExpect(jsonPath("$.data.recommendedActions[0].institutionId").value("institution:local-labor-office"))
                .andExpect(jsonPath("$.data.relatedInstitutions[0].institutionId").value("institution:local-labor-office"));

        String getResponse = mockMvc.perform(get("/api/documents/{documentId}/analysis", documentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(DocumentAnalysisStatus.COMPLETED.name()))
                .andExpect(jsonPath("$.data.detailJson.status").value("COMPLETED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(getResponse).doesNotContain("layoutParsingResults", "rawOcrText", "markdown");
    }

    @Test
    void analyzeDocumentReturnsFailureResponseAndPersistsFailedAnalysisWhenAiFails() throws Exception {
        TestCase testCase = activeCase();
        String accessToken = jwtProvider.generateAccessToken(testCase.employer());

        String uploadResponse = mockMvc.perform(multipart("/api/cases/{caseId}/documents", testCase.caseEntity().getId())
                        .file(new MockMultipartFile("file", "contract.pdf", "application/pdf", "sample-pdf".getBytes()))
                        .param("documentType", DocumentType.EMPLOYMENT_CONTRACT.name())
                        .param("issuedAt", "2026-06-01")
                        .param("expiresAt", "2027-05-31")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String documentId = objectMapper.readTree(uploadResponse).path("data").path("id").asText();

        mockMvc.perform(post("/api/documents/{documentId}/extraction/paddle-ocr", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ocrResult": {
                                    "layoutParsingResults": [
                                      {
                                        "markdown": {
                                          "text": "표준근로계약서 26년 06월 01일 ~ 27년 05월 31일 09시 00분 ~ 18시 00분 5. 휴게시간 1일 60분 월 통상임금 ( 2,500,000 )원"
                                        }
                                      }
                                    ]
                                  }
                                }
                                """)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(DocumentExtractionStatus.EXTRACTED.name()));

        when(documentAiAnalysisPort.analyze(any()))
                .thenThrow(new IllegalStateException("AI analysis is disabled or DOCUMENT_AI_ENDPOINT is not configured"));

        mockMvc.perform(post("/api/documents/{documentId}/analysis", documentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AI_503"))
                .andExpect(jsonPath("$.error.message").value("AI analysis is disabled or DOCUMENT_AI_ENDPOINT is not configured"));

        mockMvc.perform(get("/api/documents/{documentId}/analysis", documentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value(DocumentAnalysisStatus.FAILED.name()))
                .andExpect(jsonPath("$.data.failedReason").value("AI analysis is disabled or DOCUMENT_AI_ENDPOINT is not configured"));
    }

    private DocumentAiAnalysisPort.AiAnalysisResult aiResult(String documentId) throws Exception {
        String riskFlags = objectMapper.writeValueAsString(List.of(Map.of(
                "code", "MINIMUM_WAGE_REVIEW_REQUIRED",
                "severity", "HIGH",
                "message", "월 임금이 최저임금 월 환산액보다 낮을 수 있습니다.",
                "evidenceIds", List.of("ev-wage")
        )));
        String issueCandidates = objectMapper.writeValueAsString(List.of("MINIMUM_WAGE", "DORMITORY_DEDUCTION"));
        String generatedAnalysis = objectMapper.writeValueAsString(Map.of(
                "status", "COMPLETED",
                "model", "test-model",
                "text", "확인된 위험\\n- 최저임금 확인 필요\\n\\n근거\\n- 근로기준법 및 최저임금법 근거\\n\\n다음 조치\\n- 관할 지방고용노동관서 상담"
        ));
        String findings = objectMapper.writeValueAsString(List.of(Map.of(
                "issueCode", "MINIMUM_WAGE",
                "issueName", "최저임금",
                "severity", "HIGH",
                "message", "월 임금이 낮을 수 있습니다.",
                "criteria", List.of(),
                "evidenceRefs", List.of(Map.of("evidenceId", "ev-wage", "fieldName", "wage.amount"))
        )));
        String citations = objectMapper.writeValueAsString(List.of(Map.of(
                "sourceType", "ARTICLE",
                "sourceId", "article:minimum-wage",
                "title", "최저임금법 제6조",
                "excerpt", "월급을 근로시간으로 환산해 최저임금 이상인지 확인해야 합니다."
        )));
        String actions = objectMapper.writeValueAsString(List.of(Map.of(
                "remedyId", "remedy:minimum-wage-difference-claim",
                "remedyName", "최저임금 차액 지급 요구",
                "institutionId", "institution:local-labor-office",
                "institutionName", "지방고용노동관서"
        )));
        String institutions = objectMapper.writeValueAsString(List.of(Map.of(
                "institutionId", "institution:local-labor-office",
                "institutionName", "지방고용노동관서"
        )));
        String detailJson = objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("requestId", "analysis-request"),
                Map.entry("document", Map.of("documentId", documentId)),
                Map.entry("status", "COMPLETED"),
                Map.entry("summary", "최저임금과 숙식비 공제 확인이 필요합니다."),
                Map.entry("riskFlags", objectMapper.readTree(riskFlags)),
                Map.entry("issueCandidates", objectMapper.readTree(issueCandidates)),
                Map.entry("findings", objectMapper.readTree(findings)),
                Map.entry("citations", objectMapper.readTree(citations)),
                Map.entry("recommendedActions", objectMapper.readTree(actions)),
                Map.entry("relatedInstitutions", objectMapper.readTree(institutions)),
                Map.entry("generatedAnalysis", objectMapper.readTree(generatedAnalysis))
        ));
        return new DocumentAiAnalysisPort.AiAnalysisResult(
                "input-hash",
                "analysis-hash",
                "최저임금과 숙식비 공제 확인이 필요합니다.",
                riskFlags,
                issueCandidates,
                generatedAnalysis,
                findings,
                "[]",
                citations,
                actions,
                institutions,
                "관련 법령과 기관 조치를 함께 검토했습니다.",
                detailJson,
                null
        );
    }

    private TestCase activeCase() {
        Enterprise company = enterpriseRepository.save(new Enterprise(
                "Harmony AI Co",
                "123-45-67890",
                "Manufacturing",
                "KR",
                "ko",
                EnterpriseStatus.ACTIVE
        ));
        User employer = userRepository.save(new User(
                "ai-employer@example.com",
                "encoded",
                "Employer",
                Role.EMPLOYER,
                UserType.EMPLOYER,
                UserStatus.ACTIVE,
                "KR",
                "ko",
                company
        ));
        User worker = userRepository.save(new User(
                "ai-worker@example.com",
                "encoded",
                "Worker",
                Role.WORKER,
                UserType.WORKER,
                UserStatus.ACTIVE,
                "KR",
                "ko",
                company
        ));
        Case caseEntity = caseRepository.save(new Case(
                company,
                employer,
                worker,
                CaseStatus.ACTIVE,
                "Manufacturing",
                "Seoul"
        ));
        return new TestCase(employer, caseEntity);
    }

    private record TestCase(
            User employer,
            Case caseEntity
    ) {
    }
}
