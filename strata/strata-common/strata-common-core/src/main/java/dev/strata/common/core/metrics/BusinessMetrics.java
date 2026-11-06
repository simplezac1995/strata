// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Global business metrics for Strata Admin.
 *
 * <p>Tracks cross-cutting business metrics across all modules:
 * <ul>
 *   <li>API request counts and error rates</li>
 *   <li>Cache hit/miss rates</li>
 *   <li>AI invocation counts and token usage</li>
 *   <li>File upload/download counts</li>
 *   <li>Email/SMS notification counts</li>
 * </ul>
 *
 * <p>All metrics use the {@code strata.business.*} namespace.
 */
@Component
public class BusinessMetrics {

    // API metrics
    private final Counter apiRequestCounter;
    private final Counter apiErrorCounter;
    private final Counter apiSuccessCounter;
    private final Timer apiRequestTimer;

    // Cache metrics
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter cacheEvictCounter;
    private final Counter cachePutCounter;

    // AI metrics
    private final Counter aiInvocationCounter;
    private final Counter aiSuccessCounter;
    private final Counter aiFailureCounter;
    private final DistributionSummary aiTokenUsageSummary;
    private final Timer aiInvocationTimer;

    // File metrics
    private final Counter fileUploadCounter;
    private final Counter fileDownloadCounter;
    private final Counter fileDeleteCounter;
    private final DistributionSummary fileSizeSummary;

    // Notification metrics
    private final Counter emailSentCounter;
    private final Counter emailFailedCounter;
    private final Counter smsSentCounter;
    private final Counter smsFailedCounter;

    // Security metrics
    private final Counter passwordResetCounter;
    private final Counter accountLockedCounter;
    private final Counter permissionDeniedCounter;

    /**
     * Micrometer is optional: a bare starter consumer without an actuator registry still gets working counters,
     * backed by a plain {@link SimpleMeterRegistry} instead of failing the context.
     */
    @Autowired
    public BusinessMetrics(ObjectProvider<MeterRegistry> registryProvider) {
        this(registryProvider.getIfAvailable(SimpleMeterRegistry::new));
    }

    public BusinessMetrics(MeterRegistry registry) {
        // API metrics
        this.apiRequestCounter = Counter.builder("strata.business.api.requests.total")
                .description("Total API requests")
                .register(registry);
        this.apiSuccessCounter = Counter.builder("strata.business.api.requests.success")
                .description("Successful API requests")
                .register(registry);
        this.apiErrorCounter = Counter.builder("strata.business.api.requests.error")
                .description("Failed API requests")
                .register(registry);
        this.apiRequestTimer = Timer.builder("strata.business.api.request.duration")
                .description("API request duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        // Cache metrics
        this.cacheHitCounter = Counter.builder("strata.business.cache.hits")
                .description("Cache hits")
                .register(registry);
        this.cacheMissCounter = Counter.builder("strata.business.cache.misses")
                .description("Cache misses")
                .register(registry);
        this.cacheEvictCounter = Counter.builder("strata.business.cache.evictions")
                .description("Cache evictions")
                .register(registry);
        this.cachePutCounter = Counter.builder("strata.business.cache.puts")
                .description("Cache puts")
                .register(registry);

        // AI metrics
        this.aiInvocationCounter = Counter.builder("strata.business.ai.invocations.total")
                .description("Total AI invocations")
                .register(registry);
        this.aiSuccessCounter = Counter.builder("strata.business.ai.invocations.success")
                .description("Successful AI invocations")
                .register(registry);
        this.aiFailureCounter = Counter.builder("strata.business.ai.invocations.failure")
                .description("Failed AI invocations")
                .register(registry);
        this.aiTokenUsageSummary = DistributionSummary.builder("strata.business.ai.tokens.usage")
                .description("AI token usage per invocation")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
        this.aiInvocationTimer = Timer.builder("strata.business.ai.invocation.duration")
                .description("AI invocation duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        // File metrics
        this.fileUploadCounter = Counter.builder("strata.business.files.uploads")
                .description("File uploads")
                .register(registry);
        this.fileDownloadCounter = Counter.builder("strata.business.files.downloads")
                .description("File downloads")
                .register(registry);
        this.fileDeleteCounter = Counter.builder("strata.business.files.deletions")
                .description("File deletions")
                .register(registry);
        this.fileSizeSummary = DistributionSummary.builder("strata.business.files.size")
                .description("File size in bytes")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        // Notification metrics
        this.emailSentCounter = Counter.builder("strata.business.notifications.email.sent")
                .description("Emails sent")
                .register(registry);
        this.emailFailedCounter = Counter.builder("strata.business.notifications.email.failed")
                .description("Emails failed")
                .register(registry);
        this.smsSentCounter = Counter.builder("strata.business.notifications.sms.sent")
                .description("SMS sent")
                .register(registry);
        this.smsFailedCounter = Counter.builder("strata.business.notifications.sms.failed")
                .description("SMS failed")
                .register(registry);

        // Security metrics
        this.passwordResetCounter = Counter.builder("strata.business.security.password.resets")
                .description("Password resets")
                .register(registry);
        this.accountLockedCounter = Counter.builder("strata.business.security.account.locks")
                .description("Account locks")
                .register(registry);
        this.permissionDeniedCounter = Counter.builder("strata.business.security.permission.denied")
                .description("Permission denied events")
                .register(registry);
    }

    // === API metrics ===

    public void recordApiRequest() {
        apiRequestCounter.increment();
    }

    public void recordApiSuccess() {
        apiSuccessCounter.increment();
    }

    public void recordApiError() {
        apiErrorCounter.increment();
    }

    public Timer.Sample startApiTimer() {
        return Timer.start();
    }

    public void stopApiTimer(Timer.Sample sample) {
        sample.stop(apiRequestTimer);
    }

    // === Cache metrics ===

    public void recordCacheHit() {
        cacheHitCounter.increment();
    }

    public void recordCacheMiss() {
        cacheMissCounter.increment();
    }

    public void recordCacheEvict() {
        cacheEvictCounter.increment();
    }

    public void recordCachePut() {
        cachePutCounter.increment();
    }

    public double getCacheHitRate() {
        double hits = cacheHitCounter.count();
        double misses = cacheMissCounter.count();
        double total = hits + misses;
        return total > 0 ? hits / total : 0.0;
    }

    // === AI metrics ===

    public void recordAiInvocation() {
        aiInvocationCounter.increment();
    }

    public void recordAiSuccess() {
        aiSuccessCounter.increment();
    }

    public void recordAiFailure() {
        aiFailureCounter.increment();
    }

    public void recordAiTokenUsage(long tokens) {
        aiTokenUsageSummary.record(tokens);
    }

    public Timer.Sample startAiTimer() {
        return Timer.start();
    }

    public void stopAiTimer(Timer.Sample sample) {
        sample.stop(aiInvocationTimer);
    }

    // === File metrics ===

    public void recordFileUpload() {
        fileUploadCounter.increment();
    }

    public void recordFileDownload() {
        fileDownloadCounter.increment();
    }

    public void recordFileDelete() {
        fileDeleteCounter.increment();
    }

    public void recordFileSize(long bytes) {
        fileSizeSummary.record(bytes);
    }

    // === Notification metrics ===

    public void recordEmailSent() {
        emailSentCounter.increment();
    }

    public void recordEmailFailed() {
        emailFailedCounter.increment();
    }

    public void recordSmsSent() {
        smsSentCounter.increment();
    }

    public void recordSmsFailed() {
        smsFailedCounter.increment();
    }

    // === Security metrics ===

    public void recordPasswordReset() {
        passwordResetCounter.increment();
    }

    public void recordAccountLocked() {
        accountLockedCounter.increment();
    }

    public void recordPermissionDenied() {
        permissionDeniedCounter.increment();
    }
}
