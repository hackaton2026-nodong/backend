package com.kworkerharmony.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.kworkerharmony.backend.auth.dto.request.SignupRequest;
import com.kworkerharmony.backend.country.Country;
import com.kworkerharmony.backend.country.CountryRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
    private CountryRepository countryRepository;

    @MockBean
    private RedisTokenRepository redisTokenRepository;

    @BeforeEach
    void setUp() {
        if (countryRepository.findByCountryCode("KR").isEmpty()) {
            countryRepository.save(new Country("KR", "Korea"));
        }
    }

    @Test
    void signupAsAdminCreatesCompanyAndAssignsAdminRole() {
        authService.signup(new SignupRequest(
                "admin@example.com",
                "password123",
                "Admin",
                null,
                "KR",
                null,
                "Harmony Co",
                "123-45-67890",
                "Manufacturing",
                "KR"
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
                UserType.WORKER,
                "KR",
                "join-code",
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
}
