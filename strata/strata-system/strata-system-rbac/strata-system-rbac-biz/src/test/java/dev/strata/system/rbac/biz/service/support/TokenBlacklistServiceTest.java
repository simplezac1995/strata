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
    class BlacklistEnabled {

        @Test
        void shouldReportEnabled() {
            assertThat(blacklistService.isEnabled()).isTrue();
        }

        @Test
        void shouldBlacklistValidToken() {
            Claims claims = mockClaims("token-jti-123", new Date(System.currentTimeMillis() + 3600_000));

            blacklistService.blacklist(claims);

            verify(valueOps).set(eq("strata:refresh:blacklist:token-jti-123"), eq("1"), any(Duration.class));
        }

        @Test
        void shouldDetectBlacklistedToken() {
            Claims claims = mockClaims("token-jti-456", new Date(System.currentTimeMillis() + 3600_000));
            when(redisTemplate.hasKey("strata:refresh:blacklist:token-jti-456")).thenReturn(true);

            boolean isBlacklisted = blacklistService.isBlacklisted(claims);

            assertThat(isBlacklisted).isTrue();
        }

        @Test
        void shouldDetectNonBlacklistedToken() {
            Claims claims = mockClaims("token-jti-789", new Date(System.currentTimeMillis() + 3600_000));
            when(redisTemplate.hasKey("strata:refresh:blacklist:token-jti-789")).thenReturn(false);

            boolean isBlacklisted = blacklistService.isBlacklisted(claims);

            assertThat(isBlacklisted).isFalse();
        }

        @Test
        void shouldNotBlacklistExpiredToken() {
            Claims claims = mockClaims("expired-jti", new Date(System.currentTimeMillis() - 1000));

            blacklistService.blacklist(claims);

            verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
        }

        @Test
        void shouldNotBlacklistTokenWithoutExpiration() {
            Claims claims = mockClaims("no-exp-jti", null);

            blacklistService.blacklist(claims);

            verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
        }

        @Test
        void shouldNotBlacklistTokenWithoutJti() {
            Claims claims = mockClaims(null, new Date(System.currentTimeMillis() + 3600_000));

            blacklistService.blacklist(claims);

            verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
        }

        @Test
        void shouldReturnNotBlacklistedForTokenWithoutJti() {
            Claims claims = mockClaims(null, new Date(System.currentTimeMillis() + 3600_000));

            boolean isBlacklisted = blacklistService.isBlacklisted(claims);

            assertThat(isBlacklisted).isFalse();
        }
    }

    @Nested
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
        void shouldReportDisabled() {
            assertThat(disabledService.isEnabled()).isFalse();
        }

        @Test
        void shouldNotBlacklistWhenDisabled() {
            Claims claims = mockClaims("jti-disabled", new Date(System.currentTimeMillis() + 3600_000));

            disabledService.blacklist(claims);

            verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
        }

        @Test
        void shouldReturnNotBlacklistedWhenDisabled() {
            Claims claims = mockClaims("jti-disabled", new Date(System.currentTimeMillis() + 3600_000));

            boolean isBlacklisted = disabledService.isBlacklisted(claims);

            assertThat(isBlacklisted).isFalse();
        }
    }

    @Nested
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
        void shouldReportDisabledWhenNoRedis() {
            assertThat(noRedisService.isEnabled()).isFalse();
        }

        @Test
        void shouldNoOpBlacklistWhenNoRedis() {
            Claims claims = mockClaims("jti-no-redis", new Date(System.currentTimeMillis() + 3600_000));

            noRedisService.blacklist(claims);
        }

        @Test
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
