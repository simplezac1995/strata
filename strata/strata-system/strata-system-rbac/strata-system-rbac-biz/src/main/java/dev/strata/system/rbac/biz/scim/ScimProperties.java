// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.scim;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SCIM 2.0 provisioning endpoint settings ({@code strata.scim.*}). Disabled by default; enabling it requires a bearer
 * token that the identity provider sends with every request (fail closed when the token is empty).
 */
@ConfigurationProperties(prefix = "strata.scim")
public class ScimProperties {

    private boolean enabled = false;
    private String token = "";
    private int maxResults = 200;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getToken() {
        return token == null ? "" : token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getMaxResults() {
        return maxResults <= 0 ? 200 : maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }
}
