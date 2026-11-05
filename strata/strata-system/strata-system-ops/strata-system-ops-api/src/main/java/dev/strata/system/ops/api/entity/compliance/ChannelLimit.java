// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity.compliance;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.BigInteger;

/** Deposit/withdraw channel limit row ({@code channel_limit}). */
@Entity
@Table(
        name = "channel_limit",
        indexes = {
            @Index(name = "idx_cl_channel_direction", columnList = "channel, direction", unique = true),
            @Index(name = "idx_cl_enabled", columnList = "enabled")
        })
public class ChannelLimit extends TimestampedEntity {

    /** tron | bsc. */
    @Column(nullable = false, length = 16)
    private String channel;

    /** deposit | withdraw. */
    @Column(nullable = false, length = 16)
    private String direction;

    @Column(name = "min_amount_minor")
    private BigInteger minAmountMinor;

    @Column(name = "max_amount_minor")
    private BigInteger maxAmountMinor;

    @Column(name = "daily_max_minor")
    private BigInteger dailyMaxMinor;

    @Column(name = "fee_percent", precision = 12, scale = 6)
    private BigDecimal feePercent;

    @Column(nullable = false)
    private boolean enabled = true;

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public BigInteger getMinAmountMinor() {
        return minAmountMinor;
    }

    public void setMinAmountMinor(BigInteger minAmountMinor) {
        this.minAmountMinor = minAmountMinor;
    }

    public BigInteger getMaxAmountMinor() {
        return maxAmountMinor;
    }

    public void setMaxAmountMinor(BigInteger maxAmountMinor) {
        this.maxAmountMinor = maxAmountMinor;
    }

    public BigInteger getDailyMaxMinor() {
        return dailyMaxMinor;
    }

    public void setDailyMaxMinor(BigInteger dailyMaxMinor) {
        this.dailyMaxMinor = dailyMaxMinor;
    }

    public BigDecimal getFeePercent() {
        return feePercent;
    }

    public void setFeePercent(BigDecimal feePercent) {
        this.feePercent = feePercent;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
