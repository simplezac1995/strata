// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigMetricsTest {

    private SimpleMeterRegistry registry;
    private ConfigMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new ConfigMetrics(registry);
    }

    @Test
    void recordDictTypeCreatedIncrementsCounter() {
        metrics.recordDictTypeCreated();
        assertThat(registry.counter("strata.system.config.dict.type.created").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordDictTypeUpdatedIncrementsCounter() {
        metrics.recordDictTypeUpdated();
        assertThat(registry.counter("strata.system.config.dict.type.updated").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordDictTypeDeletedIncrementsCounter() {
        metrics.recordDictTypeDeleted();
        assertThat(registry.counter("strata.system.config.dict.type.deleted").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordDictDataCreatedIncrementsCounter() {
        metrics.recordDictDataCreated();
        assertThat(registry.counter("strata.system.config.dict.data.created").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordDictDataUpdatedIncrementsCounter() {
        metrics.recordDictDataUpdated();
        assertThat(registry.counter("strata.system.config.dict.data.updated").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordDictDataDeletedIncrementsCounter() {
        metrics.recordDictDataDeleted();
        assertThat(registry.counter("strata.system.config.dict.data.deleted").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordParamUpdatedIncrementsCounter() {
        metrics.recordParamUpdated();
        assertThat(registry.counter("strata.system.config.param.updated").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordWebhookCreatedIncrementsCounter() {
        metrics.recordWebhookCreated();
        assertThat(registry.counter("strata.system.config.webhook.created").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordWebhookUpdatedIncrementsCounter() {
        metrics.recordWebhookUpdated();
        assertThat(registry.counter("strata.system.config.webhook.updated").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordWebhookDeletedIncrementsCounter() {
        metrics.recordWebhookDeleted();
        assertThat(registry.counter("strata.system.config.webhook.deleted").count())
                .isEqualTo(1.0);
    }
}
