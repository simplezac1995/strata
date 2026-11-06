// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import dev.strata.system.api.dto.OpsHealthDto;
import dev.strata.system.api.dto.OpsHealthDto.ComponentHealth;
import dev.strata.system.api.dto.OpsHealthDto.CpuHealth;
import dev.strata.system.api.dto.OpsHealthDto.DiskHealth;
import dev.strata.system.api.dto.OpsHealthDto.MemoryHealth;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Real-time system health probe, hand-rolled from DataSource / RedisTemplate / JDK MXBeans (the same
 * approach as {@code CacheMonitorAppService} rather than pulling in actuator at this layer).
 */
@Service
public class OpsHealthAppService {

    private static final Logger log = LoggerFactory.getLogger(OpsHealthAppService.class);
    private static final long BYTES_PER_MB = 1024L * 1024L;
    private static final double BYTES_PER_GB = 1024d * 1024d * 1024d;

    private final ObjectProvider<DataSource> dataSourceProvider;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    public OpsHealthAppService(
            ObjectProvider<DataSource> dataSourceProvider, ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.dataSourceProvider = dataSourceProvider;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    @Transactional(readOnly = true)
    public OpsHealthDto check() {
        return new OpsHealthDto(
                probeDb(),
                probeRedis(),
                disk(),
                cpu(),
                memory(),
                ManagementFactory.getRuntimeMXBean().getUptime());
    }

    private ComponentHealth probeDb() {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            return new ComponentHealth("unknown", 0L);
        }
        long start = System.nanoTime();
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            return new ComponentHealth(valid ? "up" : "down", elapsedMs(start));
        } catch (Exception ex) {
            log.debug("health db probe failed: {}", ex.getMessage());
            return new ComponentHealth("down", elapsedMs(start));
        }
    }

    private ComponentHealth probeRedis() {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return new ComponentHealth("unknown", 0L);
        }
        RedisConnectionFactory factory = redis.getConnectionFactory();
        if (factory == null) {
            return new ComponentHealth("unknown", 0L);
        }
        long start = System.nanoTime();
        try {
            factory.getConnection().ping();
            return new ComponentHealth("up", elapsedMs(start));
        } catch (Exception ex) {
            log.debug("health redis probe failed: {}", ex.getMessage());
            return new ComponentHealth("down", elapsedMs(start));
        }
    }

    private static DiskHealth disk() {
        File root = new File(".");
        double totalGb = root.getTotalSpace() / BYTES_PER_GB;
        double freeGb = root.getUsableSpace() / BYTES_PER_GB;
        double usedGb = totalGb - freeGb;
        return new DiskHealth(totalGb, usedGb, freeGb);
    }

    private static CpuHealth cpu() {
        double load = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        if (load < 0) {
            load = 0d;
        }
        int processors = Runtime.getRuntime().availableProcessors();
        double percent = processors > 0 ? load / processors * 100d : 0d;
        return new CpuHealth(Math.min(100d, Math.max(0d, percent)));
    }

    private static MemoryHealth memory() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long total = runtime.totalMemory();
        double percent = total > 0 ? used * 100d / total : 0d;
        return new MemoryHealth(used / BYTES_PER_MB, total / BYTES_PER_MB, percent);
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
