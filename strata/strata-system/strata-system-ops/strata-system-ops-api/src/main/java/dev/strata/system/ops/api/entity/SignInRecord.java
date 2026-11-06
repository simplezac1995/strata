// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One player's daily sign-in record.
 *
 * <p>{@code (user_id, sign_date)} is logically unique: a player signs at most once per day.
 */
@Entity
@Table(
        name = "sign_in_record",
        indexes = {
            @Index(name = "idx_signin_user_date", columnList = "user_id, sign_date"),
            @Index(name = "idx_signin_date", columnList = "sign_date")
        })
public class SignInRecord extends TimestampedEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sign_date", nullable = false)
    private LocalDate signDate;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column(name = "reward_points", nullable = false)
    private long rewardPoints;

    @Column(name = "reward_minor", nullable = false, precision = 38, scale = 0)
    private BigDecimal rewardMinor = BigDecimal.ZERO;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getSignDate() {
        return signDate;
    }

    public void setSignDate(LocalDate signDate) {
        this.signDate = signDate;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
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
