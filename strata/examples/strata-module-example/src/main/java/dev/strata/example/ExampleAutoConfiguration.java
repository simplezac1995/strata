// SPDX-License-Identifier: Apache-2.0
package dev.strata.example;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * The single entry point of a Strata feature module.
 *
 * <p>The host application depends on this jar and gets the module's beans automatically through the
 * {@code AutoConfiguration.imports} file — no {@code scanBasePackages} edit, no application class change. The
 * {@code strata.example.enabled} switch lets a host disable the module without removing the dependency (useful for
 * integration tests and for editions that ship the jar but not the feature).
 */
@AutoConfiguration
@ComponentScan(basePackageClasses = ExampleAutoConfiguration.class)
@ConditionalOnProperty(prefix = "strata.example", name = "enabled", matchIfMissing = true)
public class ExampleAutoConfiguration {}
