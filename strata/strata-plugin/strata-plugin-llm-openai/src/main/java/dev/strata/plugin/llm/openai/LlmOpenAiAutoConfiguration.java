// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.llm.openai;

import dev.strata.plugin.api.llm.LlmGateway;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(
        prefix = "strata.plugin.llm.openai-compatible",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class LlmOpenAiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LlmGateway.class)
    LlmGateway openAiCompatibleLlmGateway() {
        return new OpenAiCompatibleLlmGateway();
    }
}
