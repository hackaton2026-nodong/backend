package com.kworkerharmony.backend.document;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kworkerharmony.backend.cases.domain.CaseStatus;
import com.kworkerharmony.backend.cases.domain.CaseRepository;
import com.kworkerharmony.backend.cases.entity.Case;
import com.kworkerharmony.backend.enterprise.Enterprise;
import com.kworkerharmony.backend.enterprise.EnterpriseRepository;
import com.kworkerharmony.backend.enterprise.EnterpriseStatus;
import com.kworkerharmony.backend.global.security.JwtProvider;
import com.kworkerharmony.backend.global.security.RedisTokenRepository;
import com.kworkerharmony.backend.document.port.DocumentAiAnalysisPort;
import com.kworkerharmony.backend.document.port.DocumentAiAnalysisPort.AiAnalysisResult;
import com.kworkerharmony.backend.document.port.DocumentOcrPort;
import com.kworkerharmony.backend.user.Role;
import com.kworkerharmony.backend.user.User;
import com.kworkerharmony.backend.user.UserRepository;
import com.kworkerharmony.backend.user.UserStatus;
import com.kworkerharmony.backend.user.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.document.storage-root=build/test-uploads")
@AutoConfigureMockMvc
@Transactional
class DocumentControllerIntegrationTest {

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
    private DocumentAiAnalysisPort documentAiAnalysisPort;

    @MockitoBean
    private DocumentOcrPort documentOcrPort;

    @BeforeEach
    void setUp() {
        when(redisTokenRepository.isBlacklisted(anyString())).thenReturn(false);
    }

    @Test
    void uploadDocumentStoresFileAndReturnsHashedStatus() throws Exception {
        Enterprise company = enterpriseRepository.save(new Enterprise(
                "Harmony Co",
                "123-45-67890",
                "Manufacturing",
                "KR",
                "ko",
                EnterpriseStatus.ACTIVE
        ));
        User employer = userRepository.save(new User(
                "employer@example.com",
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
                "worker@example.com",
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

        String accessToken = jwtProvider.generateAccessToken(employer);

        mockMvc.perform(multipart("/api/cases/{caseId}/documents", caseEntity.getId())
                        .file(new MockMultipartFile("file", "contract.pdf", "application/pdf", "sample-pdf".getBytes()))
                        .param("documentType", DocumentType.EMPLOYMENT_CONTRACT.name())
                        .param("issuedAt", "2026-01-01")
                        .param("expiresAt", "2027-01-01")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.caseId").value(caseEntity.getId()))
                .andExpect(jsonPath("$.data.status").value(DocumentStatus.HASHED.name()))
                .andExpect(jsonPath("$.data.stored").value(true))
                .andExpect(jsonPath("$.data.sha256Hash").isNotEmpty())
                .andExpect(jsonPath("$.data.storageKey").isNotEmpty());
    }

    @Test
    void uploadTestPageIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/document-upload-test.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("step-upload")));
    }

    @Test
    void extractionTestPageIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/extraction-test.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/document-upload-test.html")));
    }

    @Test
    void aiHealthIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/ai/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mode").value("STUB"))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void aiHealthAllowsFrontendCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/ai/health")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:5174")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5174"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void companyAdminCanListDocumentsForSharedCase() throws Exception {
        Enterprise company = enterpriseRepository.save(new Enterprise(
                "Harmony Co",
                "123-45-67890",
                "Manufacturing",
                "KR",
                "ko",
                EnterpriseStatus.ACTIVE
        ));
        User admin = userRepository.save(new User(
                "admin@example.com",
                "encoded",
                "Admin",
                Role.ADMIN,
                UserType.EMPLOYER,
                UserStatus.ACTIVE,
                "KR",
                "ko",
                company
        ));
        User employer = userRepository.save(new User(
                "employer@example.com",
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
                "worker@example.com",
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

        String ownerToken = jwtProvider.generateAccessToken(employer);
        String adminToken = jwtProvider.generateAccessToken(admin);

        mockMvc.perform(multipart("/api/cases/{caseId}/documents", caseEntity.getId())
                        .file(new MockMultipartFile("file", "contract.pdf", "application/pdf", "sample-pdf".getBytes()))
                        .param("documentType", DocumentType.EMPLOYMENT_CONTRACT.name())
                        .param("issuedAt", "2026-01-01")
                        .param("expiresAt", "2027-01-01")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cases/{caseId}/documents", caseEntity.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].caseId").value(caseEntity.getId()))
                .andExpect(jsonPath("$.data[0].status").value(DocumentStatus.HASHED.name()));
    }

    @Test
    void workerCanGetDocumentDetailForOwnCaseAndUploaderMatchesAuthenticatedUser() throws Exception {
        Enterprise company = enterpriseRepository.save(new Enterprise(
                "Harmony Co",
                "123-45-67890",
                "Manufacturing",
                "KR",
                "ko",
                EnterpriseStatus.ACTIVE
        ));
        User employer = userRepository.save(new User(
                "employer@example.com",
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
                "worker@example.com",
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

        String responseBody = mockMvc.perform(multipart("/api/cases/{caseId}/documents", caseEntity.getId())
                        .file(new MockMultipartFile("file", "contract.pdf", "application/pdf", "sample-pdf".getBytes()))
                        .param("documentType", DocumentType.EMPLOYMENT_CONTRACT.name())
                        .param("issuedAt", "2026-01-01")
                        .param("expiresAt", "2027-01-01")
                        .header("Authorization", "Bearer " + jwtProvider.generateAccessToken(worker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.uploaderUserId").value(worker.getId()))
                .andExpect(jsonPath("$.data.status").value(DocumentStatus.HASHED.name()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String documentId = objectMapper.readTree(responseBody).path("data").path("id").asText();

        mockMvc.perform(get("/api/documents/{documentId}", documentId)
                        .header("Authorization", "Bearer " + jwtProvider.generateAccessToken(worker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.caseId").value(caseEntity.getId()))
                .andExpect(jsonPath("$.data.uploaderUserId").value(worker.getId()));
    }

    @Test
    void employerCanGetDocumentListForOwnCase() throws Exception {
        Enterprise company = enterpriseRepository.save(new Enterprise(
                "Harmony Co",
                "123-45-67890",
                "Manufacturing",
                "KR",
                "ko",
                EnterpriseStatus.ACTIVE
        ));
        User employer = userRepository.save(new User(
                "employer@example.com",
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
                "worker@example.com",
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

        mockMvc.perform(multipart("/api/cases/{caseId}/documents", caseEntity.getId())
                        .file(new MockMultipartFile("file", "contract.pdf", "application/pdf", "sample-pdf".getBytes()))
                        .param("documentType", DocumentType.EMPLOYMENT_CONTRACT.name())
                        .param("issuedAt", "2026-01-01")
                        .param("expiresAt", "2027-01-01")
                        .header("Authorization", "Bearer " + jwtProvider.generateAccessToken(worker)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cases/{caseId}/documents", caseEntity.getId())
                        .header("Authorization", "Bearer " + jwtProvider.generateAccessToken(employer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].caseId").value(caseEntity.getId()));
    }

    @Test
    void documentDetailIsForbiddenForAnotherCompanyUser() throws Exception {
        Enterprise companyA = enterpriseRepository.save(new Enterprise(
                "Company A",
                "111-11-11111",
                "Manufacturing",
                "KR",
                "ko",
                EnterpriseStatus.ACTIVE
        ));
        Enterprise companyB = enterpriseRepository.save(new Enterprise(
                "Company B",
                "222-22-22222",
                "Logistics",
                "KR",
                "ko",
                EnterpriseStatus.ACTIVE
        ));
        User employer = userRepository.save(new User(
                "a@example.com",
                "encoded",
                "Employer A",
                Role.EMPLOYER,
                UserType.EMPLOYER,
                UserStatus.ACTIVE,
                "KR",
                "ko",
                companyA
        ));
        User worker = userRepository.save(new User(
                "aw@example.com",
                "encoded",
                "Worker A",
                Role.WORKER,
                UserType.WORKER,
                UserStatus.ACTIVE,
                "KR",
                "ko",
                companyA
        ));
        User outsider = userRepository.save(new User(
                "b@example.com",
                "encoded",
                "Employer B",
                Role.EMPLOYER,
                UserType.EMPLOYER,
                UserStatus.ACTIVE,
                "KR",
                "ko",
                companyB
        ));
        Case caseEntity = caseRepository.save(new Case(
                companyA,
                employer,
                worker,
                CaseStatus.ACTIVE,
                "Manufacturing",
                "Seoul"
        ));
        String ownerToken = jwtProvider.generateAccessToken(employer);
        String outsiderToken = jwtProvider.generateAccessToken(outsider);

        String responseBody = mockMvc.perform(multipart("/api/cases/{caseId}/documents", caseEntity.getId())
                        .file(new MockMultipartFile("file", "contract.pdf", "application/pdf", "sample-pdf".getBytes()))
                        .param("documentType", DocumentType.EMPLOYMENT_CONTRACT.name())
                        .param("issuedAt", "2026-01-01")
                        .param("expiresAt", "2027-01-01")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String documentId = objectMapper.readTree(responseBody).path("data").path("id").asText();

        mockMvc.perform(get("/api/documents/{documentId}", documentId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void documentListIsForbiddenForAnotherCompanyUser() throws Exception {
        Enterprise companyA = enterpriseRepository.save(new Enterprise(
                "Company A",
                "111-11-11111",
                "Manufacturing",
                "KR",
                "ko",
                EnterpriseStatus.ACTIVE
        ));
        Enterprise companyB = enterpriseRepository.save(new Enterprise(
                "Company B",
                "222-22-22222",
                "Logistics",
                "KR",
                "ko",
                EnterpriseStatus.ACTIVE
        ));
        User employer = userRepository.save(new User(
                "a@example.com",
                "encoded",
                "Employer A",
                Role.EMPLOYER,
                UserType.EMPLOYER,
                UserStatus.ACTIVE,
                "KR",
                "ko",
                companyA
        ));
        User worker = userRepository.save(new User(
                "aw@example.com",
                "encoded",
                "Worker A",
                Role.WORKER,
                UserType.WORKER,
                UserStatus.ACTIVE,
                "KR",
                "ko",
                companyA
        ));
        User outsider = userRepository.save(new User(
                "b@example.com",
                "encoded",
                "Employer B",
                Role.EMPLOYER,
                UserType.EMPLOYER,
                UserStatus.ACTIVE,
                "KR",
                "ko",
                companyB
        ));
        Case caseEntity = caseRepository.save(new Case(
                companyA,
                employer,
                worker,
                CaseStatus.ACTIVE,
                "Manufacturing",
                "Seoul"
        ));

        mockMvc.perform(get("/api/cases/{caseId}/documents", caseEntity.getId())
                        .header("Authorization", "Bearer " + jwtProvider.generateAccessToken(outsider)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTH_403"));
    }

    @Test
    void createGetAndCorrectExtractionStoresOnlySanitizedPayload() throws Exception {
        Enterprise company = enterpriseRepository.save(new Enterprise(
                "Harmony Co",
                "123-45-67890",
                "Manufacturing",
                "KR",
                "ko",
                EnterpriseStatus.ACTIVE
        ));
        User employer = userRepository.save(new User(
                "employer@example.com",
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
                "worker@example.com",
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
        String accessToken = jwtProvider.generateAccessToken(employer);

        String uploadResponse = mockMvc.perform(multipart("/api/cases/{caseId}/documents", caseEntity.getId())
                        .file(new MockMultipartFile("file", "contract.pdf", "application/pdf", "sample-pdf".getBytes()))
                        .param("documentType", DocumentType.EMPLOYMENT_CONTRACT.name())
                        .param("issuedAt", "2026-01-01")
                        .param("expiresAt", "2027-01-01")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String documentId = objectMapper.readTree(uploadResponse).path("data").path("id").asText();

        String ocrRequest = """
                {
                  "ocrResult": {
                    "layoutParsingResults": [
                      {
                        "markdown": {
                          "text": "## 표준근로계약서 Standard Labor Contract\\n한국제조 031-555-1290 김민수 Identification number 214-86-73951 MARIA LUZ SANTOS 1998-07-21 26년 06월 01일 ~ 27년 05월 31일 - 수습기간: [√] 활용(입국일부터 [√] 1개월) - 업종: 제조업 - 사업내용: 자동차 금속부품 생산 - 직무내용: 금속부품 조립, 품질검사, 포장작업 08시 30분 ~ 17시 30분 -1일 평균 시간외 근로시간: 1시간 (사업장 사정에 따라 변동 가능: 2시간 이내) 5. 휴게시간 1일 60분 6. 휴일 [√]일요일 [√]공휴일([√]유급 [ ]무급) [√]매주 토요일 7. 임금 1) 월 통상임금 ( 2,300,000 )원- 기본급[ 월급 ] ( 2,150,000 )원- 고정적 수당: ( 생산 수당: 100,000 )원), ( 식대 수당: 50,000 )원)- 상여금 ( 0 )원) 8) 임금지급일 매월 ( 10 )일 9) 지급방법 [ ]직접 지급, [ √ ]통장 임금 1) 숙박시설 제공- 숙박시설 제공 여부: [ √ ]제공 [ ]미제공 기타주택형태 시설( 기숙사 ))10) 숙박시설 제공 시 근로자 부담금액: 매월 150,000 원2) 식사 제공- 식사 제공 여부: 제공([ ]조식, [ √ ]중식, [ ]석식), [ ]미제공- 식사 제공시 근로자 부담금액:매월 0 )원 2026.06.01. 사용자:김민수 근로자 : MARIA LUZ SANTOS"
                        }
                      }
                    ]
                  }
                }
                """;

        mockMvc.perform(post("/api/documents/{documentId}/extraction/paddle-ocr", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ocrRequest)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(DocumentExtractionStatus.EXTRACTED.name()))
                .andExpect(jsonPath("$.data.sourceEngine").value("PADDLE_OCR"))
                .andExpect(jsonPath("$.data.sourceResultHash").isNotEmpty())
                .andExpect(jsonPath("$.data.extractedPayload.contractTerms.wage.amount").value(2300000))
                .andExpect(jsonPath("$.data.extractedPayload.contractTerms.workingHours.startTime").value("08:30"))
                .andExpect(jsonPath("$.data.extractedPayload.contractTerms.dormitory.deductionAmount").value(150000))
                .andExpect(jsonPath("$.data.extractedPayload.layoutParsingResults").doesNotExist())
                .andExpect(jsonPath("$.data.extractedPayload.markdown").doesNotExist());

        mockMvc.perform(get("/api/documents/{documentId}/extraction", documentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.extractedPayload.contractTerms.wage.paymentDay").value(10));

        String correctionRequest = """
                {
                  "correctedPayload": {
                    "schemaVersion": "employment-contract-v1",
                    "contractTerms": {
                      "wage": {
                        "status": "FOUND",
                        "amount": 2400000,
                        "currency": "KRW",
                        "period": "MONTHLY"
                      }
                    }
                  }
                }
                """;

        mockMvc.perform(put("/api/documents/{documentId}/extraction/correction", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctionRequest)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(DocumentExtractionStatus.CORRECTED.name()))
                .andExpect(jsonPath("$.data.correctedPayload.contractTerms.wage.amount").value(2400000));
    }

    @Test
    void analysisApiStoresAndReturnsFrontendShape() throws Exception {
        Enterprise company = enterpriseRepository.save(new Enterprise(
                "Harmony Co",
                "123-45-67890",
                "Manufacturing",
                "KR",
                "ko",
                EnterpriseStatus.ACTIVE
        ));
        User employer = userRepository.save(new User(
                "analysis-employer@example.com",
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
                "analysis-worker@example.com",
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
        String accessToken = jwtProvider.generateAccessToken(employer);
        String uploadResponse = mockMvc.perform(multipart("/api/cases/{caseId}/documents", caseEntity.getId())
                        .file(new MockMultipartFile("file", "contract.pdf", "application/pdf", "sample-pdf".getBytes()))
                        .param("documentType", DocumentType.EMPLOYMENT_CONTRACT.name())
                        .param("issuedAt", "2026-01-01")
                        .param("expiresAt", "2027-01-01")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String documentId = objectMapper.readTree(uploadResponse).path("data").path("id").asText();

        String correctionRequest = """
                {
                  "correctedPayload": {
                    "schemaVersion": "employment-contract-v1",
                    "contractTerms": {
                      "wage": {
                        "status": "FOUND",
                        "amount": 1850000,
                        "currency": "KRW",
                        "period": "MONTHLY"
                      }
                    },
                    "evidenceRefs": [],
                    "candidateChecklistItemCodes": ["LRA_MINIMUM_WAGE"]
                  }
                }
                """;

        mockMvc.perform(put("/api/documents/{documentId}/extraction/correction", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctionRequest)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(DocumentExtractionStatus.CORRECTED.name()));

        when(documentAiAnalysisPort.analyze(any())).thenReturn(new AiAnalysisResult(
                "input-hash",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "최저임금과 휴게시간 확인이 필요합니다.",
                "[{\"code\":\"MINIMUM_WAGE_REVIEW_REQUIRED\",\"severity\":\"HIGH\"}]",
                "[\"MINIMUM_WAGE\",\"BREAK_TIME\"]",
                "{\"status\":\"COMPLETED\",\"text\":\"사용자 표시용 생성 분석입니다.\"}",
                "[{\"issueCode\":\"MINIMUM_WAGE\",\"severity\":\"HIGH\"}]",
                "[{\"fieldName\":\"wage.amount\",\"status\":\"FOUND\"}]",
                "[{\"sourceType\":\"ARTICLE\",\"statuteName\":\"근로기준법\",\"articleNo\":\"43\"}]",
                "[{\"nameKo\":\"임금체불 진정\",\"institutionName\":\"고용노동부\"}]",
                "[{\"institutionName\":\"고용노동부\"}]",
                "관련 판례 1건을 함께 검토했습니다.",
                "{\"status\":\"COMPLETED\",\"summary\":\"최저임금과 휴게시간 확인이 필요합니다.\"}",
                null
        ));

        mockMvc.perform(post("/api/documents/{documentId}/analysis", documentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.analysisId").isNotEmpty())
                .andExpect(jsonPath("$.data.caseId").value(caseEntity.getId()))
                .andExpect(jsonPath("$.data.status").value(DocumentAnalysisStatus.COMPLETED.name()))
                .andExpect(jsonPath("$.data.summary").value("최저임금과 휴게시간 확인이 필요합니다."))
                .andExpect(jsonPath("$.data.riskFlags[0].code").value("MINIMUM_WAGE_REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.data.issueCandidates[0]").value("MINIMUM_WAGE"))
                .andExpect(jsonPath("$.data.generatedAnalysis.text").value("사용자 표시용 생성 분석입니다."))
                .andExpect(jsonPath("$.data.findings[0].issueCode").value("MINIMUM_WAGE"))
                .andExpect(jsonPath("$.data.fieldFindings[0].fieldName").value("wage.amount"))
                .andExpect(jsonPath("$.data.citations[0].sourceType").value("ARTICLE"))
                .andExpect(jsonPath("$.data.recommendedActions[0].nameKo").value("임금체불 진정"))
                .andExpect(jsonPath("$.data.relatedInstitutions[0].institutionName").value("고용노동부"))
                .andExpect(jsonPath("$.data.caseStatus").value("관련 판례 1건을 함께 검토했습니다."))
                .andExpect(jsonPath("$.data.detailJson.status").value("COMPLETED"));

        mockMvc.perform(get("/api/documents/{documentId}/analysis", documentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedAnalysis.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.citations[0].statuteName").value("근로기준법"));
    }

    @Test
    void uploadCreatesPendingExtractionAndCallbackStoresExtractedPayload() throws Exception {
        Enterprise company = enterpriseRepository.save(new Enterprise(
                "Harmony Co",
                "123-45-67890",
                "Manufacturing",
                "KR",
                "ko",
                EnterpriseStatus.ACTIVE
        ));
        User employer = userRepository.save(new User(
                "employer-callback@example.com",
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
                "worker-callback@example.com",
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
        String accessToken = jwtProvider.generateAccessToken(employer);

        String uploadResponse = mockMvc.perform(multipart("/api/cases/{caseId}/documents", caseEntity.getId())
                        .file(new MockMultipartFile("file", "contract.pdf", "application/pdf", "sample-pdf".getBytes()))
                        .param("documentType", DocumentType.EMPLOYMENT_CONTRACT.name())
                        .param("issuedAt", "2026-01-01")
                        .param("expiresAt", "2027-01-01")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String documentId = objectMapper.readTree(uploadResponse).path("data").path("id").asText();

        verify(documentOcrPort).requestOcr(argThat(command ->
                command.documentId().equals(documentId)
                        && command.caseId().equals(caseEntity.getId())
                        && command.documentType().equals(DocumentType.EMPLOYMENT_CONTRACT.name())
                        && command.callbackUrl().contains("/api/internal/documents/" + documentId + "/ocr-result")
        ));

        mockMvc.perform(get("/api/documents/{documentId}/extraction", documentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(DocumentExtractionStatus.PENDING.name()));

        String callbackRequest = """
                {
                  "ocrResult": {
                    "layoutParsingResults": [
                      {
                        "markdown": {
                          "text": "표준근로계약서 26년 06월 01일 ~ 27년 05월 31일 08시 30분 ~ 17시 30분 5. 휴게시간 1일 60분 월 통상임금 ( 2,300,000 )원"
                        }
                      }
                    ]
                  }
                }
                """;

        mockMvc.perform(post("/api/internal/documents/{documentId}/ocr-result", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(DocumentExtractionStatus.EXTRACTED.name()))
                .andExpect(jsonPath("$.data.extractedPayload.contractTerms.wage.amount").value(2300000));
    }
}
