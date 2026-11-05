// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Business metrics for the Config subdomain.
 *
 * <p>Tracks dictionary, parameter, region, and webhook configuration changes.
 */
@Component
public class ConfigMetrics {

    private final Counter dictTypeCreatedCounter;
    private final Counter dictTypeUpdatedCounter;
    private final Counter dictTypeDeletedCounter;
    private final Counter dictDataCreatedCounter;
    private final Counter dictDataUpdatedCounter;
    private final Counter dictDataDeletedCounter;
    private final Counter paramUpdatedCounter;
    private final Counter webhookCreatedCounter;
    private final Counter webhookUpdatedCounter;
    private final Counter webhookDeletedCounter;

    public ConfigMetrics(MeterRegistry registry) {
        this.dictTypeCreatedCounter = Counter.builder("strata.system.config.dict.type.created")
                .description("Dictionary types created")
                .register(registry);
        this.dictTypeUpdatedCounter = Counter.builder("strata.system.config.dict.type.updated")
                .description("Dictionary types updated")
                .register(registry);
        this.dictTypeDeletedCounter = Counter.builder("strata.system.config.dict.type.deleted")
                .description("Dictionary types deleted")
                .register(registry);
        this.dictDataCreatedCounter = Counter.builder("strata.system.config.dict.data.created")
                .description("Dictionary data entries created")
                .register(registry);
        this.dictDataUpdatedCounter = Counter.builder("strata.system.config.dict.data.updated")
                .description("Dictionary data entries updated")
                .register(registry);
        this.dictDataDeletedCounter = Counter.builder("strata.system.config.dict.data.deleted")
                .description("Dictionary data entries deleted")
                .register(registry);
        this.paramUpdatedCounter = Counter.builder("strata.system.config.param.updated")
                .description("System parameters updated")
                .register(registry);
        this.webhookCreatedCounter = Counter.builder("strata.system.config.webhook.created")
                .description("Webhooks created")
                .register(registry);
        this.webhookUpdatedCounter = Counter.builder("strata.system.config.webhook.updated")
                .description("Webhooks updated")
                .register(registry);
        this.webhookDeletedCounter = Counter.builder("strata.system.config.webhook.deleted")
                .description("Webhooks deleted")
                .register(registry);
    }

    public void recordDictTypeCreated() {
        dictTypeCreatedCounter.increment();
    }

    public void recordDictTypeUpdated() {
        dictTypeUpdatedCounter.increment();
    }

    public void recordDictTypeDeleted() {
        dictTypeDeletedCounter.increment();
    }

    public void recordDictDataCreated() {
        dictDataCreatedCounter.increment();
    }

    public void recordDictDataUpdated() {
        dictDataUpdatedCounter.increment();
    }

    public void recordDictDataDeleted() {
        dictDataDeletedCounter.increment();
    }

    public void recordParamUpdated() {
        paramUpdatedCounter.increment();
    }

    public void recordWebhookCreated() {
        webhookCreatedCounter.increment();
    }

    public void recordWebhookUpdated() {
        webhookUpdatedCounter.increment();
    }

    public void recordWebhookDeleted() {
        webhookDeletedCounter.increment();
    }
}
