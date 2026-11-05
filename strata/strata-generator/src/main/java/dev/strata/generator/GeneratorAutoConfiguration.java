// SPDX-License-Identifier: Apache-2.0
package dev.strata.generator;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * Wires the code generator when it is enabled.
 *
 * <p>The generator is a development tool: it is off unless {@code strata.generator.enabled=true}, and the write
 * endpoint is additionally blocked in the prod profile. Consumers that enable it get the controller and its services
 * without touching their component scan.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "strata.generator", name = "enabled", havingValue = "true")
@ComponentScan(basePackages = "dev.strata.generator")
public class GeneratorAutoConfiguration {}
