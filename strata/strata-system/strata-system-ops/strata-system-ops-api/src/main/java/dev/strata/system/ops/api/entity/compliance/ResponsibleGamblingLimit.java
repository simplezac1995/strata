// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity.compliance;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigInteger;
import java.time.Instant;

/** Responsible gambling limit row ({@code responsible_gambling_limit}). */
@Entity
@Table(
        name = "responsible_gambling_limit",
        indexes = {
            @Index(name = "idx_rgl_user_id", columnList = "user_id"),
            @Index(name = "idx_rgl_user_type", columnList = "user_id, limit_type")
        })
public class ResponsibleGamblingLimit extends TimestampedEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** deposit | bet | loss. */
    @Column(name = "limit_type", nullable = false, length = 16)
    private String limitType;

    @Column(name = "amount_minor", nullable = false)
    private BigInteger amountMinor;

    /** daily | weekly | monthly. */
    @Column(nullable = false, length = 16)
    private String period;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getLimitType() {
        return limitType;
    }

    public void setLimitType(String limitType) {
        this.limitType = limitType;
    }

    public BigInteger getAmountMinor() {
        return amountMinor;
    }

    public void setAmountMinor(BigInteger amountMinor) {
        this.amountMinor = amountMinor;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }
}
