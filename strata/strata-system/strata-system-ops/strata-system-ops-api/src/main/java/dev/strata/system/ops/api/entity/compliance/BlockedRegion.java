// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity.compliance;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Country-level blocked region row ({@code blocked_region}). */
@Entity
@Table(
        name = "blocked_region",
        indexes = {@Index(name = "idx_br_country_code", columnList = "country_code", unique = true)})
public class BlockedRegion extends TimestampedEntity {

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "country_name", nullable = false, length = 128)
    private String countryName;

    @Column(name = "block_registration", nullable = false)
    private boolean blockRegistration;

    @Column(name = "block_login", nullable = false)
    private boolean blockLogin;

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public boolean isBlockRegistration() {
        return blockRegistration;
    }

    public void setBlockRegistration(boolean blockRegistration) {
        this.blockRegistration = blockRegistration;
    }

    public boolean isBlockLogin() {
        return blockLogin;
    }

    public void setBlockLogin(boolean blockLogin) {
        this.blockLogin = blockLogin;
    }
}
