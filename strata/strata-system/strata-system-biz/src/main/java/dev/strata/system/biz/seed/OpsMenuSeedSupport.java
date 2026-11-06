// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.seed;

import dev.strata.system.api.seed.OpsMenuSeedContext;
import dev.strata.system.rbac.api.entity.Menu;
import dev.strata.system.rbac.api.entity.RoleMenu;
import dev.strata.system.rbac.biz.repository.MenuRepository;
import dev.strata.system.rbac.biz.repository.RoleMenuRepository;
import dev.strata.system.rbac.biz.repository.RoleRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OpsMenuSeedSupport implements OpsMenuSeedContext {

    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;

    public OpsMenuSeedSupport(
            MenuRepository menuRepository, RoleRepository roleRepository, RoleMenuRepository roleMenuRepository) {
        this.menuRepository = menuRepository;
        this.roleRepository = roleRepository;
        this.roleMenuRepository = roleMenuRepository;
    }

    @Override
    public boolean existsByPermission(String permission) {
        return menuRepository.existsByPermission(permission);
    }

    @Override
    public Long findDirectoryId(String path, String type) {
        return menuRepository
                .findFirstByPathAndType(path, type)
                .map(Menu::getId)
                .orElse(null);
    }

    @Override
    public boolean existsChild(long parentId, String path) {
        return menuRepository.existsByParentIdAndPath(parentId, path);
    }

    @Override
    public long ensureDirectory(String name, String path, int sortOrder, Long parentId) {
        return menuRepository
                .findFirstByPathAndType(path, "directory")
                .map(Menu::getId)
                .orElseGet(() -> menuRepository
                        .save(menu(name, "directory", path, "Layout", null, parentId, sortOrder))
                        .getId());
    }

    @Override
    public List<Long> addMenuWithButtons(
            String name, String path, String component, String permissionPrefix, long parentId, int sortOrder) {
        Menu menu = menu(name, "menu", path, component, permissionPrefix + ":list", parentId, sortOrder);
        menuRepository.save(menu);
        List<Long> ids = new ArrayList<>();
        ids.add(menu.getId());
        if ("system:sitesetting".equals(permissionPrefix) || "system:messagechannel".equals(permissionPrefix)) {
            Menu update = button(name + " Update", permissionPrefix + ":update", menu.getId(), 1);
            menuRepository.save(update);
            ids.add(update.getId());
            return ids;
        }
        if ("system:cache".equals(permissionPrefix)) {
            Menu delete = button(name + " Delete", permissionPrefix + ":delete", menu.getId(), 1);
            menuRepository.save(delete);
            ids.add(delete.getId());
            return ids;
        }
        Menu create = button(name + " Create", permissionPrefix + ":create", menu.getId(), 1);
        Menu update = button(name + " Update", permissionPrefix + ":update", menu.getId(), 2);
        Menu delete = button(name + " Delete", permissionPrefix + ":delete", menu.getId(), 3);
        menuRepository.saveAll(List.of(create, update, delete));
        ids.add(create.getId());
        ids.add(update.getId());
        ids.add(delete.getId());
        return ids;
    }

    @Override
    public long addMenu(String name, String path, String component, String permission, long parentId, int sortOrder) {
        return menuRepository
                .save(menu(name, "menu", path, component, permission, parentId, sortOrder))
                .getId();
    }

    @Override
    public long addButton(String name, String permission, long parentId, int sortOrder) {
        return menuRepository
                .save(button(name, permission, parentId, sortOrder))
                .getId();
    }

    @Override
    public void linkAdminRole(List<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return;
        }
        roleRepository.findByCodeAndDeletedFalse("admin").ifPresent(admin -> {
            for (Long menuId : menuIds) {
                RoleMenu link = new RoleMenu();
                link.setRoleId(admin.getId());
                link.setMenuId(menuId);
                roleMenuRepository.save(link);
            }
        });
    }

    private static Menu menu(
            String name, String type, String path, String component, String permission, Long parentId, int sort) {
        Menu menu = new Menu();
        menu.setName(name);
        menu.setType(type);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setPermission(permission);
        menu.setParentId(parentId);
        menu.setSortOrder(sort);
        menu.setVisible(true);
        menu.setEnabled(true);
        return menu;
    }

    private static Menu button(String name, String permission, Long parentId, int sort) {
        return menu(name, "button", null, null, permission, parentId, sort);
    }
}
