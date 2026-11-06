// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Per-day reward configuration for the daily sign-in cycle.
 *
 * <p>{@code rewardMinor} is minor units (DECIMAL(38,0)) per ADR-0026; {@code day} is 1-based.
 */
@Entity
@Table(name = "sign_in_config")
public class SignInConfig extends TimestampedEntity {

    @Column(name = "day_number", nullable = false, unique = true)
    private int day;

    @Column(name = "reward_points", nullable = false)
    private long rewardPoints;

    @Column(name = "reward_minor", nullable = false, precision = 38, scale = 0)
    private BigDecimal rewardMinor = BigDecimal.ZERO;

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public long getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(long rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

    public BigDecimal getRewardMinor() {
        return rewardMinor;
    }

    public void setRewardMinor(BigDecimal rewardMinor) {
        this.rewardMinor = rewardMinor;
    }
}
