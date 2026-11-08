// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "sys_post")
@Data
@EqualsAndHashCode(callSuper = true)
public class Post extends AuditingEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean enabled = true;
}
