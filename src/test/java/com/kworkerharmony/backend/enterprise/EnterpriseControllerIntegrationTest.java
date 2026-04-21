package com.kworkerharmony.backend.enterprise;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kworkerharmony.backend.country.Country;
import com.kworkerharmony.backend.country.CountryRepository;
import com.kworkerharmony.backend.global.security.JwtProvider;
import com.kworkerharmony.backend.global.security.RedisTokenRepository;
import com.kworkerharmony.backend.user.Role;
import com.kworkerharmony.backend.user.User;
import com.kworkerharmony.backend.user.UserRepository;
import com.kworkerharmony.backend.user.UserStatus;
import com.kworkerharmony.backend.user.UserType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EnterpriseControllerIntegrationTest {

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
    private CompanyInviteCodeRepository companyInviteCodeRepository;

    @MockitoBean
    private RedisTokenRepository redisTokenRepository;

    @BeforeEach
    void setUp() {
        when(redisTokenRepository.isBlacklisted(anyString())).thenReturn(false);
        if (countryRepository.findByCountryCode("KR").isEmpty()) {
            countryRepository.save(new Country("KR", "Korea"));
        }
    }

    @Test
    void adminCreatesInviteCodeForOwnCompany() throws Exception {
        Enterprise company = saveEnterprise("Company A", "111-11-11111");
        User admin = saveUser("admin@company-a.com", Role.ADMIN, UserType.EMPLOYER, company);

        mockMvc.perform(post("/api/companies/invite-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateInviteCodePayload(
                                LocalDateTime.now().plusDays(1),
                                3,
                                Role.WORKER
                        )))
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.companyId").value(company.getId()))
                .andExpect(jsonPath("$.data.maxUses").value(3))
                .andExpect(jsonPath("$.data.defaultRole").value(Role.WORKER.name()));
    }

    @Test
    void nonAdminCannotCreateInviteCode() throws Exception {
        Enterprise company = saveEnterprise("Company A", "111-11-11111");
        User employer = saveUser("employer@company-a.com", Role.EMPLOYER, UserType.EMPLOYER, company);

        mockMvc.perform(post("/api/companies/invite-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateInviteCodePayload(
                                LocalDateTime.now().plusDays(1),
                                3,
                                Role.WORKER
                        )))
                        .header("Authorization", bearerToken(employer)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTH_403"));
    }

    @Test
    void joinCompanyAssignsEnterpriseAndRoleFromInviteCode() throws Exception {
        Enterprise company = saveEnterprise("Company A", "111-11-11111");
        User admin = saveUser("admin@company-a.com", Role.ADMIN, UserType.EMPLOYER, company);
        User pendingWorker = saveUserWithoutCompany("worker@outside.com");

        MvcResult inviteCodeResult = mockMvc.perform(post("/api/companies/invite-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateInviteCodePayload(
                                LocalDateTime.now().plusDays(1),
                                2,
                                Role.WORKER
                        )))
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk())
                .andReturn();

        String inviteCode = readData(inviteCodeResult).path("code").asText();

        mockMvc.perform(post("/api/companies/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinCompanyPayload(inviteCode)))
                        .header("Authorization", bearerToken(pendingWorker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(company.getId()));

        User joinedUser = userRepository.findById(pendingWorker.getId()).orElseThrow();
        CompanyInviteCode usedInviteCode = companyInviteCodeRepository.findByCode(inviteCode).orElseThrow();

        org.assertj.core.api.Assertions.assertThat(joinedUser.getEnterprise()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(joinedUser.getEnterprise().getId()).isEqualTo(company.getId());
        org.assertj.core.api.Assertions.assertThat(joinedUser.getRole()).isEqualTo(Role.WORKER);
        org.assertj.core.api.Assertions.assertThat(joinedUser.getUserType()).isEqualTo(UserType.WORKER);
        org.assertj.core.api.Assertions.assertThat(usedInviteCode.getUsedCount()).isEqualTo(1);
    }

    @Test
    void adminGetsOnlyOwnCompanyUsers() throws Exception {
        Enterprise companyA = saveEnterprise("Company A", "111-11-11111");
        Enterprise companyB = saveEnterprise("Company B", "222-22-22222");
        User admin = saveUser("admin@company-a.com", Role.ADMIN, UserType.EMPLOYER, companyA);
        saveUser("employer@company-a.com", Role.EMPLOYER, UserType.EMPLOYER, companyA);
        saveUser("worker@company-a.com", Role.WORKER, UserType.WORKER, companyA);
        saveUser("outsider@company-b.com", Role.EMPLOYER, UserType.EMPLOYER, companyB);

        mockMvc.perform(get("/api/companies/users")
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[?(@.email=='admin@company-a.com')]").exists())
                .andExpect(jsonPath("$.data[?(@.email=='employer@company-a.com')]").exists())
                .andExpect(jsonPath("$.data[?(@.email=='worker@company-a.com')]").exists())
                .andExpect(jsonPath("$.data[?(@.email=='outsider@company-b.com')]").doesNotExist());
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
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
                EnterpriseStatus.ACTIVE
        ));
    }

    private User saveUser(String email, Role role, UserType userType, Enterprise enterprise) {
        Country country = countryRepository.findByCountryCode("KR").orElseThrow();
        return userRepository.save(new User(
                email,
                "encoded",
                email,
                role,
                userType,
                UserStatus.ACTIVE,
                country,
                enterprise
        ));
    }

    private User saveUserWithoutCompany(String email) {
        Country country = countryRepository.findByCountryCode("KR").orElseThrow();
        return userRepository.save(new User(
                email,
                "encoded",
                email,
                Role.WORKER,
                UserType.WORKER,
                UserStatus.ACTIVE,
                country,
                null
        ));
    }

    private record CreateInviteCodePayload(
            LocalDateTime expiresAt,
            int maxUses,
            Role defaultRole
    ) {
    }

    private record JoinCompanyPayload(
            String inviteCode
    ) {
    }
}
