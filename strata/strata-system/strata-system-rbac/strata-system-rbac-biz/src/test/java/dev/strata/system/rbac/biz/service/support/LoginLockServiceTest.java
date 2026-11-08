// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.system.rbac.biz.config.LoginLockProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

class LoginLockServiceTest {

    private LoginLockService loginLockService;
    private LoginLockProperties properties;

    @BeforeEach
    void setUp() {
        // 内存模式，最大尝试 3 次，锁定 15 分钟
        properties = new LoginLockProperties(3, 15, "memory");
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
        loginLockService = new LoginLockService(properties, redisProvider);
    }

    @Nested
    class NotLockedState {

        @Test
        void shouldNotLockNewUser() {
            loginLockService.checkNotLocked("new-user");
        }

        @Test
        void shouldNotLockWhenBelowMaxAttempts() {
            loginLockService.recordFailure("user1");
            loginLockService.recordFailure("user1");

            loginLockService.checkNotLocked("user1");
        }
    }

    @Nested
    class LockedState {

        @Test
        void shouldLockWhenMaxAttemptsReached() {
            loginLockService.recordFailure("user-locked");
            loginLockService.recordFailure("user-locked");
            loginLockService.recordFailure("user-locked");

            assertThatThrownBy(() -> loginLockService.checkNotLocked("user-locked"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Too many failed attempts");
        }

        @Test
        void shouldLockImmediatelyWhenMaxAttemptsReached() {
            for (int i = 0; i < 3; i++) {
                loginLockService.recordFailure("user-exceeded");
            }

            assertThatThrownBy(() -> loginLockService.checkNotLocked("user-exceeded"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    class ClearLock {

        @Test
        void shouldClearFailuresOnSuccess() {
            loginLockService.recordFailure("user-clear");
            loginLockService.recordFailure("user-clear");

            loginLockService.clear("user-clear");

            loginLockService.checkNotLocked("user-clear");
        }

        @Test
        void shouldAllowRetryAfterClear() {
            for (int i = 0; i < 3; i++) {
                loginLockService.recordFailure("user-retry");
            }
            assertThatThrownBy(() -> loginLockService.checkNotLocked("user-retry"))
                    .isInstanceOf(BusinessException.class);

            loginLockService.clear("user-retry");

            loginLockService.checkNotLocked("user-retry");
        }
    }

    @Nested
    class MultiUserIsolation {

        @Test
        void shouldIsolateLocksBetweenUsers() {
            for (int i = 0; i < 3; i++) {
                loginLockService.recordFailure("user-a");
            }

            assertThatThrownBy(() -> loginLockService.checkNotLocked("user-a")).isInstanceOf(BusinessException.class);

            // user-b 应该不受影响
            loginLockService.checkNotLocked("user-b");
        }

        @Test
        void shouldCountFailuresIndependently() {
            loginLockService.recordFailure("user-x");
            loginLockService.recordFailure("user-x");
            loginLockService.recordFailure("user-y");

            // user-x 有 2 次失败，未锁定
            loginLockService.checkNotLocked("user-x");
            // user-y 有 1 次失败，未锁定
            loginLockService.checkNotLocked("user-y");
        }
    }

    @Nested
    class ConfigDefaults {

        @Test
        void shouldUseDefaultMaxAttemptsForInvalidValue() {
            LoginLockProperties defaultProps = new LoginLockProperties(0, 15, "memory");
            ObjectProvider<StringRedisTemplate> nullProvider = new ObjectProvider<>() {
                @Override
                public StringRedisTemplate getObject() {
                    return null;
                }

                @Override
                public StringRedisTemplate getIfAvailable() {
                    return null;
                }
            };
            LoginLockService service = new LoginLockService(defaultProps, nullProvider);

            // 4 次失败不应锁定（默认 5 次）
            for (int i = 0; i < 4; i++) {
                service.recordFailure("user-default");
            }
            service.checkNotLocked("user-default");

            // 第 5 次失败后应锁定
            service.recordFailure("user-default");
            assertThatThrownBy(() -> service.checkNotLocked("user-default")).isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldUseMemoryStoreForBlankStore() {
            LoginLockProperties memoryProps = new LoginLockProperties(3, 15, "  ");
            ObjectProvider<StringRedisTemplate> nullProvider2 = new ObjectProvider<>() {
                @Override
                public StringRedisTemplate getObject() {
                    return null;
                }

                @Override
                public StringRedisTemplate getIfAvailable() {
                    return null;
                }
            };
            LoginLockService service = new LoginLockService(memoryProps, nullProvider2);

            service.recordFailure("user-blank-store");
            service.recordFailure("user-blank-store");
            service.recordFailure("user-blank-store");

            assertThatThrownBy(() -> service.checkNotLocked("user-blank-store")).isInstanceOf(BusinessException.class);
        }
    }
}
