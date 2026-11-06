// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.chat.biz.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables the chat-biz configuration properties ({@link AiChatProperties}).
 *
 * <p>Kept in its own {@code config} package so the properties bean is picked up regardless of which scan path the
 * app uses, mirroring how the casino modules register their {@code @ConfigurationProperties} records.
 */
@Configuration
@EnableConfigurationProperties(AiChatProperties.class)
public class AiChatConfiguration {}
