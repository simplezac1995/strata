// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.seed;

import dev.strata.system.config.api.entity.DictData;
import dev.strata.system.config.api.entity.DictType;
import dev.strata.system.config.api.entity.SysParam;
import dev.strata.system.config.biz.repository.DictDataRepository;
import dev.strata.system.config.biz.repository.DictTypeRepository;
import dev.strata.system.config.biz.repository.SysParamRepository;
import dev.strata.system.rbac.api.entity.Menu;
import dev.strata.system.rbac.api.entity.Post;
import dev.strata.system.rbac.api.entity.Role;
import dev.strata.system.rbac.api.entity.RoleMenu;
import dev.strata.system.rbac.api.entity.User;
import dev.strata.system.rbac.api.entity.UserRole;
import dev.strata.system.rbac.biz.repository.MenuRepository;
import dev.strata.system.rbac.biz.repository.PostRepository;
import dev.strata.system.rbac.biz.repository.RoleMenuRepository;
import dev.strata.system.rbac.biz.repository.RoleRepository;
import dev.strata.system.rbac.biz.repository.UserRepository;
import dev.strata.system.rbac.biz.repository.UserRoleRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "strata.seed.enabled", havingValue = "true", matchIfMissing = true)
@Order(1)
public class SystemDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final PostRepository postRepository;
    private final DictTypeRepository dictTypeRepository;
    private final DictDataRepository dictDataRepository;
    private final SysParamRepository sysParamRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeedProperties seedProperties;
    private final ObjectMapper objectMapper;

    public SystemDataInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            MenuRepository menuRepository,
            UserRoleRepository userRoleRepository,
            RoleMenuRepository roleMenuRepository,
            PostRepository postRepository,
            DictTypeRepository dictTypeRepository,
            DictDataRepository dictDataRepository,
            SysParamRepository sysParamRepository,
            PasswordEncoder passwordEncoder,
            SeedProperties seedProperties,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.menuRepository = menuRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleMenuRepository = roleMenuRepository;
        this.postRepository = postRepository;
        this.dictTypeRepository = dictTypeRepository;
        this.dictDataRepository = dictDataRepository;
        this.sysParamRepository = sysParamRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedProperties = seedProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        seedMenus();

        Role adminRole = new Role();
        adminRole.setCode("admin");
        adminRole.setName("Administrator");
        adminRole.setEnabled(true);
        roleRepository.save(adminRole);

        for (Menu menu : menuRepository.findAll()) {
            RoleMenu link = new RoleMenu();
            link.setRoleId(adminRole.getId());
            link.setMenuId(menu.getId());
            roleMenuRepository.save(link);
        }

        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode(seedProperties.adminPassword()));
        admin.setNickname("Admin");
        admin.setEnabled(true);
        userRepository.save(admin);

        UserRole userRole = new UserRole();
        userRole.setUserId(admin.getId());
        userRole.setRoleId(adminRole.getId());
        userRoleRepository.save(userRole);

        seedReferenceData();
    }

    private void seedReferenceData() {
        ReferenceDataSeed seed = referenceData();
        seedPosts(seed);
        seedDictionaries(seed);
        seedParams(seed);
    }

    private void seedPosts(ReferenceDataSeed seed) {
        for (ReferenceDataSeed.PostSeed row : seed.posts()) {
            Post post = new Post();
            post.setCode(row.code());
            post.setName(row.name());
            post.setSortOrder(row.sortOrder());
            post.setEnabled(true);
            postRepository.save(post);
        }
    }

    private void seedDictionaries(ReferenceDataSeed seed) {
        Map<String, Long> typeIds = new HashMap<>();
        for (ReferenceDataSeed.DictTypeSeed row : seed.dictTypes()) {
            DictType type = new DictType();
            type.setCode(row.code());
            type.setName(row.name());
            type.setSortOrder(row.sortOrder());
            type.setEnabled(true);
            dictTypeRepository.save(type);
            typeIds.put(row.code(), type.getId());
        }

        for (ReferenceDataSeed.DictDataSeed item : seed.dictData()) {
            Long typeId = typeIds.get(item.typeCode());
            if (typeId == null) {
                continue;
            }
            DictData data = new DictData();
            data.setTypeId(typeId);
            data.setValue(item.value());
            data.setLabel(item.label());
            data.setSortOrder(item.sortOrder());
            data.setEnabled(true);
            dictDataRepository.save(data);
        }
    }

    private void seedParams(ReferenceDataSeed seed) {
        for (ReferenceDataSeed.ParamSeed row : seed.params()) {
            SysParam param = new SysParam();
            param.setKey(row.key());
            param.setValue(row.value());
            param.setType(row.type());
            param.setRemark(row.remark());
            sysParamRepository.save(param);
        }
    }

    private void seedMenus() {
        MenuSeedFile file = menuSeed();
        for (MenuSeedFile.MenuNodeSeed root : file.menus()) {
            persistMenuNode(root, null);
        }
    }

    private void persistMenuNode(MenuSeedFile.MenuNodeSeed node, Long parentId) {
        if (Boolean.TRUE.equals(node.crudButtons())) {
            String base = node.permissionBase();
            Menu menu = menu(
                    node.name(), "menu", node.path(), node.component(), base + ":list", parentId, node.sortOrder());
            menuRepository.save(menu);
            // Button labels are i18n keys resolved by the frontend t(node.name); they must be
            // language-agnostic (common.create/update/delete), never "{menuNameKey} Create" —
            // that leaks the raw key in every locale. Mirrors the explicit-buttons convention.
            menuRepository.save(button("common.create", base + ":create", menu.getId(), 1));
            menuRepository.save(button("common.update", base + ":update", menu.getId(), 2));
            menuRepository.save(button("common.delete", base + ":delete", menu.getId(), 3));
            return;
        }

        String type = node.type() != null ? node.type() : "menu";
        Menu menu =
                menu(node.name(), type, node.path(), node.component(), node.permission(), parentId, node.sortOrder());
        menuRepository.save(menu);

        if (node.buttons() != null) {
            for (MenuSeedFile.ButtonSeed button : node.buttons()) {
                menuRepository.save(button(button.name(), button.permission(), menu.getId(), button.sortOrder()));
            }
        }

        if (node.children() != null) {
            for (MenuSeedFile.MenuNodeSeed child : node.children()) {
                persistMenuNode(child, menu.getId());
            }
        }
    }

    private MenuSeedFile menuSeed() {
        try (InputStream in = getClass().getResourceAsStream("/seed/menus.json")) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath seed: /seed/menus.json");
            }
            return objectMapper.readValue(in, MenuSeedFile.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load menu seed data", ex);
        }
    }

    private ReferenceDataSeed referenceData() {
        try (InputStream in = getClass().getResourceAsStream("/seed/reference-data.json")) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath seed: /seed/reference-data.json");
            }
            return objectMapper.readValue(in, ReferenceDataSeed.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load reference seed data", ex);
        }
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
