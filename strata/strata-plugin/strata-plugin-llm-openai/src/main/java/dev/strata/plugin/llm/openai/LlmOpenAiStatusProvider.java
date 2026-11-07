// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.llm.openai;

import dev.strata.plugin.api.llm.LlmGateway;
import dev.strata.plugin.api.status.PluginStatusProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Self-declares the {@code llmOpenAi} status key (ADR-0020).
 *
 * <p>Registered while the OpenAI-compatible LLM module is enabled — mirroring {@link
 * LlmOpenAiAutoConfiguration} (default on, {@code matchIfMissing}) — so the key is omitted from
 * {@code /plugins/status} only when the switch is explicitly off. Availability tracks the {@link
 * LlmGateway} bean the module provides; per-request provider configuration (base URL/API key) is
 * validated at call time by the gateway itself.
 */
@Component
@Order(30)
@ConditionalOnProperty(
        prefix = "strata.plugin.llm.openai-compatible",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class LlmOpenAiStatusProvider implements PluginStatusProvider {

    private final ObjectProvider<LlmGateway> llmGateway;

    public LlmOpenAiStatusProvider(ObjectProvider<LlmGateway> llmGateway) {
        this.llmGateway = llmGateway;
    }

    @Override
    public String key() {
        return "llmOpenAi";
    }

    @Override
    public String label() {
        return "OpenAI-compatible LLM";
    }

    @Override
    public boolean available() {
        return llmGateway.getIfAvailable() != null;
    }

    @Override
    public String detail() {
        return "OpenAI-compatible /chat/completions gateway";
    }
}
