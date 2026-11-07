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
        name = "sys_user_role",
        indexes = {
            @Index(name = "idx_user_role_user_id", columnList = "user_id"),
            @Index(name = "idx_user_role_role_id", columnList = "role_id"),
            @Index(name = "idx_user_role_user_role", columnList = "user_id, role_id")
        })
@IdClass(UserRoleId.class)
@Data
public class UserRole {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "role_id", nullable = false)
    private Long roleId;
}
