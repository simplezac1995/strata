// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.event;

import dev.strata.ai.api.event.AiChatSessionCreatedEvent;
import dev.strata.ai.api.event.AiModelConfigChangedEvent;
import dev.strata.ai.api.event.AiProviderConfigChangedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * AI 领域事件监听器
 * 监听 AI 相关的领域事件，用于日志记录、指标统计、缓存更新等
 *
 * 可以通过 @Async 注解实现异步处理，避免阻塞主业务流程
 */
@Component
public class AiDomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(AiDomainEventListener.class);

    /** Cache name for AI model configurations. */
    private static final String MODEL_CACHE_NAME = "ai-models";

    /** Cache name for AI provider configurations. */
    private static final String PROVIDER_CACHE_NAME = "ai-providers";

    private final Counter chatSessionCreatedCounter;
    private final Counter modelConfigChangedCounter;
    private final Counter providerConfigChangedCounter;
    private final CacheManager cacheManager;

    /**
     * Micrometer and Spring Cache are optional for the AI add-on: a consumer without actuator/cache still gets the
     * counters (simple registry) and the cache evictions are skipped instead of failing the context.
     */
    @Autowired
    public AiDomainEventListener(ObjectProvider<MeterRegistry> meters, ObjectProvider<CacheManager> caches) {
        this(meters.getIfAvailable(SimpleMeterRegistry::new), caches.getIfAvailable());
    }

    public AiDomainEventListener(MeterRegistry meterRegistry, CacheManager cacheManager) {
        this.chatSessionCreatedCounter = meterRegistry.counter("strata.ai.chat.session.created");
        this.modelConfigChangedCounter = meterRegistry.counter("strata.ai.model.config.changed");
        this.providerConfigChangedCounter = meterRegistry.counter("strata.ai.provider.config.changed");
        this.cacheManager = cacheManager;
    }

    /**
     * 监听对话会话创建事件
     * 异步处理，用于统计和通知
     */
    @Async
    @EventListener
    public void onChatSessionCreated(AiChatSessionCreatedEvent event) {
        log.info(
                "AI chat session created: sessionId={}, userId={}, title={}",
                event.getSessionId(),
                event.getUserId(),
                event.getTitle());
        chatSessionCreatedCounter.increment();
    }

    /**
     * 监听模型配置变更事件
     * 刷新模型缓存、通知相关服务
     */
    @EventListener
    public void onModelConfigChanged(AiModelConfigChangedEvent event) {
        log.info(
                "AI model config changed: modelId={}, modelCode={}, changeType={}",
                event.getModelId(),
                event.getModelCode(),
                event.getChangeType());
        modelConfigChangedCounter.increment();

        // 刷新模型缓存
        evictCache(MODEL_CACHE_NAME, event.getModelId(), "model");
    }

    /**
     * 监听提供商配置变更事件
     * 刷新提供商缓存、重新测试连接
     */
    @EventListener
    public void onProviderConfigChanged(AiProviderConfigChangedEvent event) {
        log.info(
                "AI provider config changed: providerId={}, providerCode={}, changeType={}",
                event.getProviderId(),
                event.getProviderCode(),
                event.getChangeType());
        providerConfigChangedCounter.increment();

        // 刷新提供商缓存
        evictCache(PROVIDER_CACHE_NAME, event.getProviderId(), "provider");
    }

    /**
     * 清除指定缓存中的条目。
     * 如果缓存不存在或条目不存在，静默跳过。
     *
     * @param cacheName 缓存名称
     * @param key 缓存键（实体 ID）
     * @param entityType 实体类型（用于日志）
     */
    private void evictCache(String cacheName, Object key, String entityType) {
        if (cacheManager == null) {
            return;
        }
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            if (key != null) {
                cache.evict(key);
                log.debug("Evicted {} cache entry: key={}", entityType, key);
            }
            // Also clear the list cache to ensure consistency
            cache.evict("all");
            cache.evict("list");
            log.debug("Cleared {} list cache", entityType);
        } else {
            log.debug("Cache '{}' not found, skipping eviction", cacheName);
        }
    }
}
