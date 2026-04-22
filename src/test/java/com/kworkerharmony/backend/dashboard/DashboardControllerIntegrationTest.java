package com.kworkerharmony.backend.dashboard;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kworkerharmony.backend.alert.domain.AlertRepository;
import com.kworkerharmony.backend.alert.domain.AlertType;
import com.kworkerharmony.backend.alert.entity.Alert;
import com.kworkerharmony.backend.cases.domain.CaseStatus;
import com.kworkerharmony.backend.cases.domain.CaseRepository;
import com.kworkerharmony.backend.cases.entity.Case;
import com.kworkerharmony.backend.checklist.domain.CaseChecklistStatusRepository;
import com.kworkerharmony.backend.checklist.domain.ChecklistStatus;
import com.kworkerharmony.backend.checklist.entity.CaseChecklistStatus;
import com.kworkerharmony.backend.document.Document;
import com.kworkerharmony.backend.document.DocumentRepository;
import com.kworkerharmony.backend.document.DocumentStatus;
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
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private CaseChecklistStatusRepository caseChecklistStatusRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AlertRepository alertRepository;

    @MockitoBean
    private RedisTokenRepository redisTokenRepository;

    @BeforeEach
    void setUp() {
        when(redisTokenRepository.isBlacklisted(anyString())).thenReturn(false);
    }

    @Test
    void workerDashboardReturnsStateBoardBlocks() throws Exception {
        Enterprise company = saveEnterprise("Harmony Co", "123-45-67890");
        User employer = saveUser("employer@example.com", Role.EMPLOYER, UserType.EMPLOYER, company);
        User worker = saveUser("worker@example.com", Role.WORKER, UserType.WORKER, company);
        Case caseEntity = caseRepository.save(new Case(company, employer, worker, CaseStatus.ACTIVE, "Manufacturing", "Seoul"));

        caseChecklistStatusRepository.save(new CaseChecklistStatus(caseEntity, "FEA_STANDARD_EMPLOYMENT_CONTRACT", ChecklistStatus.COMPLETED, "done"));
        caseChecklistStatusRepository.save(new CaseChecklistStatus(caseEntity, "LRA_WRITTEN_CONDITIONS", ChecklistStatus.REVIEW_REQUIRED, "review"));

        documentRepository.save(new Document(
                caseEntity.getId(),
                worker.getId(),
                "EMPLOYMENT_CONTRACT",
                "contract.pdf",
                "docs/contract.pdf",
                "application/pdf",
                128L,
                "hash-1",
                null,
                DocumentStatus.HASHED,
                LocalDate.now().minusDays(30),
                LocalDate.now().plusDays(7),
                null,
                null
        ));
        documentRepository.save(new Document(
                caseEntity.getId(),
                worker.getId(),
                "PAYSLIP",
                "payslip.pdf",
                "docs/payslip.pdf",
                "application/pdf",
                64L,
                null,
                null,
                DocumentStatus.FAILED,
                LocalDate.now().minusDays(3),
                null,
                null,
                null
        ));

        alertRepository.save(new Alert(worker, "Checklist review pending", "Please review checklist", AlertType.CHECKLIST, false));

        mockMvc.perform(get("/api/dashboard/worker")
                        .header("Authorization", bearerToken(worker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.header.userId").value(worker.getId()))
                .andExpect(jsonPath("$.data.header.caseId").value(caseEntity.getId()))
                .andExpect(jsonPath("$.data.agentCenter.reasonTypes.length()").value(2))
                .andExpect(jsonPath("$.data.summaryCards.length()").value(4))
                .andExpect(jsonPath("$.data.summaryCards[0].key").value("risks"))
                .andExpect(jsonPath("$.data.todayActions.length()").value(3))
                .andExpect(jsonPath("$.data.recommendationSlot.title").value("추천 기관 · 교육"))
                .andExpect(jsonPath("$.data.noticePanel.severity").value("high"));
    }

    @Test
    void workerDashboardFallsBackToEmptyCaseStateWhenNoCaseExists() throws Exception {
        Enterprise company = saveEnterprise("Harmony Co", "123-45-67890");
        User worker = saveUser("worker@example.com", Role.WORKER, UserType.WORKER, company);

        mockMvc.perform(get("/api/dashboard/worker")
                        .header("Authorization", bearerToken(worker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.header.caseId").doesNotExist())
                .andExpect(jsonPath("$.data.agentCenter.title").value("아직 연결된 케이스가 없어요"))
                .andExpect(jsonPath("$.data.summaryCards.length()").value(4))
                .andExpect(jsonPath("$.data.todayActions.length()").value(1))
                .andExpect(jsonPath("$.data.noticePanel.severity").value("info"));
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
}
