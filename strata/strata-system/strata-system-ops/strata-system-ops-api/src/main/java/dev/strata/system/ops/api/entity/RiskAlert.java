// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Risk alert row ({@code risk_alert}). */
@Entity
@Table(
        name = "risk_alert",
        indexes = {
            @Index(name = "idx_ra_user_id", columnList = "user_id"),
            @Index(name = "idx_ra_status", columnList = "status"),
            @Index(name = "idx_ra_created_at", columnList = "created_at")
        })
public class RiskAlert extends TimestampedEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "device_hash", length = 128)
    private String deviceHash;

    @Column(name = "rule_name", length = 128)
    private String ruleName;

    @Column(length = 1024)
    private String detail;

    /** open | ignored | resolved. */
    @Column(nullable = false, length = 16)
    private String status = "open";

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDeviceHash() {
        return deviceHash;
    }

    public void setDeviceHash(String deviceHash) {
        this.deviceHash = deviceHash;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
