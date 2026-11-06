// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(
        name = "sys_sensitive_word",
        indexes = {
            @Index(name = "idx_sw_word", columnList = "word"),
            @Index(name = "idx_sw_enabled", columnList = "enabled")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class SensitiveWord extends AuditingEntity {

    @Column(nullable = false, unique = true, length = 128)
    private String word;

    @Column(nullable = false)
    private boolean enabled = true;
}
