// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "strata.tenant")
public class TenantProperties {

    /** When true, APIs scope data by {@code X-Tenant-Id} header (Pro row-level isolation). */
    private boolean rowLevelEnabled = false;

    public boolean isRowLevelEnabled() {
        return rowLevelEnabled;
    }

    public void setRowLevelEnabled(boolean rowLevelEnabled) {
        this.rowLevelEnabled = rowLevelEnabled;
    }
}
