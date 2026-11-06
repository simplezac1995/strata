// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(basePackages = "dev.strata.common.core")
public class CommonCoreAutoConfiguration {}
