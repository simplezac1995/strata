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
        name = "sys_dict_type",
        indexes = {
            @Index(name = "idx_dt_code", columnList = "code"),
            @Index(name = "idx_dt_enabled", columnList = "enabled")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class DictType extends AuditingEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean enabled = true;
}
