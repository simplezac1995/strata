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
        name = "sys_menu",
        indexes = {
            @Index(name = "idx_menu_parent_id", columnList = "parent_id"),
            @Index(name = "idx_menu_sort", columnList = "sort_order"),
            @Index(name = "idx_menu_type", columnList = "type"),
            @Index(name = "idx_menu_parent_sort", columnList = "parent_id, sort_order"),
            @Index(name = "idx_menu_permission", columnList = "permission")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class Menu extends AuditingEntity {

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 64)
    private String name;

    /** directory | menu | button */
    @Column(nullable = false, length = 32)
    private String type;

    @Column(length = 128)
    private String path;

    @Column(length = 128)
    private String component;

    @Column(length = 128)
    private String permission;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean visible = true;

    @Column(nullable = false)
    private boolean enabled = true;
}
