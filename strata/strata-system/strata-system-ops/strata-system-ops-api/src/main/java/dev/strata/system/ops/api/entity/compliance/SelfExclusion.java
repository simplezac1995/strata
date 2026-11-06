// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity.compliance;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/** Self-exclusion row ({@code self_exclusion}). */
@Entity
@Table(
        name = "self_exclusion",
        indexes = {
            @Index(name = "idx_se_user_id", columnList = "user_id"),
            @Index(name = "idx_se_end_date", columnList = "end_date")
        })
public class SelfExclusion extends TimestampedEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(length = 512)
    private String reason;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
