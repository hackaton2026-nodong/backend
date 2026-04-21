package com.kworkerharmony.backend.auth;

import com.kworkerharmony.backend.auth.dto.request.LoginRequest;
import com.kworkerharmony.backend.auth.dto.request.LogoutRequest;
import com.kworkerharmony.backend.auth.dto.request.ReissueRequest;
import com.kworkerharmony.backend.auth.dto.request.SignupRequest;
import com.kworkerharmony.backend.auth.dto.response.LoginResponse;
import com.kworkerharmony.backend.auth.dto.response.ReissueResponse;
import com.kworkerharmony.backend.enterprise.CompanyInviteCode;
import com.kworkerharmony.backend.enterprise.CompanyInviteCodeRepository;
import com.kworkerharmony.backend.enterprise.Enterprise;
import com.kworkerharmony.backend.enterprise.EnterpriseRepository;
import com.kworkerharmony.backend.enterprise.EnterpriseStatus;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import com.kworkerharmony.backend.global.security.JwtProperties;
import com.kworkerharmony.backend.global.security.JwtProvider;
import com.kworkerharmony.backend.global.security.RedisTokenRepository;
import com.kworkerharmony.backend.reference.country.CountryCatalog;
import com.kworkerharmony.backend.reference.language.LanguageCatalog;
import com.kworkerharmony.backend.user.Role;
import com.kworkerharmony.backend.user.User;
import com.kworkerharmony.backend.user.UserRepository;
import com.kworkerharmony.backend.user.UserStatus;
import com.kworkerharmony.backend.user.UserType;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final CompanyInviteCodeRepository companyInviteCodeRepository;
    private final CountryCatalog countryCatalog;
    private final LanguageCatalog languageCatalog;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RedisTokenRepository redisTokenRepository;

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE, "Email already exists");
        }

        validateCountryCode(request.countryCode());
        validateLanguageCode(request.languageCode());

        SignupTarget signupTarget = resolveSignupTarget(request);

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                signupTarget.role(),
                signupTarget.userType(),
                UserStatus.ACTIVE,
                countryCatalog.normalize(request.countryCode()),
                languageCatalog.normalize(request.languageCode()),
                signupTarget.enterprise()
        );

        userRepository.save(user);
    }

    private SignupTarget resolveSignupTarget(SignupRequest request) {
        if (request.inviteCode() != null && !request.inviteCode().isBlank()) {
            CompanyInviteCode inviteCode = companyInviteCodeRepository.findByCode(request.inviteCode())
                    .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Invite code not found"));

            if (!inviteCode.isUsableAt(LocalDateTime.now())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Invite code is expired or exhausted");
            }

            Role role = inviteCode.getDefaultRole();
            UserType userType = toUserType(role);
            inviteCode.use();
            return new SignupTarget(inviteCode.getEnterprise(), role, userType);
        }

        Enterprise enterprise = enterpriseRepository.save(new Enterprise(
                request.companyName(),
                request.companyBusinessNumber(),
                request.companyIndustry(),
                validatedCountryCode(request.companyCountryCode()),
                validatedLanguageCode(request.companyLanguageCode()),
                EnterpriseStatus.ACTIVE
        ));
        return new SignupTarget(enterprise, Role.ADMIN, UserType.EMPLOYER);
    }

    private UserType toUserType(Role role) {
        return switch (role) {
            case EMPLOYER, ADMIN -> UserType.EMPLOYER;
            case WORKER -> UserType.WORKER;
        };
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        redisTokenRepository.saveRefreshToken(
                user.getId(),
                refreshToken,
                Duration.ofSeconds(jwtProperties.refreshTokenExpirationSeconds())
        );

        return new LoginResponse(accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public ReissueResponse reissue(ReissueRequest request) {
        jwtProvider.validateRefreshToken(request.refreshToken());

        Long userId = jwtProvider.getUserId(request.refreshToken());
        String storedRefreshToken = redisTokenRepository.getRefreshToken(userId);

        if (storedRefreshToken == null) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        if (!storedRefreshToken.equals(request.refreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "Refresh token does not match");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));

        return new ReissueResponse(jwtProvider.generateAccessToken(user));
    }

    public void logout(LogoutRequest request) {
        jwtProvider.validateAccessTokenAllowExpired(request.accessToken());
        Long userId = jwtProvider.getUserIdAllowExpired(request.accessToken());
        redisTokenRepository.deleteRefreshToken(userId);
        redisTokenRepository.blacklistAccessToken(
                request.accessToken(),
                jwtProvider.getRemainingValidity(request.accessToken())
        );
    }

    private record SignupTarget(
            Enterprise enterprise,
            Role role,
            UserType userType
    ) {
    }

    private void validateCountryCode(String countryCode) {
        if (!countryCatalog.exists(countryCode)) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Country not found");
        }
    }

    private void validateLanguageCode(String languageCode) {
        if (!languageCatalog.exists(languageCode)) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Language not found");
        }
    }

    private String validatedCountryCode(String countryCode) {
        validateCountryCode(countryCode);
        return countryCatalog.normalize(countryCode);
    }

    private String validatedLanguageCode(String languageCode) {
        validateLanguageCode(languageCode);
        return languageCatalog.normalize(languageCode);
    }
}
