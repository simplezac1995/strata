// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpsMetricsTest {

    private SimpleMeterRegistry registry;
    private OpsMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new OpsMetrics(registry);
    }

    @Test
    void recordOperationLogIncrementsCounter() {
        metrics.recordOperationLog();
        metrics.recordOperationLog();
        assertThat(registry.counter("strata.system.ops.operation.log.recorded").count())
                .isEqualTo(2.0);
    }

    @Test
    void recordLoginLogIncrementsCounter() {
        metrics.recordLoginLog();
        assertThat(registry.counter("strata.system.ops.login.log.recorded").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordScheduledTaskExecutedIncrementsCounter() {
        metrics.recordScheduledTaskExecuted();
        assertThat(registry.counter("strata.system.ops.scheduled.task.executed").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordScheduledTaskFailedIncrementsCounter() {
        metrics.recordScheduledTaskFailed();
        assertThat(registry.counter("strata.system.ops.scheduled.task.failed").count())
                .isEqualTo(1.0);
    }
}
