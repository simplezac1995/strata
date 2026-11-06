// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BusinessMetrics}: every recorder increments its registered meter, the cache hit
 * rate ratio handles the empty-data and mixed-data cases, and the timer start/stop cycle works.
 */
@DisplayName("BusinessMetrics 业务指标")
class BusinessMetricsTest {

    @Test
    @DisplayName("无缓存数据时命中率为 0")
    void hitRateIsZeroWhenNoData() {
        BusinessMetrics metrics = new BusinessMetrics(new SimpleMeterRegistry());

        assertThat(metrics.getCacheHitRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("全部 recorder 方法都注册并递增对应 meter")
    void recordersIncrementMeters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BusinessMetrics metrics = new BusinessMetrics(registry);

        metrics.recordApiRequest();
        metrics.recordApiSuccess();
        metrics.recordApiError();
        metrics.recordCacheHit();
        metrics.recordCacheMiss();
        metrics.recordCacheEvict();
        metrics.recordCachePut();
        metrics.recordAiInvocation();
        metrics.recordAiSuccess();
        metrics.recordAiFailure();
        metrics.recordAiTokenUsage(128);
        metrics.recordFileUpload();
        metrics.recordFileDownload();
        metrics.recordFileDelete();
        metrics.recordFileSize(2048);
        metrics.recordEmailSent();
        metrics.recordEmailFailed();
        metrics.recordSmsSent();
        metrics.recordSmsFailed();
        metrics.recordPasswordReset();
        metrics.recordAccountLocked();
        metrics.recordPermissionDenied();

        assertThat(registry.find("strata.business.api.requests.total").counter().count())
                .isEqualTo(1);
        assertThat(registry.find("strata.business.api.requests.success")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(registry.find("strata.business.cache.hits").counter().count())
                .isEqualTo(1);
        assertThat(registry.find("strata.business.cache.misses").counter().count())
                .isEqualTo(1);
        assertThat(registry.find("strata.business.ai.tokens.usage").summary().totalAmount())
                .isEqualTo(128);
        assertThat(registry.find("strata.business.files.size").summary().totalAmount())
                .isEqualTo(2048);
    }

    @Test
    @DisplayName("命中/未命中混合时命中率为命中占比")
    void hitRateIsRatioOfHits() {
        BusinessMetrics metrics = new BusinessMetrics(new SimpleMeterRegistry());

        metrics.recordCacheHit();
        metrics.recordCacheHit();
        metrics.recordCacheMiss();

        assertThat(metrics.getCacheHitRate()).isEqualTo(2.0 / 3.0);
    }

    @Test
    @DisplayName("API 与 AI 计时器 start/stop 记录耗时")
    void timersRecordDuration() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BusinessMetrics metrics = new BusinessMetrics(registry);

        metrics.stopApiTimer(metrics.startApiTimer());
        metrics.stopAiTimer(metrics.startAiTimer());

        assertThat(registry.find("strata.business.api.request.duration").timer().count())
                .isEqualTo(1);
        assertThat(registry.find("strata.business.ai.invocation.duration")
                        .timer()
                        .count())
                .isEqualTo(1);
    }
}
