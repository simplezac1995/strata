// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.monitor;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import dev.strata.system.api.dto.MonitorSnapshotDto;
import dev.strata.system.api.dto.MonitorSnapshotDto.GcDto;
import dev.strata.system.api.dto.MonitorSnapshotDto.MemoryDto;
import dev.strata.system.api.dto.MonitorSnapshotDto.PoolDto;
import dev.strata.system.api.dto.MonitorSnapshotDto.ThreadsDto;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Read-only process/JVM snapshot (#8). Never touches the database or writes state. The HikariCP pool
 * section is best-effort: when the primary {@link DataSource} is a routing wrapper it cannot be unwrapped
 * to a {@link HikariDataSource} and the pool block is reported as {@code null} rather than guessed.
 */
@Service
public class MonitorAppService {

    private static final Logger log = LoggerFactory.getLogger(MonitorAppService.class);

    private final ObjectProvider<DataSource> dataSourceProvider;

    public MonitorAppService(ObjectProvider<DataSource> dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
    }

    public MonitorSnapshotDto snapshot() {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memory.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memory.getNonHeapMemoryUsage();
        MemoryDto heap = new MemoryDto(heapUsage.getUsed(), heapUsage.getCommitted(), heapUsage.getMax());
        MemoryDto nonHeap = new MemoryDto(nonHeapUsage.getUsed(), nonHeapUsage.getCommitted(), nonHeapUsage.getMax());

        List<GcDto> gc = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .map(MonitorAppService::toGcDto)
                .toList();

        ThreadMXBean thread = ManagementFactory.getThreadMXBean();
        ThreadsDto threads = new ThreadsDto(
                thread.getThreadCount(),
                thread.getPeakThreadCount(),
                thread.getDaemonThreadCount(),
                thread.getTotalStartedThreadCount());

        PoolDto pool = poolSnapshot();

        // TODO(system-monitor): no HTTP response-time collector exists yet; return an empty list.
        return new MonitorSnapshotDto(heap, nonHeap, gc, threads, pool, List.of());
    }

    private static GcDto toGcDto(GarbageCollectorMXBean bean) {
        long count = bean.getCollectionCount();
        long time = bean.getCollectionTime();
        return new GcDto(bean.getName(), Math.max(count, 0), Math.max(time, 0));
    }

    private PoolDto poolSnapshot() {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            return null;
        }
        HikariDataSource hikari = unwrapHikari(dataSource);
        if (hikari == null) {
            return null;
        }
        try {
            HikariPoolMXBean mxBean = hikari.getHikariPoolMXBean();
            if (mxBean == null) {
                return new PoolDto(hikari.getPoolName(), 0, 0, 0, hikari.getMaximumPoolSize(), hikari.getMinimumIdle());
            }
            return new PoolDto(
                    hikari.getPoolName(),
                    mxBean.getActiveConnections(),
                    mxBean.getIdleConnections(),
                    mxBean.getThreadsAwaitingConnection(),
                    hikari.getMaximumPoolSize(),
                    hikari.getMinimumIdle());
        } catch (Exception ex) {
            log.debug("HikariCP metrics unavailable: {}", ex.getMessage());
            return null;
        }
    }

    private static HikariDataSource unwrapHikari(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource direct) {
            return direct;
        }
        try {
            return dataSource.unwrap(HikariDataSource.class);
        } catch (Exception ex) {
            return null;
        }
    }
}
