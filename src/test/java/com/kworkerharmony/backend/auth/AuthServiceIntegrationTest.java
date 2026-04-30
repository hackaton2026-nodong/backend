package com.kworkerharmony.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.kworkerharmony.backend.auth.dto.request.SignupRequest;
import com.kworkerharmony.backend.cases.domain.CaseRepository;
import com.kworkerharmony.backend.cases.domain.CaseStatus;
import com.kworkerharmony.backend.cases.entity.Case;
import com.kworkerharmony.backend.enterprise.CompanyInviteCode;
import com.kworkerharmony.backend.enterprise.CompanyInviteCodeRepository;
import com.kworkerharmony.backend.enterprise.Enterprise;
import com.kworkerharmony.backend.enterprise.EnterpriseRepository;
import com.kworkerharmony.backend.enterprise.EnterpriseStatus;
import com.kworkerharmony.backend.global.security.RedisTokenRepository;
import com.kworkerharmony.backend.user.Role;
import com.kworkerharmony.backend.user.User;
import com.kworkerharmony.backend.user.UserRepository;
import com.kworkerharmony.backend.user.UserType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private CompanyInviteCodeRepository companyInviteCodeRepository;

    @Autowired
    private CaseRepository caseRepository;

    @MockitoBean
    private RedisTokenRepository redisTokenRepository;

    @Test
    void signupAsAdminCreatesCompanyAndAssignsAdminRole() {
        authService.signup(new SignupRequest(
                "admin@example.com",
                "password123",
                "Admin",
                null,
                "010-1000-0000",
                null,
                null,
                "KR",
                "ko",
                null,
                "Harmony Co",
                "123-45-67890",
                "Manufacturing",
                "Seoul",
                5,
                "EPS-001",
                "KR",
                "ko"
        ));

        User savedUser = userRepository.findByEmail("admin@example.com").orElseThrow();
        assertThat(savedUser.getRole()).isEqualTo(Role.ADMIN);
        assertThat(savedUser.getEnterprise()).isNotNull();
        assertThat(savedUser.getEnterprise().getName()).isEqualTo("Harmony Co");
        assertThat(enterpriseRepository.findAll()).hasSize(1);
    }

    @Test
    void signupWithInviteCodeJoinsExistingCompany() {
        Enterprise company = enterpriseRepository.save(new Enterprise(
                "Existing Co",
                "999-99-99999",
                "Construction",
                "KR",
                "ko",
                EnterpriseStatus.ACTIVE
        ));
        companyInviteCodeRepository.save(new CompanyInviteCode(
                company,
                "join-code",
                LocalDateTime.now().plusDays(1),
                3,
                0,
                true,
                Role.WORKER
        ));

        authService.signup(new SignupRequest(
                "worker@example.com",
                "password123",
                "Worker",
                java.time.LocalDate.of(1995, 1, 1),
                "010-2000-0000",
                java.time.LocalDate.now().plusYears(1),
                UserType.WORKER,
                "KR",
                "ko",
                "join-code",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        User savedUser = userRepository.findByEmail("worker@example.com").orElseThrow();
        CompanyInviteCode inviteCode = companyInviteCodeRepository.findByCode("join-code").orElseThrow();

        assertThat(savedUser.getEnterprise()).isNotNull();
        assertThat(savedUser.getEnterprise().getId()).isEqualTo(company.getId());
        assertThat(savedUser.getRole()).isEqualTo(Role.WORKER);
        assertThat(savedUser.getUserType()).isEqualTo(UserType.WORKER);
        assertThat(inviteCode.getUsedCount()).isEqualTo(1);
    }

    @Test
    void signupWithCaseInviteCodeConnectsWorkerToPendingCase() {
        Enterprise company = enterpriseRepository.save(new Enterprise(
                "Existing Co",
                "999-99-99999",
                "Construction",
                "Seoul",
                5,
                "EPS-TEST-001",
                "KR",
                "ko",
                EnterpriseStatus.ACTIVE
        ));
        User employer = userRepository.save(new User(
                "employer@example.com",
                "encoded",
                "Employer",
                null,
                "010-1000-0000",
                null,
                Role.ADMIN,
                UserType.EMPLOYER,
                com.kworkerharmony.backend.user.UserStatus.ACTIVE,
                "KR",
                "ko",
                company
        ));
        Case pendingCase = caseRepository.save(new Case(
                company,
                employer,
                null,
                CaseStatus.PENDING,
                "Construction",
                "Seoul"
        ));
        companyInviteCodeRepository.save(new CompanyInviteCode(
                company,
                pendingCase.getId(),
                "case-join-code",
                LocalDateTime.now().plusDays(1),
                1,
                0,
                true,
                Role.WORKER
        ));

        authService.signup(new SignupRequest(
                "case.worker@example.com",
                "password123",
                "Case Worker",
                java.time.LocalDate.of(1998, 1, 1),
                "010-3000-0000",
                java.time.LocalDate.now().plusYears(1),
                UserType.WORKER,
                "KR",
                "ko",
                "case-join-code",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        Case connectedCase = caseRepository.findById(pendingCase.getId()).orElseThrow();
        User savedUser = userRepository.findByEmail("case.worker@example.com").orElseThrow();

        assertThat(connectedCase.getStatus()).isEqualTo(CaseStatus.ACTIVE);
        assertThat(connectedCase.getWorker().getId()).isEqualTo(savedUser.getId());
    }
}
