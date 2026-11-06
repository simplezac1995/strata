// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Risk control rule row ({@code risk_rule}). */
@Entity
@Table(
        name = "risk_rule",
        indexes = {
            @Index(name = "idx_rr_type", columnList = "type"),
            @Index(name = "idx_rr_enabled", columnList = "enabled")
        })
public class RiskRule extends TimestampedEntity {

    @Column(nullable = false, length = 128)
    private String name;

    /** ip_blacklist | device | amount | frequency. */
    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false)
    private int threshold;

    /** reject | freeze | alert. */
    @Column(nullable = false, length = 16)
    private String action;

    @Column(nullable = false)
    private boolean enabled = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
