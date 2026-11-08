// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.system.rbac.biz.config.LoginLockProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LoginLockServiceRedisTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private LoginLockProperties redisProps;

    @BeforeEach
    void setUp() {
        redisProps = new LoginLockProperties(3, 15, "redis");
    }

    private LoginLockService redisService() {
        org.mockito.Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        return new LoginLockService(redisProps, new org.springframework.beans.factory.ObjectProvider<>() {
            @Override
            public StringRedisTemplate getObject() {
                return redisTemplate;
            }

            @Override
            public StringRedisTemplate getIfAvailable() {
                return redisTemplate;
            }
        });
    }

    @Nested
    class RedisModeTests {

        @Test
        void checkNotLocked_NoValue_Passes() {
            LoginLockService service = redisService();
            when(valueOps.get("strata:login-lock:user-r")).thenReturn(null);

            assertThatCode(() -> service.checkNotLocked("user-r")).doesNotThrowAnyException();
        }

        @Test
        void checkNotLocked_TooMany_Throws() {
            LoginLockService service = redisService();
            when(valueOps.get("strata:login-lock:user-l")).thenReturn("3");

            assertThatThrownBy(() -> service.checkNotLocked("user-l")).isInstanceOf(BusinessException.class);
        }

        @Test
        void checkNotLocked_InvalidValue_TreatedAsZero() {
            LoginLockService service = redisService();
            when(valueOps.get("strata:login-lock:user-x")).thenReturn("not-a-number");

            assertThatCode(() -> service.checkNotLocked("user-x")).doesNotThrowAnyException();
        }

        @Test
        void recordFailure_IncrementsAndExpires() {
            LoginLockService service = redisService();

            service.recordFailure("user-rf");

            verify(valueOps).increment("strata:login-lock:user-rf");
            verify(redisTemplate)
                    .expire(
                            org.mockito.ArgumentMatchers.eq("strata:login-lock:user-rf"),
                            org.mockito.ArgumentMatchers.any(Duration.class));
        }

        @Test
        void clear_DeletesRedisKey() {
            LoginLockService service = redisService();

            service.clear("user-cl");

            verify(redisTemplate).delete("strata:login-lock:user-cl");
        }
    }

    @Nested
    class MemoryExpiryTests {

        private LoginLockService memoryService() {
            LoginLockProperties props = new LoginLockProperties(3, 15, "memory");
            return new LoginLockService(props, new org.springframework.beans.factory.ObjectProvider<>() {
                @Override
                public StringRedisTemplate getObject() {
                    return null;
                }

                @Override
                public StringRedisTemplate getIfAvailable() {
                    return null;
                }
            });
        }

        @Test
        void memoryFailures_ExpiredLock_AutoClears() throws Exception {
            LoginLockService service = memoryService();
            // 用反射塞入一个已过期的锁定条目（Entry 是私有 record）
            @SuppressWarnings("unchecked")
            Map<String, Object> entries = (Map<String, Object>) ReflectionTestUtils.getField(service, "memoryEntries");
            Class<?> entryClass = Class.forName("dev.strata.system.rbac.biz.service.support.LoginLockService$Entry");
            java.lang.reflect.Constructor<?> ctor = entryClass.getDeclaredConstructor(int.class, Instant.class);
            ctor.setAccessible(true);
            Object expiredEntry = ctor.newInstance(5, Instant.now().minusSeconds(60));
            entries.put("user-expired", expiredEntry);

            assertThatCode(() -> service.checkNotLocked("user-expired")).doesNotThrowAnyException();
            // 过期后条目应被移除
            assertThat(entries).doesNotContainKey("user-expired");
        }

        @Test
        void recordFailure_WhenLocked_ResetsToOne() {
            LoginLockService service = memoryService();
            // 锁定用户
            for (int i = 0; i < 3; i++) {
                service.recordFailure("user-lock-reset");
            }
            assertThatThrownBy(() -> service.checkNotLocked("user-lock-reset")).isInstanceOf(BusinessException.class);

            // 再失败一次 → 因 lockedUntil != null，compute 重置为 1
            service.recordFailure("user-lock-reset");

            // 重置后 1 < 3，应通过
            assertThatCode(() -> service.checkNotLocked("user-lock-reset")).doesNotThrowAnyException();
        }
    }
}
