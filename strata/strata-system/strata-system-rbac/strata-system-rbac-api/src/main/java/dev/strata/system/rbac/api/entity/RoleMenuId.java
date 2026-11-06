// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.api.entity;

import java.io.Serializable;
import java.util.Objects;

public class RoleMenuId implements Serializable {

    private Long roleId;
    private Long menuId;

    public RoleMenuId() {}

    public RoleMenuId(Long roleId, Long menuId) {
        this.roleId = roleId;
        this.menuId = menuId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoleMenuId that)) {
            return false;
        }
        return Objects.equals(roleId, that.roleId) && Objects.equals(menuId, that.menuId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId, menuId);
    }
}
