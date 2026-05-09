package com.kworkerharmony.backend.global.security;

import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import com.kworkerharmony.backend.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private static final String USER_ID_CLAIM = "userId";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = createSecretKey(jwtProperties.secret());
    }

    public String generateAccessToken(User user) {
        return generateToken(user, ACCESS_TOKEN_TYPE, jwtProperties.accessTokenExpirationSeconds());
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, REFRESH_TOKEN_TYPE, jwtProperties.refreshTokenExpirationSeconds());
    }

    public boolean validateAccessToken(String token) {
        validateToken(token, ACCESS_TOKEN_TYPE);
        return true;
    }

    public boolean validateRefreshToken(String token) {
        validateToken(token, REFRESH_TOKEN_TYPE);
        return true;
    }

    public boolean validateAccessTokenAllowExpired(String token) {
        validateTokenAllowExpired(token, ACCESS_TOKEN_TYPE);
        return true;
    }

    public Long getUserId(String token) {
        Object userId = parseClaims(token).get(USER_ID_CLAIM);
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(userId));
    }

    public Long getUserIdAllowExpired(String token) {
        Object userId = parseClaimsAllowExpired(token).get(USER_ID_CLAIM);
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(userId));
    }

    public Duration getRemainingValidity(String token) {
        Date expiration = parseClaimsAllowExpired(token).getExpiration();
        long millis = expiration.getTime() - System.currentTimeMillis();
        return Duration.ofMillis(Math.max(millis, 0));
    }

    private String generateToken(User user, String tokenType, long expirationSeconds) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim(USER_ID_CLAIM, user.getId())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    private void validateToken(String token, String expectedTokenType) {
        Claims claims = parseClaims(token);
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);

        if (!expectedTokenType.equals(tokenType)) {
            throw new CustomException(ErrorCode.TOKEN_TYPE_MISMATCH);
        }
    }

    private void validateTokenAllowExpired(String token, String expectedTokenType) {
        Claims claims = parseClaimsAllowExpired(token);
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);

        if (!expectedTokenType.equals(tokenType)) {
            throw new CustomException(ErrorCode.TOKEN_TYPE_MISMATCH);
        }
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (SecurityException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException ex) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (ExpiredJwtException ex) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "Expired token");
        }
    }

    private Claims parseClaimsAllowExpired(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            return ex.getClaims();
        } catch (SecurityException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException ex) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    private SecretKey createSecretKey(String secret) {
        String actualSecret = Objects.requireNonNull(secret, "jwt.secret must not be null");
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(actualSecret));
        } catch (IllegalArgumentException | DecodingException ex) {
            return Keys.hmacShaKeyFor(actualSecret.getBytes(StandardCharsets.UTF_8));
        }
    }
}
