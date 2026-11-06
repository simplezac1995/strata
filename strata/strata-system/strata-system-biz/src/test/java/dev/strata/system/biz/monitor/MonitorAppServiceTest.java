// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.monitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.zaxxer.hikari.HikariDataSource;
import dev.strata.system.api.dto.MonitorSnapshotDto;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonitorAppService 单元测试")
class MonitorAppServiceTest {

    @Mock
    private ObjectProvider<DataSource> dataSourceProvider;

    @Test
    @DisplayName("无 DataSource 时仍返回 JVM 指标，pool 为 null")
    void snapshot_NoDataSource_JvmMetricsPresent() {
        when(dataSourceProvider.getIfAvailable()).thenReturn(null);
        MonitorAppService service = new MonitorAppService(dataSourceProvider);

        MonitorSnapshotDto snapshot = service.snapshot();

        assertThat(snapshot.heap()).isNotNull();
        assertThat(snapshot.heap().max()).isPositive();
        assertThat(snapshot.nonHeap()).isNotNull();
        assertThat(snapshot.gc()).isNotEmpty();
        assertThat(snapshot.threads()).isNotNull();
        assertThat(snapshot.threads().current()).isPositive();
        assertThat(snapshot.pool()).isNull();
        // TODO: recent responses collector not wired yet.
        assertThat(snapshot.recentResponses()).isEmpty();
    }

    @Test
    @DisplayName("DataSource 非 HikariCP 时 pool 为 null 不抛异常")
    void snapshot_NonHikariDataSource_PoolNull() {
        DataSource proxy = new javax.sql.DataSource() {
            @Override
            public java.io.PrintWriter getLogWriter() {
                return null;
            }

            @Override
            public void setLogWriter(java.io.PrintWriter out) {}

            @Override
            public void setLoginTimeout(int seconds) {}

            @Override
            public int getLoginTimeout() {
                return 0;
            }

            @Override
            public java.util.logging.Logger getParentLogger() {
                return java.util.logging.Logger.getGlobal();
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws java.sql.SQLException {
                throw new java.sql.SQLException("not a hikari");
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) {
                return false;
            }

            @Override
            public java.sql.Connection getConnection() {
                return null;
            }

            @Override
            public java.sql.Connection getConnection(String username, String password) {
                return null;
            }
        };
        when(dataSourceProvider.getIfAvailable()).thenReturn(proxy);
        MonitorAppService service = new MonitorAppService(dataSourceProvider);

        MonitorSnapshotDto snapshot = service.snapshot();

        assertThat(snapshot.pool()).isNull();
        assertThat(snapshot.heap()).isNotNull();
    }

    @Test
    @DisplayName("HikariCP DataSource 读取连接池指标")
    void snapshot_HikariDataSource_PoolPopulated() {
        HikariDataSource hikari = new HikariDataSource();
        hikari.setPoolName("test-pool");
        hikari.setMaximumPoolSize(10);
        hikari.setMinimumIdle(2);
        when(dataSourceProvider.getIfAvailable()).thenReturn(hikari);
        MonitorAppService service = new MonitorAppService(dataSourceProvider);

        MonitorSnapshotDto snapshot = service.snapshot();

        try {
            assertThat(snapshot.pool()).isNotNull();
            assertThat(snapshot.pool().poolName()).isEqualTo("test-pool");
            assertThat(snapshot.pool().max()).isEqualTo(10);
            assertThat(snapshot.pool().minIdle()).isEqualTo(2);
        } finally {
            hikari.close();
        }
    }
}
