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
        name = "sys_tenant",
        indexes = {
            @Index(name = "idx_tenant_code", columnList = "code"),
            @Index(name = "idx_tenant_status", columnList = "status")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class Tenant extends AuditingEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "tenant_domain", length = 128)
    private String domain;

    @Column(nullable = false, length = 16)
    private String status = "active";

    @Column(nullable = false, length = 16)
    private String plan = "free";
}
