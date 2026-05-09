package com.kworkerharmony.backend.global.security;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisTokenRepository {

    private static final String REFRESH_PREFIX = "refresh:";
    private static final String ACCESS_BLACKLIST_PREFIX = "blacklist:access:";

    private final StringRedisTemplate stringRedisTemplate;

    public void saveRefreshToken(Long userId, String refreshToken, Duration ttl) {
        stringRedisTemplate.opsForValue().set(refreshKey(userId), refreshToken, ttl);
    }

    public String getRefreshToken(Long userId) {
        return stringRedisTemplate.opsForValue().get(refreshKey(userId));
    }

    public void deleteRefreshToken(Long userId) {
        stringRedisTemplate.delete(refreshKey(userId));
    }

    public void blacklistAccessToken(String accessToken, Duration ttl) {
        if (!ttl.isPositive()) {
            return;
        }
        stringRedisTemplate.opsForValue().set(accessBlacklistKey(accessToken), "true", ttl);
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(accessBlacklistKey(accessToken)));
    }

    private String refreshKey(Long userId) {
        return REFRESH_PREFIX + userId;
    }

    private String accessBlacklistKey(String accessToken) {
        return ACCESS_BLACKLIST_PREFIX + accessToken;
    }
}
