package com.kworkerharmony.backend.document;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kworkerharmony.backend.country.Country;
import com.kworkerharmony.backend.country.CountryRepository;
import com.kworkerharmony.backend.cases.domain.CaseStatus;
import com.kworkerharmony.backend.cases.domain.CaseRepository;
import com.kworkerharmony.backend.cases.entity.Case;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
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
    private CountryRepository countryRepository;

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaseRepository caseRepository;

    @MockBean
    private RedisTokenRepository redisTokenRepository;

    @BeforeEach
    void setUp() {
        when(redisTokenRepository.isBlacklisted(anyString())).thenReturn(false);
        if (countryRepository.findByCountryCode("KR").isEmpty()) {
            countryRepository.save(new Country("KR", "Korea"));
        }
    }

    @Test
    void uploadDocumentStoresFileAndReturnsHashedStatus() throws Exception {
        Country country = countryRepository.findByCountryCode("KR").orElseThrow();
        Enterprise company = enterpriseRepository.save(new Enterprise(
                "Harmony Co",
                "123-45-67890",
                "Manufacturing",
                "KR",
                EnterpriseStatus.ACTIVE
        ));
        User employer = userRepository.save(new User(
                "employer@example.com",
                "encoded",
                "Employer",
                Role.EMPLOYER,
                UserType.EMPLOYER,
                UserStatus.ACTIVE,
                country,
                company
        ));
        User worker = userRepository.save(new User(
                "worker@example.com",
                "encoded",
                "Worker",
                Role.WORKER,
                UserType.WORKER,
                UserStatus.ACTIVE,
                country,
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Document Upload Test")));
    }

    @Test
    void documentDetailIsForbiddenForAnotherCompanyUser() throws Exception {
        Country country = countryRepository.findByCountryCode("KR").orElseThrow();
        Enterprise companyA = enterpriseRepository.save(new Enterprise(
                "Company A",
                "111-11-11111",
                "Manufacturing",
                "KR",
                EnterpriseStatus.ACTIVE
        ));
        Enterprise companyB = enterpriseRepository.save(new Enterprise(
                "Company B",
                "222-22-22222",
                "Logistics",
                "KR",
                EnterpriseStatus.ACTIVE
        ));
        User employer = userRepository.save(new User(
                "a@example.com",
                "encoded",
                "Employer A",
                Role.EMPLOYER,
                UserType.EMPLOYER,
                UserStatus.ACTIVE,
                country,
                companyA
        ));
        User worker = userRepository.save(new User(
                "aw@example.com",
                "encoded",
                "Worker A",
                Role.WORKER,
                UserType.WORKER,
                UserStatus.ACTIVE,
                country,
                companyA
        ));
        User outsider = userRepository.save(new User(
                "b@example.com",
                "encoded",
                "Employer B",
                Role.EMPLOYER,
                UserType.EMPLOYER,
                UserStatus.ACTIVE,
                country,
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
}
