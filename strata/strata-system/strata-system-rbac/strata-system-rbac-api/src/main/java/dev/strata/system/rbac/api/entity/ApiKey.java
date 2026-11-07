// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(
        name = "sys_api_key",
        indexes = {
            @Index(name = "idx_api_key_key_hash", columnList = "key_hash"),
            @Index(name = "idx_api_key_enabled", columnList = "enabled"),
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiKey extends AuditingEntity {

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "key_prefix", nullable = false, length = 16)
    private String keyPrefix;

    /**
     * Hash of the API key (the raw key is never persisted). Excluded from {@code toString()} (P1-8) as it is
     * derived credential material.
     */
    @ToString.Exclude
    @Column(name = "key_hash", nullable = false, length = 64)
    private String keyHash;

    @Column(length = 256)
    private String scopes;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(nullable = false)
    private boolean deleted = false;
}
