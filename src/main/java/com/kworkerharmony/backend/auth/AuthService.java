package com.kworkerharmony.backend.auth;

import com.kworkerharmony.backend.auth.dto.request.LoginRequest;
import com.kworkerharmony.backend.auth.dto.request.LogoutRequest;
import com.kworkerharmony.backend.auth.dto.request.ReissueRequest;
import com.kworkerharmony.backend.auth.dto.request.SignupRequest;
import com.kworkerharmony.backend.auth.dto.response.LoginResponse;
import com.kworkerharmony.backend.auth.dto.response.ReissueResponse;
import com.kworkerharmony.backend.country.Country;
import com.kworkerharmony.backend.country.CountryRepository;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import com.kworkerharmony.backend.global.security.JwtProperties;
import com.kworkerharmony.backend.global.security.JwtProvider;
import com.kworkerharmony.backend.global.security.RedisTokenRepository;
import com.kworkerharmony.backend.user.Role;
import com.kworkerharmony.backend.user.User;
import com.kworkerharmony.backend.user.UserRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CountryRepository countryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RedisTokenRepository redisTokenRepository;

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE, "Email already exists");
        }

        Country country = countryRepository.findByCountryCode(request.countryCode())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Country not found"));

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                Role.USER,
                request.userType(),
                country
        );

        userRepository.save(user);
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
}
