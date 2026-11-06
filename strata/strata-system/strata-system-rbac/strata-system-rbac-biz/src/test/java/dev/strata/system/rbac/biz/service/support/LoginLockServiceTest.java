// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.system.rbac.biz.config.LoginLockProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("未锁定状态")
    class NotLockedState {

        @Test
        @DisplayName("新用户应未被锁定")
        void shouldNotLockNewUser() {
            loginLockService.checkNotLocked("new-user");
        }

        @Test
        @DisplayName("失败次数未达上限应未被锁定")
        void shouldNotLockWhenBelowMaxAttempts() {
            loginLockService.recordFailure("user1");
            loginLockService.recordFailure("user1");

            loginLockService.checkNotLocked("user1");
        }
    }

    @Nested
    @DisplayName("锁定状态")
    class LockedState {

        @Test
        @DisplayName("达到最大尝试次数应被锁定")
        void shouldLockWhenMaxAttemptsReached() {
            loginLockService.recordFailure("user-locked");
            loginLockService.recordFailure("user-locked");
            loginLockService.recordFailure("user-locked");

            assertThatThrownBy(() -> loginLockService.checkNotLocked("user-locked"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Too many failed attempts");
        }

        @Test
        @DisplayName("达到最大尝试次数后应立即被锁定")
        void shouldLockImmediatelyWhenMaxAttemptsReached() {
            for (int i = 0; i < 3; i++) {
                loginLockService.recordFailure("user-exceeded");
            }

            assertThatThrownBy(() -> loginLockService.checkNotLocked("user-exceeded"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("清除锁定")
    class ClearLock {

        @Test
        @DisplayName("成功登录后应清除失败记录")
        void shouldClearFailuresOnSuccess() {
            loginLockService.recordFailure("user-clear");
            loginLockService.recordFailure("user-clear");

            loginLockService.clear("user-clear");

            loginLockService.checkNotLocked("user-clear");
        }

        @Test
        @DisplayName("清除锁定后应能重新尝试")
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
    @DisplayName("多用户隔离")
    class MultiUserIsolation {

        @Test
        @DisplayName("一个用户锁定不应影响其他用户")
        void shouldIsolateLocksBetweenUsers() {
            for (int i = 0; i < 3; i++) {
                loginLockService.recordFailure("user-a");
            }

            assertThatThrownBy(() -> loginLockService.checkNotLocked("user-a")).isInstanceOf(BusinessException.class);

            // user-b 应该不受影响
            loginLockService.checkNotLocked("user-b");
        }

        @Test
        @DisplayName("不同用户的失败计数应独立")
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
    @DisplayName("配置默认值")
    class ConfigDefaults {

        @Test
        @DisplayName("无效的最大尝试次数应使用默认值 5")
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
        @DisplayName("空存储类型应使用 memory 模式")
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
