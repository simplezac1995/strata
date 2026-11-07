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
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

/**
 * Unit tests for {@link RedissonDistributedLock}: key prefixing, tryLock/unlock semantics,
 * watchdog lease=-1 contract, and executeWithLock error paths.
 */
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
    void getLockAddsPrefix() {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("strata:lock:order")).thenReturn(lock);

        assertThat(manager.getLock("order")).isSameAs(lock);
        verify(redissonClient).getLock("strata:lock:order");
    }

    @Test
    void getFairLockAddsFairPrefix() {
        RLock lock = mock(RLock.class);
        when(redissonClient.getFairLock("strata:lock:fair:order")).thenReturn(lock);

        assertThat(manager.getFairLock("order")).isSameAs(lock);
    }

    @Test
    void getReadWriteLockAddsRwPrefix() {
        RReadWriteLock rw = mock(RReadWriteLock.class);
        when(redissonClient.getReadWriteLock("strata:lock:rw:order")).thenReturn(rw);

        assertThat(manager.getReadWriteLock("order")).isSameAs(rw);
    }

    @Test
    void tryLockDelegatesToRLock() throws Exception {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("strata:lock:order")).thenReturn(lock);
        when(lock.tryLock(5, 30, TimeUnit.SECONDS)).thenReturn(true);

        assertThat(manager.tryLock("order", 5, 30, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void tryLockReturnsFalseWhenNotAcquired() throws Exception {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("strata:lock:order")).thenReturn(lock);
        when(lock.tryLock(5, 30, TimeUnit.SECONDS)).thenReturn(false);

        assertThat(manager.tryLock("order", 5, 30, TimeUnit.SECONDS)).isFalse();
    }

    @Test
    void tryLockWithWatchdogPassesNegativeOneLease() throws Exception {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("strata:lock:order")).thenReturn(lock);
        when(lock.tryLock(5, -1, TimeUnit.SECONDS)).thenReturn(true);

        assertThat(manager.tryLockWithWatchdog("order", 5, TimeUnit.SECONDS)).isTrue();
        verify(lock).tryLock(5, -1, TimeUnit.SECONDS);
    }

    @Test
    void unlockReleasesWhenHeldByCurrentThread() {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("strata:lock:order")).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        manager.unlock("order");

        verify(lock).unlock();
    }

    @Test
    void unlockSkipsWhenNotHeldByCurrentThread() {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("strata:lock:order")).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(false);

        manager.unlock("order");

        verify(lock, never()).unlock();
    }

    @Test
    void stateChecksDelegate() {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("strata:lock:order")).thenReturn(lock);
        when(lock.isLocked()).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(false);

        assertThat(manager.isLocked("order")).isTrue();
        assertThat(manager.isHeldByCurrentThread("order")).isFalse();
    }

    @Test
    void forceUnlockDelegates() {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("strata:lock:order")).thenReturn(lock);
        when(lock.forceUnlock()).thenReturn(true);

        assertThat(manager.forceUnlock("order")).isTrue();
    }

    @Test
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
