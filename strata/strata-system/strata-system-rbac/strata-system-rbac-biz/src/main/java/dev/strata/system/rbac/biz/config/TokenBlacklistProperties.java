// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "strata.security.token")
public record TokenBlacklistProperties(boolean blacklistEnabled) {}
