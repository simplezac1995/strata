// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Business metrics for the Ops subdomain.
 *
 * <p>Tracks operation logs, scheduled tasks, and system monitoring events.
 */
@Component
public class OpsMetrics {

    private final Counter operationLogRecordedCounter;
    private final Counter loginLogRecordedCounter;
    private final Counter scheduledTaskExecutedCounter;
    private final Counter scheduledTaskFailedCounter;

    public OpsMetrics(MeterRegistry registry) {
        this.operationLogRecordedCounter = Counter.builder("strata.system.ops.operation.log.recorded")
                .description("Operation log entries recorded")
                .register(registry);
        this.loginLogRecordedCounter = Counter.builder("strata.system.ops.login.log.recorded")
                .description("Login log entries recorded")
                .register(registry);
        this.scheduledTaskExecutedCounter = Counter.builder("strata.system.ops.scheduled.task.executed")
                .description("Scheduled tasks executed")
                .register(registry);
        this.scheduledTaskFailedCounter = Counter.builder("strata.system.ops.scheduled.task.failed")
                .description("Scheduled tasks failed")
                .register(registry);
    }

    public void recordOperationLog() {
        operationLogRecordedCounter.increment();
    }

    public void recordLoginLog() {
        loginLogRecordedCounter.increment();
    }

    public void recordScheduledTaskExecuted() {
        scheduledTaskExecutedCounter.increment();
    }

    public void recordScheduledTaskFailed() {
        scheduledTaskFailedCounter.increment();
    }
}
