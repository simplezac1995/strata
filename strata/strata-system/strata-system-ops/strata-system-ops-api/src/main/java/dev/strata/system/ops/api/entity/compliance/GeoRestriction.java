// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity.compliance;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** IP-range geo restriction row ({@code geo_restriction}). */
@Entity
@Table(
        name = "geo_restriction",
        indexes = {
            @Index(name = "idx_gr_ip_range", columnList = "ip_range_start, ip_range_end"),
            @Index(name = "idx_gr_country_code", columnList = "country_code"),
            @Index(name = "idx_gr_enabled", columnList = "enabled")
        })
public class GeoRestriction extends TimestampedEntity {

    @Column(name = "ip_range_start", nullable = false, length = 64)
    private String ipRangeStart;

    @Column(name = "ip_range_end", nullable = false, length = 64)
    private String ipRangeEnd;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    /** block | warn. */
    @Column(nullable = false, length = 16)
    private String action = "block";

    @Column(length = 512)
    private String reason;

    @Column(nullable = false)
    private boolean enabled = true;

    public String getIpRangeStart() {
        return ipRangeStart;
    }

    public void setIpRangeStart(String ipRangeStart) {
        this.ipRangeStart = ipRangeStart;
    }

    public String getIpRangeEnd() {
        return ipRangeEnd;
    }

    public void setIpRangeEnd(String ipRangeEnd) {
        this.ipRangeEnd = ipRangeEnd;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
