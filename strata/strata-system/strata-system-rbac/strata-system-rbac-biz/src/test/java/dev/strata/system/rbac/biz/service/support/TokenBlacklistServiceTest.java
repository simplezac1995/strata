// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.system.rbac.biz.config.TokenBlacklistProperties;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SuppressWarnings({"unchecked", "varargs"})
class TokenBlacklistServiceTest {

    private TokenBlacklistService blacklistService;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        TokenBlacklistProperties properties = new TokenBlacklistProperties(true);
        ObjectProvider<StringRedisTemplate> redisProvider = new ObjectProvider<>() {
            @Override
            public StringRedisTemplate getObject() {
                return redisTemplate;
            }

            @Override
            public StringRedisTemplate getIfAvailable() {
                return redisTemplate;
            }
        };
        blacklistService = new TokenBlacklistService(redisProvider, properties);
    }

    @Nested
    @DisplayName("黑名单启用状态")
    class BlacklistEnabled {

        @Test
        @DisplayName("应报告黑名单已启用")
        void shouldReportEnabled() {
            assertThat(blacklistService.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("应将有效 token 加入黑名单")
        void shouldBlacklistValidToken() {
            Claims claims = mockClaims("token-jti-123", new Date(System.currentTimeMillis() + 3600_000));

            blacklistService.blacklist(claims);

            verify(valueOps).set(eq("strata:refresh:blacklist:token-jti-123"), eq("1"), any(Duration.class));
        }

        @Test
        @DisplayName("应检测已加入黑名单的 token")
        void shouldDetectBlacklistedToken() {
            Claims claims = mockClaims("token-jti-456", new Date(System.currentTimeMillis() + 3600_000));
            when(redisTemplate.hasKey("strata:refresh:blacklist:token-jti-456")).thenReturn(true);

            boolean isBlacklisted = blacklistService.isBlacklisted(claims);

            assertThat(isBlacklisted).isTrue();
        }

        @Test
        @DisplayName("应检测未加入黑名单的 token")
        void shouldDetectNonBlacklistedToken() {
            Claims claims = mockClaims("token-jti-789", new Date(System.currentTimeMillis() + 3600_000));
            when(redisTemplate.hasKey("strata:refresh:blacklist:token-jti-789")).thenReturn(false);

            boolean isBlacklisted = blacklistService.isBlacklisted(claims);

            assertThat(isBlacklisted).isFalse();
        }

        @Test
        @DisplayName("已过期的 token 不应加入黑名单")
        void shouldNotBlacklistExpiredToken() {
            Claims claims = mockClaims("expired-jti", new Date(System.currentTimeMillis() - 1000));

            blacklistService.blacklist(claims);

            verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("无过期时间的 token 不应加入黑名单")
        void shouldNotBlacklistTokenWithoutExpiration() {
            Claims claims = mockClaims("no-exp-jti", null);

            blacklistService.blacklist(claims);

            verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("无 jti 的 token 不应加入黑名单")
        void shouldNotBlacklistTokenWithoutJti() {
            Claims claims = mockClaims(null, new Date(System.currentTimeMillis() + 3600_000));

            blacklistService.blacklist(claims);

            verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("无 jti 的 token 应返回未黑名单")
        void shouldReturnNotBlacklistedForTokenWithoutJti() {
            Claims claims = mockClaims(null, new Date(System.currentTimeMillis() + 3600_000));

            boolean isBlacklisted = blacklistService.isBlacklisted(claims);

            assertThat(isBlacklisted).isFalse();
        }
    }

    @Nested
    @DisplayName("黑名单禁用状态")
    class BlacklistDisabled {

        private TokenBlacklistService disabledService;

        @BeforeEach
        void setUpDisabled() {
            TokenBlacklistProperties disabledProps = new TokenBlacklistProperties(false);
            ObjectProvider<StringRedisTemplate> redisProvider = new ObjectProvider<>() {
                @Override
                public StringRedisTemplate getObject() {
                    return redisTemplate;
                }

                @Override
                public StringRedisTemplate getIfAvailable() {
                    return redisTemplate;
                }
            };
            disabledService = new TokenBlacklistService(redisProvider, disabledProps);
        }

        @Test
        @DisplayName("应报告黑名单未启用")
        void shouldReportDisabled() {
            assertThat(disabledService.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("禁用时不应将 token 加入黑名单")
        void shouldNotBlacklistWhenDisabled() {
            Claims claims = mockClaims("jti-disabled", new Date(System.currentTimeMillis() + 3600_000));

            disabledService.blacklist(claims);

            verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("禁用时应返回未黑名单")
        void shouldReturnNotBlacklistedWhenDisabled() {
            Claims claims = mockClaims("jti-disabled", new Date(System.currentTimeMillis() + 3600_000));

            boolean isBlacklisted = disabledService.isBlacklisted(claims);

            assertThat(isBlacklisted).isFalse();
        }
    }

    @Nested
    @DisplayName("无 Redis 状态")
    class NoRedisState {

        private TokenBlacklistService noRedisService;

        @BeforeEach
        void setUpNoRedis() {
            TokenBlacklistProperties properties = new TokenBlacklistProperties(true);
            ObjectProvider<StringRedisTemplate> redisProvider = new ObjectProvider<>() {
                @Override
                public StringRedisTemplate getObject() {
                    return null;
                }

                @Override
                public StringRedisTemplate getIfAvailable() {
                    return null;
                }
            };
            noRedisService = new TokenBlacklistService(redisProvider, properties);
        }

        @Test
        @DisplayName("无 Redis 时应报告未启用")
        void shouldReportDisabledWhenNoRedis() {
            assertThat(noRedisService.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("无 Redis 时 blacklist 应为 no-op")
        void shouldNoOpBlacklistWhenNoRedis() {
            Claims claims = mockClaims("jti-no-redis", new Date(System.currentTimeMillis() + 3600_000));

            noRedisService.blacklist(claims);
        }

        @Test
        @DisplayName("无 Redis 时 isBlacklisted 应返回 false")
        void shouldReturnFalseWhenNoRedis() {
            Claims claims = mockClaims("jti-no-redis", new Date(System.currentTimeMillis() + 3600_000));

            assertThat(noRedisService.isBlacklisted(claims)).isFalse();
        }
    }

    private Claims mockClaims(String jti, Date expiration) {
        Claims claims = mock(Claims.class);
        when(claims.getId()).thenReturn(jti);
        when(claims.getExpiration()).thenReturn(expiration);
        return claims;
    }
}
