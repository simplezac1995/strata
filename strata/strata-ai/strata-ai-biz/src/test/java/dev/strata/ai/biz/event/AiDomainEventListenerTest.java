// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.event;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.ai.api.event.AiChatSessionCreatedEvent;
import dev.strata.ai.api.event.AiModelConfigChangedEvent;
import dev.strata.ai.api.event.AiModelConfigChangedEvent.ChangeType;
import dev.strata.ai.api.event.AiProviderConfigChangedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * {@link AiDomainEventListener} 纯 Mockito 单测：覆盖三类领域事件分发、缓存驱逐的成功/缺失/空键分支。
 */
@ExtendWith(MockitoExtension.class)
class AiDomainEventListenerTest {

    private static final String MODEL_CACHE = "ai-models";
    private static final String PROVIDER_CACHE = "ai-providers";

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Counter chatCounter;

    @Mock
    private Counter modelCounter;

    @Mock
    private Counter providerCounter;

    private AiDomainEventListener listener;

    @BeforeEach
    void setUp() {
        lenient().when(meterRegistry.counter("strata.ai.chat.session.created")).thenReturn(chatCounter);
        lenient().when(meterRegistry.counter("strata.ai.model.config.changed")).thenReturn(modelCounter);
        lenient()
                .when(meterRegistry.counter("strata.ai.provider.config.changed"))
                .thenReturn(providerCounter);
        listener = new AiDomainEventListener(meterRegistry, cacheManager);
    }

    @Nested
    class ChatSession {

        @Test
        void incrementsCounter() {
            listener.onChatSessionCreated(new AiChatSessionCreatedEvent("sess-1", "user-9", "标题"));

            verify(chatCounter).increment();
        }
    }

    @Nested
    class ModelConfig {

        @Test
        void evictsEntryAndListCaches() {
            Cache cache = mock(Cache.class);
            when(cacheManager.getCache(MODEL_CACHE)).thenReturn(cache);

            listener.onModelConfigChanged(new AiModelConfigChangedEvent("model-1", "gpt", ChangeType.UPDATED));

            verify(modelCounter).increment();
            verify(cache).evict("model-1");
            verify(cache).evict("all");
            verify(cache).evict("list");
        }

        @Test
        void nullKeySkipsEntryButClearsList() {
            Cache cache = mock(Cache.class);
            when(cacheManager.getCache(MODEL_CACHE)).thenReturn(cache);

            listener.onModelConfigChanged(new AiModelConfigChangedEvent(null, "gpt", ChangeType.CREATED));

            verify(cache).evict("all");
            verify(cache).evict("list");
            // 键为 null 时不调用 evict(key)
            org.mockito.Mockito.verify(cache, org.mockito.Mockito.never()).evict((Object) null);
        }

        @Test
        void missingCacheIsNoOp() {
            when(cacheManager.getCache(MODEL_CACHE)).thenReturn(null);

            listener.onModelConfigChanged(new AiModelConfigChangedEvent("model-2", "gpt", ChangeType.DELETED));

            verify(modelCounter).increment();
        }
    }

    @Nested
    class ProviderConfig {

        @Test
        void evictsProviderCaches() {
            Cache cache = mock(Cache.class);
            when(cacheManager.getCache(PROVIDER_CACHE)).thenReturn(cache);

            listener.onProviderConfigChanged(new AiProviderConfigChangedEvent(
                    "prov-1", "openai", AiProviderConfigChangedEvent.ChangeType.CREATED));

            verify(providerCounter).increment();
            verify(cache).evict("prov-1");
            verify(cache).evict("all");
            verify(cache).evict("list");
        }

        @Test
        void missingProviderCacheIsNoOp() {
            when(cacheManager.getCache(PROVIDER_CACHE)).thenReturn(null);

            listener.onProviderConfigChanged(new AiProviderConfigChangedEvent(
                    "prov-2", "openai", AiProviderConfigChangedEvent.ChangeType.UPDATED));

            verify(providerCounter).increment();
        }
    }
}
