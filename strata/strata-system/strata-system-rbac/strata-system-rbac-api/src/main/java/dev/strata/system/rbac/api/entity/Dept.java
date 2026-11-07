// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(
        name = "sys_dept",
        indexes = {
            @Index(name = "idx_dept_parent_id", columnList = "parent_id"),
            @Index(name = "idx_dept_tenant_id", columnList = "tenant_id"),
            @Index(name = "idx_dept_enabled", columnList = "enabled"),
            @Index(name = "idx_dept_parent_sort", columnList = "parent_id, sort_order")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class Dept extends AuditingEntity {

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "tenant_id")
    private Long tenantId;
}
