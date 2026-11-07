// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(
        name = "sys_role_menu",
        indexes = {
            @Index(name = "idx_role_menu_role_id", columnList = "role_id"),
            @Index(name = "idx_role_menu_menu_id", columnList = "menu_id"),
            @Index(name = "idx_role_menu_role_menu", columnList = "role_id, menu_id")
        })
@IdClass(RoleMenuId.class)
@Data
public class RoleMenu {

    @Id
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Id
    @Column(name = "menu_id", nullable = false)
    private Long menuId;
}
