// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.redisson.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Redisson integration.
 *
 * <p>Redisson provides advanced distributed objects and services:
 * <ul>
 *   <li>Distributed locks: Reentrant, Fair, ReadWrite, RedLock, MultiLock</li>
 *   <li>Distributed collections: Map, List, Set, Queue, Deque, BlockingQueue</li>
 *   <li>Distributed objects: ObjectHolder, AtomicLong, AtomicDouble, BinaryStream</li>
 *   <li>Distributed services: RemoteService, LiveObjectService, ExecutorService</li>
 *   <li>Publish/Subscribe: Topic, PatternTopic</li>
 * </ul>
 *
 * <p>Configuration example:
 * <pre>{@code
 * strata:
 *   redisson:
 *     enabled: true
 *     address: redis://localhost:6379
 *     password: secret
 *     database: 0
 *     threads: 16
 *     netty-threads: 32
 *     lock-watchdog-timeout: 30000
 * }</pre>
 */
@ConfigurationProperties(prefix = "strata.redisson")
public class RedissonProperties {

    /**
     * Whether Redisson integration is enabled.
     */
    private boolean enabled = false;

    /**
     * Redis server address (e.g., redis://localhost:6379).
     */
    private String address = "redis://localhost:6379";

    /**
     * Redis password.
     */
    private String password;

    /**
     * Redis database index.
     */
    private int database = 0;

    /**
     * Number of threads used by Redisson for internal operations.
     */
    private int threads = 16;

    /**
     * Number of Netty threads for network operations.
     */
    private int nettyThreads = 32;

    /**
     * Lock watchdog timeout in milliseconds.
     * Redisson automatically extends lock lease while the lock holder is alive.
     */
    private long lockWatchdogTimeout = 30000;

    /**
     * Connection minimum idle size.
     */
    private int connectionMinimumIdleSize = 10;

    /**
     * Connection pool size.
     */
    private int connectionPoolSize = 64;

    /**
     * Connect timeout in milliseconds.
     */
    private int connectTimeout = 10000;

    /**
     * Response timeout in milliseconds.
     */
    private int timeout = 3000;

    /**
     * Retry attempts.
     */
    private int retryAttempts = 3;

    /**
     * Retry interval in milliseconds.
     */
    private int retryInterval = 1500;

    /**
     * Whether to use Redisson distributed lock as the primary lock implementation.
     * If true, replaces the default RedisTemplate-based lock.
     */
    private boolean useAsPrimaryLock = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public int getNettyThreads() {
        return nettyThreads;
    }

    public void setNettyThreads(int nettyThreads) {
        this.nettyThreads = nettyThreads;
    }

    public long getLockWatchdogTimeout() {
        return lockWatchdogTimeout;
    }

    public void setLockWatchdogTimeout(long lockWatchdogTimeout) {
        this.lockWatchdogTimeout = lockWatchdogTimeout;
    }

    public int getConnectionMinimumIdleSize() {
        return connectionMinimumIdleSize;
    }

    public void setConnectionMinimumIdleSize(int connectionMinimumIdleSize) {
        this.connectionMinimumIdleSize = connectionMinimumIdleSize;
    }

    public int getConnectionPoolSize() {
        return connectionPoolSize;
    }

    public void setConnectionPoolSize(int connectionPoolSize) {
        this.connectionPoolSize = connectionPoolSize;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public int getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
    }

    public boolean isUseAsPrimaryLock() {
        return useAsPrimaryLock;
    }

    public void setUseAsPrimaryLock(boolean useAsPrimaryLock) {
        this.useAsPrimaryLock = useAsPrimaryLock;
    }
}
