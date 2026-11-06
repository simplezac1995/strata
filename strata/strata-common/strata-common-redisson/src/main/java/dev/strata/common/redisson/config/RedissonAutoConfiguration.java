// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.redisson.config;

import dev.strata.common.redisson.lock.RedissonDistributedLock;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for Redisson integration.
 *
 * <p>Activated when {@code strata.redisson.enabled=true}. Creates a
 * {@link RedissonClient} bean configured with the specified Redis connection
 * settings and performance tuning parameters.
 *
 * <p>Redisson provides:
 * <ul>
 *   <li>Advanced distributed locks (Reentrant, Fair, ReadWrite, RedLock, MultiLock)</li>
 *   <li>Distributed collections (Map, List, Set, Queue, BlockingQueue)</li>
 *   <li>Distributed objects (ObjectHolder, AtomicLong, AtomicDouble)</li>
 *   <li>Publish/Subscribe (Topic, PatternTopic)</li>
 *   <li>Remote service invocation</li>
 * </ul>
 *
 * <p>Configuration:
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
@Configuration
@EnableConfigurationProperties(RedissonProperties.class)
@ConditionalOnProperty(prefix = "strata.redisson", name = "enabled", havingValue = "true")
public class RedissonAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RedissonAutoConfiguration.class);

    private final RedissonProperties properties;

    public RedissonAutoConfiguration(RedissonProperties properties) {
        this.properties = properties;
    }

    /**
     * Create the Redisson client bean.
     *
     * @return the configured RedissonClient
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        log.info(
                "Initializing Redisson client (address: {}, database: {})",
                properties.getAddress(),
                properties.getDatabase());

        Config config = new Config();

        // Single server configuration
        config.useSingleServer()
                .setAddress(properties.getAddress())
                .setDatabase(properties.getDatabase())
                .setConnectionMinimumIdleSize(properties.getConnectionMinimumIdleSize())
                .setConnectionPoolSize(properties.getConnectionPoolSize())
                .setConnectTimeout(properties.getConnectTimeout())
                .setTimeout(properties.getTimeout())
                .setRetryAttempts(properties.getRetryAttempts())
                .setRetryInterval(properties.getRetryInterval());

        // Password (optional)
        if (properties.getPassword() != null && !properties.getPassword().isEmpty()) {
            config.useSingleServer().setPassword(properties.getPassword());
        }

        // Threading
        config.setThreads(properties.getThreads());
        config.setNettyThreads(properties.getNettyThreads());

        // Lock watchdog timeout
        config.setLockWatchdogTimeout(properties.getLockWatchdogTimeout());

        RedissonClient client = Redisson.create(config);

        log.info(
                "Redisson client initialized successfully (threads: {}, netty-threads: {}, "
                        + "lock-watchdog-timeout: {}ms)",
                properties.getThreads(),
                properties.getNettyThreads(),
                properties.getLockWatchdogTimeout());

        return client;
    }

    /**
     * Distributed lock facade built on the {@link RedissonClient}. Containers that run scheduled jobs across
     * instances inject this (typically via {@code ObjectProvider}) and wrap the job body in a tryLock so only one
     * node runs a given job at a time. When Redisson is disabled the bean is absent and the job runs unlocked on a
     * single node — the safe default.
     *
     * @param redissonClient the configured client
     * @return the lock facade
     */
    @Bean
    public RedissonDistributedLock redissonDistributedLock(RedissonClient redissonClient) {
        return new RedissonDistributedLock(redissonClient);
    }
}
