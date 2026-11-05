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
        name = "sys_dict_data",
        indexes = {
            @Index(name = "idx_dd_type_id", columnList = "type_id"),
            @Index(name = "idx_dd_enabled", columnList = "enabled"),
            @Index(name = "idx_dd_sort", columnList = "sort_order")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class DictData extends AuditingEntity {

    @Column(name = "type_id", nullable = false)
    private Long typeId;

    @Column(name = "dict_value", nullable = false, length = 128)
    private String value;

    @Column(nullable = false, length = 128)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean enabled = true;
}
