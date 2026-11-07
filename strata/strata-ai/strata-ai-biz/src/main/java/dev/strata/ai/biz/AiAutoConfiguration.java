// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Wires the AI domain for consumers that add the AI modules explicitly.
 *
 * <p>AI is an optional add-on: the core starter does not depend on it. Adding {@code strata-ai-biz} (which brings the
 * chat/knowledge/config biz modules) plus this auto-configuration gives the host the AI controllers, services,
 * entities and repositories without changing its component scan.
 *
 * <p>Gated by the same {@code strata.starter.auto-configuration} switch as the core starter auto-configuration: the
 * distribution application scans {@code dev.strata} itself and turns the switch off, consumers leave it at the
 * default.
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "strata.starter",
        name = "auto-configuration",
        havingValue = "true",
        matchIfMissing = true)
@ComponentScan(basePackages = "dev.strata.ai")
@EntityScan(basePackages = "dev.strata.ai")
@EnableJpaRepositories(basePackages = "dev.strata.ai")
public class AiAutoConfiguration {}
