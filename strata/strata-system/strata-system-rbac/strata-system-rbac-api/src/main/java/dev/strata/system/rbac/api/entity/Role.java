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
        name = "sys_role",
        indexes = {
            @Index(name = "idx_role_code", columnList = "code"),
            @Index(name = "idx_role_tenant_id", columnList = "tenant_id"),
            @Index(name = "idx_role_enabled", columnList = "enabled"),
            @Index(name = "idx_role_tenant_enabled", columnList = "tenant_id, enabled")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class Role extends AuditingEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "tenant_id")
    private Long tenantId;
}
