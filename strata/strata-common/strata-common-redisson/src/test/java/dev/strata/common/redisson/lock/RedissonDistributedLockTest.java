// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.redisson.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

/**
 * Unit tests for {@link RedissonDistributedLock}: key prefixing, tryLock/unlock semantics,
 * watchdog lease=-1 contract, and executeWithLock error paths.
 */
@DisplayName("RedissonDistributedLock Redisson 锁门面")
class RedissonDistributedLockTest {

    private RedissonClient redissonClient;
    private RedissonDistributedLock manager;

    @BeforeEach
    void setUp() {
        redissonClient = mock(RedissonClient.class);
        manager = new RedissonDistributedLock(redissonClient);
    }

    @AfterEach
    void clearInterrupt() {
        // executeWithLock sets the interrupt flag when lock.tryLock throws InterruptedException.
        Thread.interrupted();
    }

    @Test
    @DisplayName("getLock 加 strata:lock: 前缀")
    void getLockAddsPrefix() {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("strata:lock:order")).thenReturn(lock);

        assertThat(manager.getLock("order")).isSameAs(lock);
        verify(redissonClient).getLock("strata:lock:order");
    }

    @Test
    @DisplayName("getFairLock 加 fair: 前缀")
    void getFairLockAddsFairPrefix() {
        RLock lock = mock(RLock.class);
        when(redissonClient.getFairLock("strata:lock:fair:order")).thenReturn(lock);

        assertThat(manager.getFairLock("order")).isSameAs(lock);
    }

    @Test
    @DisplayName("getReadWriteLock 加 rw: 前缀")
    void getReadWriteLockAddsRwPrefix() {
        RReadWriteLock rw = mock(RReadWriteLock.class);
        when(redissonClient.getReadWriteLock("strata:lock:rw:order")).thenReturn(rw);

        assertThat(manager.getReadWriteLock("order")).isSameAs(rw);
    }

    @Test
    @DisplayName("tryLock 透传 wait/lease 参数，成功返回 true")
    void tryLockDelegatesToRLock() throws Exception {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("strata:lock:order")).thenReturn(lock);
        when(lock.tryLock(5, 30, TimeUnit.SECONDS)).thenReturn(true);

        assertThat(manager.tryLock("order", 5, 30, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("tryLock 获取失败返回 false")
    void tryLockReturnsFalseWhenNotAcquired() throws Exception {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("strata:lock:order")).thenReturn(lock);
        when(lock.tryLock(5, 30, TimeUnit.SECONDS)).thenReturn(false);

        assertThat(manager.tryLock("order", 5, 30, TimeUnit.SECONDS)).isFalse();
    }

    @Test
    @DisplayName("tryLockWithWatchdog 以 leaseTime=-1 启用自动续期")
    void tryLockWithWatchdogPassesNegativeOneLease() throws Exception {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("strata:lock:order")).thenReturn(lock);
        when(lock.tryLock(5, -1, TimeUnit.SECONDS)).thenReturn(true);

        assertThat(manager.tryLockWithWatchdog("order", 5, TimeUnit.SECONDS)).isTrue();
        verify(lock).tryLock(5, -1, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("当前线程持锁时 unlock 释放")
    void unlockReleasesWhenHeldByCurrentThread() {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("strata:lock:order")).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        manager.unlock("order");

        verify(lock).unlock();
    }

    @Test
    @DisplayName("非当前线程持锁时 unlock 不调用 unlock")
    void unlockSkipsWhenNotHeldByCurrentThread() {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("strata:lock:order")).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(false);

        manager.unlock("order");

        verify(lock, never()).unlock();
    }

    @Test
    @DisplayName("isLocked 与 isHeldByCurrentThread 透传")
    void stateChecksDelegate() {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("strata:lock:order")).thenReturn(lock);
        when(lock.isLocked()).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(false);

        assertThat(manager.isLocked("order")).isTrue();
        assertThat(manager.isHeldByCurrentThread("order")).isFalse();
    }

    @Test
    @DisplayName("forceUnlock 透传结果")
    void forceUnlockDelegates() {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("strata:lock:order")).thenReturn(lock);
        when(lock.forceUnlock()).thenReturn(true);

        assertThat(manager.forceUnlock("order")).isTrue();
    }

    @Test
    @DisplayName("executeWithLock 成功时执行任务并释放锁")
    void executeWithLockRunsTaskAndReleases() throws Exception {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("strata:lock:order")).thenReturn(lock);
        when(lock.tryLock(5, 30, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        AtomicBoolean ran = new AtomicBoolean(false);

        boolean result = manager.executeWithLock("order", 5, 30, TimeUnit.SECONDS, () -> ran.set(true));

        assertThat(result).isTrue();
        assertThat(ran).isTrue();
        InOrder ordered = inOrder(lock);
        ordered.verify(lock).tryLock(5, 30, TimeUnit.SECONDS);
        ordered.verify(lock).unlock();
    }

    @Test
    @DisplayName("executeWithLock 获取锁失败时不执行任务返回 false")
    void executeWithLockReturnsFalseWhenNotAcquired() throws Exception {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("strata:lock:order")).thenReturn(lock);
        when(lock.tryLock(5, 30, TimeUnit.SECONDS)).thenReturn(false);
        AtomicBoolean ran = new AtomicBoolean(false);

        boolean result = manager.executeWithLock("order", 5, 30, TimeUnit.SECONDS, () -> ran.set(true));

        assertThat(result).isFalse();
        assertThat(ran).isFalse();
        verify(lock, never()).unlock();
    }

    @Test
    @DisplayName("executeWithLock 等待锁被中断时返回 false 并设置中断标志")
    void executeWithLockHandlesInterruption() throws Exception {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("strata:lock:order")).thenReturn(lock);
        when(lock.tryLock(5, 30, TimeUnit.SECONDS)).thenThrow(new InterruptedException("interrupted"));
        AtomicBoolean ran = new AtomicBoolean(false);

        boolean result = manager.executeWithLock("order", 5, 30, TimeUnit.SECONDS, () -> ran.set(true));

        assertThat(result).isFalse();
        assertThat(ran).isFalse();
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }
}
