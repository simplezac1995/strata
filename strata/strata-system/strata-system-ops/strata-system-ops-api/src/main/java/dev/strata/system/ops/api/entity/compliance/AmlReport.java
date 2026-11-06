// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity.compliance;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigInteger;

/** AML report row ({@code aml_report}). */
@Entity
@Table(
        name = "aml_report",
        indexes = {
            @Index(name = "idx_ar_user_id", columnList = "user_id"),
            @Index(name = "idx_ar_status", columnList = "status"),
            @Index(name = "idx_ar_report_type", columnList = "report_type")
        })
public class AmlReport extends TimestampedEntity {

    @Column(name = "user_id")
    private Long userId;

    /** large_transaction | suspicious. */
    @Column(name = "report_type", nullable = false, length = 32)
    private String reportType;

    @Column(name = "amount_minor")
    private BigInteger amountMinor;

    @Column(length = 1024)
    private String description;

    /** pending | reviewed | cleared | flagged. */
    @Column(nullable = false, length = 16)
    private String status = "pending";

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public BigInteger getAmountMinor() {
        return amountMinor;
    }

    public void setAmountMinor(BigInteger amountMinor) {
        this.amountMinor = amountMinor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
