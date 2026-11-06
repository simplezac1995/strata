// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity.compliance;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigInteger;

/** AML rule row ({@code aml_rule}). */
@Entity
@Table(
        name = "aml_rule",
        indexes = {
            @Index(name = "idx_ar_rule_type", columnList = "rule_type"),
            @Index(name = "idx_ar_enabled", columnList = "enabled")
        })
public class AmlRule extends TimestampedEntity {

    @Column(nullable = false, length = 128)
    private String name;

    /** large_deposit | large_withdraw | velocity | unusual_pattern. */
    @Column(name = "rule_type", nullable = false, length = 32)
    private String ruleType;

    @Column(name = "threshold_minor")
    private BigInteger thresholdMinor;

    /** report | freeze_account. */
    @Column(nullable = false, length = 32)
    private String action = "report";

    @Column(nullable = false)
    private boolean enabled = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public BigInteger getThresholdMinor() {
        return thresholdMinor;
    }

    public void setThresholdMinor(BigInteger thresholdMinor) {
        this.thresholdMinor = thresholdMinor;
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
