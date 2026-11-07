// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.redisson.lock;

import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Distributed lock implementation backed by Redisson.
 *
 * <p>Provides a rich set of lock types:
 * <ul>
 *   <li>Reentrant lock - same thread can acquire multiple times</li>
 *   <li>Fair lock - threads acquire in FIFO order</li>
 *   <li>ReadWrite lock - multiple readers, exclusive writer</li>
 *   <li>RedLock - multi-node consensus lock</li>
 * </ul>
 *
 * <p>Redisson locks include automatic watchdog renewal: if the lock holder
 * is still alive, the lease is automatically extended every 1/3 of the
 * lease time. This prevents lock expiration during long-running operations.
 *
 * <p>Usage:
 * <pre>{@code
 * @Autowired
 * private RedissonDistributedLock lockManager;
 *
 * // Acquire and release
 * Lock lock = lockManager.getLock("my-resource");
 * if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
 *     try {
 *         // critical section
 *     } finally {
 *         lock.unlock();
 *     }
 * }
 *
 * // Fair lock
 * Lock fairLock = lockManager.getFairLock("my-resource");
 *
 * // ReadWrite lock
 * ReadWriteLock rwLock = lockManager.getReadWriteLock("my-resource");
 * rwLock.readLock().lock();
 * }</pre>
 */
public class RedissonDistributedLock {

    private static final Logger log = LoggerFactory.getLogger(RedissonDistributedLock.class);

    private final RedissonClient redissonClient;

    /**
     * Lock key prefix to avoid collisions with other Redis keys.
     */
    private static final String LOCK_PREFIX = "strata:lock:";

    public RedissonDistributedLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * Get a reentrant distributed lock.
     *
     * @param name the lock name
     * @return the Redisson RLock (implements java.util.concurrent.locks.Lock)
     */
    public RLock getLock(String name) {
        String key = LOCK_PREFIX + name;
        RLock lock = redissonClient.getLock(key);
        log.debug("Got reentrant lock: {}", key);
        return lock;
    }

    /**
     * Get a fair distributed lock (FIFO ordering).
     *
     * @param name the lock name
     * @return the fair RLock
     */
    public RLock getFairLock(String name) {
        String key = LOCK_PREFIX + "fair:" + name;
        RLock lock = redissonClient.getFairLock(key);
        log.debug("Got fair lock: {}", key);
        return lock;
    }

    /**
     * Get a read-write distributed lock.
     *
     * @param name the lock name
     * @return the RReadWriteLock
     */
    public org.redisson.api.RReadWriteLock getReadWriteLock(String name) {
        String key = LOCK_PREFIX + "rw:" + name;
        org.redisson.api.RReadWriteLock lock = redissonClient.getReadWriteLock(key);
        log.debug("Got read-write lock: {}", key);
        return lock;
    }

    /**
     * Try to acquire a lock with timeout and lease time.
     *
     * @param name the lock name
     * @param waitTime maximum time to wait for the lock
     * @param leaseTime time after which the lock is automatically released
     * @param unit time unit
     * @return true if the lock was acquired, false otherwise
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public boolean tryLock(String name, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        RLock lock = getLock(name);
        boolean acquired = lock.tryLock(waitTime, leaseTime, unit);
        if (acquired) {
            log.debug("Acquired lock: {} (lease: {} {})", name, leaseTime, unit);
        } else {
            log.debug("Failed to acquire lock: {} (wait timeout: {} {})", name, waitTime, unit);
        }
        return acquired;
    }

    /**
     * Acquire a lock with automatic watchdog renewal (no explicit lease time).
     * The lock will be held until explicitly released.
     *
     * @param name the lock name
     * @param waitTime maximum time to wait for the lock
     * @param unit time unit
     * @return true if the lock was acquired, false otherwise
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public boolean tryLockWithWatchdog(String name, long waitTime, TimeUnit unit) throws InterruptedException {
        RLock lock = getLock(name);
        // leaseTime = -1 enables automatic watchdog renewal
        boolean acquired = lock.tryLock(waitTime, -1, unit);
        if (acquired) {
            log.debug("Acquired lock with watchdog: {}", name);
        }
        return acquired;
    }

    /**
     * Release a lock.
     *
     * @param name the lock name
     */
    public void unlock(String name) {
        RLock lock = getLock(name);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("Released lock: {}", name);
        } else {
            log.warn("Attempted to release lock not held by current thread: {}", name);
        }
    }

    /**
     * Check if a lock is currently held.
     *
     * @param name the lock name
     * @return true if the lock is held by any thread
     */
    public boolean isLocked(String name) {
        return getLock(name).isLocked();
    }

    /**
     * Check if the current thread holds the lock.
     *
     * @param name the lock name
     * @return true if the current thread holds the lock
     */
    public boolean isHeldByCurrentThread(String name) {
        return getLock(name).isHeldByCurrentThread();
    }

    /**
     * Force unlock a lock (use with caution).
     *
     * @param name the lock name
     * @return true if the lock was released
     */
    public boolean forceUnlock(String name) {
        boolean unlocked = getLock(name).forceUnlock();
        log.warn("Force unlocked lock: {} (success: {})", name, unlocked);
        return unlocked;
    }

    /**
     * Execute a task within a lock, automatically releasing the lock afterward.
     *
     * @param name the lock name
     * @param waitTime maximum time to wait for the lock
     * @param leaseTime time after which the lock is automatically released
     * @param unit time unit
     * @param task the task to execute
     * @return true if the lock was acquired and task executed, false otherwise
     */
    public boolean executeWithLock(String name, long waitTime, long leaseTime, TimeUnit unit, Runnable task) {
        try {
            if (tryLock(name, waitTime, leaseTime, unit)) {
                try {
                    task.run();
                    return true;
                } finally {
                    unlock(name);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for lock: {}", name);
        }
        return false;
    }
}
