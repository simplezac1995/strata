// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.repository;

import dev.strata.system.rbac.api.entity.RoleMenu;
import dev.strata.system.rbac.api.entity.RoleMenuId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleMenuRepository extends JpaRepository<RoleMenu, RoleMenuId> {

    List<RoleMenu> findByRoleId(Long roleId);

    List<RoleMenu> findByRoleIdIn(List<Long> roleIds);

    void deleteByRoleId(Long roleId);
}
