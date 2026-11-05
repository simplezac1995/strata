// SPDX-License-Identifier: Apache-2.0
package dev.strata.generator;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeneratorProperties.class)
public class GeneratorConfiguration {}
