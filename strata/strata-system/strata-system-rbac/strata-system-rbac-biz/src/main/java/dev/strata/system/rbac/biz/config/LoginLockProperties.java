// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "strata.security.login-lock")
public record LoginLockProperties(int maxAttempts, int lockMinutes, String store) {

    public LoginLockProperties {
        if (maxAttempts <= 0) {
            maxAttempts = 5;
        }
        if (lockMinutes <= 0) {
            lockMinutes = 15;
        }
        if (store == null || store.isBlank()) {
            store = "memory";
        }
    }
}
