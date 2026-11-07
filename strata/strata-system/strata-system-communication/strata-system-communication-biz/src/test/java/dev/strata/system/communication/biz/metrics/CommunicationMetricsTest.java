// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommunicationMetricsTest {

    private SimpleMeterRegistry registry;
    private CommunicationMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new CommunicationMetrics(registry);
    }

    @Test
    void recordEmailSentIncrementsCounter() {
        metrics.recordEmailSent();
        metrics.recordEmailSent();
        assertThat(registry.counter("strata.system.communication.email.sent").count())
                .isEqualTo(2.0);
    }

    @Test
    void recordEmailFailedIncrementsCounter() {
        metrics.recordEmailFailed();
        assertThat(registry.counter("strata.system.communication.email.failed").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordSmsSentIncrementsCounter() {
        metrics.recordSmsSent();
        assertThat(registry.counter("strata.system.communication.sms.sent").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordSmsFailedIncrementsCounter() {
        metrics.recordSmsFailed();
        assertThat(registry.counter("strata.system.communication.sms.failed").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordAnnouncementCreatedIncrementsCounter() {
        metrics.recordAnnouncementCreated();
        assertThat(registry.counter("strata.system.communication.announcement.created")
                        .count())
                .isEqualTo(1.0);
    }

    @Test
    void recordAnnouncementPublishedIncrementsCounter() {
        metrics.recordAnnouncementPublished();
        assertThat(registry.counter("strata.system.communication.announcement.published")
                        .count())
                .isEqualTo(1.0);
    }

    @Test
    void recordNotificationSentIncrementsCounter() {
        metrics.recordNotificationSent();
        assertThat(registry.counter("strata.system.communication.notification.sent")
                        .count())
                .isEqualTo(1.0);
    }

    @Test
    void recordNotificationReadIncrementsCounter() {
        metrics.recordNotificationRead();
        assertThat(registry.counter("strata.system.communication.notification.read")
                        .count())
                .isEqualTo(1.0);
    }
}
