// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity.compliance;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/** Cool-off row ({@code cool_off}). */
@Entity
@Table(
        name = "cool_off",
        indexes = {
            @Index(name = "idx_co_user_id", columnList = "user_id"),
            @Index(name = "idx_co_end_date", columnList = "end_date")
        })
public class CoolOff extends TimestampedEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }
}
