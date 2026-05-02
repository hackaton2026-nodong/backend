package com.kworkerharmony.backend.cases;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kworkerharmony.backend.cases.domain.CaseRepository;
import com.kworkerharmony.backend.cases.domain.CaseStatus;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CaseControllerIntegrationTest {

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

    @BeforeEach
    void setUp() {
        when(redisTokenRepository.isBlacklisted(anyString())).thenReturn(false);
    }

    @Test
    void employerCreatesCaseWithOwnCompanyAndBecomesEmployerParty() throws Exception {
        User employer = saveUser("employer@company-a.com", Role.EMPLOYER, UserType.EMPLOYER, saveEnterprise("Company A", "111-11-11111"));

        mockMvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "industry": "Manufacturing",
                                  "region": "Seoul"
                                }
                                """)
                        .header("Authorization", bearerToken(employer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.companyId").value(employer.getEnterprise().getId()))
                .andExpect(jsonPath("$.data.employerId").value(employer.getId()))
                .andExpect(jsonPath("$.data.workerId").doesNotExist())
                .andExpect(jsonPath("$.data.status").value(CaseStatus.PENDING.name()))
                .andExpect(jsonPath("$.data.industry").value("Manufacturing"))
                .andExpect(jsonPath("$.data.region").value("Seoul"));
    }

    @Test
    void workerCreatesCaseAndBecomesWorkerParty() throws Exception {
        User worker = saveUser("worker@company-a.com", Role.WORKER, UserType.WORKER, saveEnterprise("Company A", "111-11-11111"));

        mockMvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "industry": "Logistics",
                                  "region": "Busan"
                                }
                                """)
                        .header("Authorization", bearerToken(worker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.companyId").value(worker.getEnterprise().getId()))
                .andExpect(jsonPath("$.data.employerId").doesNotExist())
                .andExpect(jsonPath("$.data.workerId").value(worker.getId()))
                .andExpect(jsonPath("$.data.status").value(CaseStatus.PENDING.name()));
    }

    @Test
    void getCaseIsForbiddenForUserFromAnotherCompany() throws Exception {
        Enterprise companyA = saveEnterprise("Company A", "111-11-11111");
        Enterprise companyB = saveEnterprise("Company B", "222-22-22222");
        User employer = saveUser("employer@company-a.com", Role.EMPLOYER, UserType.EMPLOYER, companyA);
        User worker = saveUser("worker@company-a.com", Role.WORKER, UserType.WORKER, companyA);
        User outsider = saveUser("outsider@company-b.com", Role.EMPLOYER, UserType.EMPLOYER, companyB);
        Case caseEntity = caseRepository.save(new Case(companyA, employer, worker, CaseStatus.ACTIVE, "Manufacturing", "Seoul"));

        mockMvc.perform(get("/api/cases/{caseId}", caseEntity.getId())
                        .header("Authorization", bearerToken(outsider)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTH_403"));
    }

    @Test
    void getActiveCasesIncludesPendingCasesForFrontendContractList() throws Exception {
        Enterprise company = saveEnterprise("Company A", "111-11-11111");
        User employer = saveUser("employer@company-a.com", Role.EMPLOYER, UserType.EMPLOYER, company);
        caseRepository.save(new Case(company, employer, null, CaseStatus.PENDING, "Manufacturing", "Seoul"));
        caseRepository.save(new Case(company, employer, null, CaseStatus.ACTIVE, "Logistics", "Busan"));
        caseRepository.save(new Case(company, employer, null, CaseStatus.CLOSED, "Food", "Incheon"));

        mockMvc.perform(get("/api/cases/active")
                        .header("Authorization", bearerToken(employer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.status == 'PENDING')]").exists())
                .andExpect(jsonPath("$.data[?(@.status == 'ACTIVE')]").exists());
    }

    @Test
    void connectMembersRejectsUserFromAnotherCompany() throws Exception {
        Enterprise companyA = saveEnterprise("Company A", "111-11-11111");
        Enterprise companyB = saveEnterprise("Company B", "222-22-22222");
        User admin = saveUser("admin@company-a.com", Role.ADMIN, UserType.EMPLOYER, companyA);
        User employer = saveUser("employer@company-a.com", Role.EMPLOYER, UserType.EMPLOYER, companyA);
        User externalWorker = saveUser("worker@company-b.com", Role.WORKER, UserType.WORKER, companyB);
        Case caseEntity = caseRepository.save(new Case(companyA, null, null, CaseStatus.PENDING, "Manufacturing", "Seoul"));

        mockMvc.perform(post("/api/cases/{caseId}/members", caseEntity.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConnectCaseMembersPayload(
                                employer.getId(),
                                externalWorker.getId()
                        )))
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTH_403"));
    }

    @Test
    void connectMembersAssignsRequestedCompanyMembersToCase() throws Exception {
        Enterprise company = saveEnterprise("Company A", "111-11-11111");
        User admin = saveUser("admin@company-a.com", Role.ADMIN, UserType.EMPLOYER, company);
        User employer = saveUser("employer@company-a.com", Role.EMPLOYER, UserType.EMPLOYER, company);
        User worker = saveUser("worker@company-a.com", Role.WORKER, UserType.WORKER, company);
        Case caseEntity = caseRepository.save(new Case(company, null, null, CaseStatus.PENDING, "Manufacturing", "Seoul"));

        mockMvc.perform(post("/api/cases/{caseId}/members", caseEntity.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConnectCaseMembersPayload(
                                employer.getId(),
                                worker.getId()
                        )))
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employerId").value(employer.getId()))
                .andExpect(jsonPath("$.data.workerId").value(worker.getId()))
                .andExpect(jsonPath("$.data.status").value(CaseStatus.ACTIVE.name()));
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtProvider.generateAccessToken(user);
    }

    private Enterprise saveEnterprise(String name, String businessNumber) {
        return enterpriseRepository.save(new Enterprise(
                name,
                businessNumber,
                "Manufacturing",
                "KR",
                "ko",
                EnterpriseStatus.ACTIVE
        ));
    }

    private User saveUser(String email, Role role, UserType userType, Enterprise enterprise) {
        return userRepository.save(new User(
                email,
                "encoded",
                email,
                role,
                userType,
                UserStatus.ACTIVE,
                "KR",
                "ko",
                enterprise
        ));
    }

    private record ConnectCaseMembersPayload(
            Long employerId,
            Long workerId
    ) {
    }
}
