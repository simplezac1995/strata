// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/** Risk IP blacklist row ({@code risk_ip_blacklist}). */
@Entity
@Table(
        name = "risk_ip_blacklist",
        indexes = {
            @Index(name = "idx_rib_ip_address", columnList = "ip_address", unique = true),
            @Index(name = "idx_rib_expires_at", columnList = "expires_at")
        })
public class RiskIpBlacklist extends TimestampedEntity {

    @Column(name = "ip_address", nullable = false, length = 64)
    private String ipAddress;

    @Column(length = 512)
    private String reason;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
