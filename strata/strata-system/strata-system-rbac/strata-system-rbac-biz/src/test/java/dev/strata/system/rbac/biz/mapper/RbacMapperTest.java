// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.system.api.dto.MenuDto;
import dev.strata.system.api.dto.RoleDto;
import dev.strata.system.api.dto.UserDto;
import dev.strata.system.api.request.MenuCreateRequest;
import dev.strata.system.api.request.MenuUpdateRequest;
import dev.strata.system.api.request.RoleCreateRequest;
import dev.strata.system.api.request.RoleUpdateRequest;
import dev.strata.system.api.request.UserCreateRequest;
import dev.strata.system.api.request.UserUpdateRequest;
import dev.strata.system.rbac.api.entity.Menu;
import dev.strata.system.rbac.api.entity.Role;
import dev.strata.system.rbac.api.entity.User;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Mapper 接口契约单元测试：通过直接实例化 MapStruct 生成实现，覆盖 toEntity/toDto/update 中的 null-check
 * 与 defaultValue 分支。不启动 Spring 容器。
 *
 * <p>本类覆盖用户 / 角色 / 菜单三个 Mapper；部门 / 租户 / OAuth 客户端见 {@link RbacMapperOrgTest}。
 */
class RbacMapperTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final UserMapper userMapper = new UserMapperImpl();
    private final RoleMapper roleMapper = new RoleMapperImpl();
    private final MenuMapper menuMapper = new MenuMapperImpl();

    @Nested
    @DisplayName("UserMapper")
    class UserMapperCases {

        @Test
        void toEntityNull() {
            assertThat(userMapper.toEntity(null)).isNull();
        }

        @Test
        void toEntityFull() {
            UserCreateRequest request = new UserCreateRequest("alice", "secret123", "Alice", true, List.of("1", "2"));
            User user = userMapper.toEntity(request);
            assertThat(user.getUsername()).isEqualTo("alice");
            assertThat(user.getNickname()).isEqualTo("Alice");
            assertThat(user.isEnabled()).isTrue();
        }

        @Test
        void toDtoBothNull() {
            assertThat(userMapper.toDto(null, null)).isNull();
        }

        @Test
        void toDtoEntityNull() {
            UserDto dto = userMapper.toDto(null, List.of("9"));
            assertThat(dto).isNotNull();
            assertThat(dto.id()).isNull();
            assertThat(dto.username()).isNull();
            assertThat(dto.enabled()).isFalse();
            assertThat(dto.roleIds()).containsExactly("9");
        }

        @Test
        void toDtoListNull() {
            User user = fullUser();
            UserDto dto = userMapper.toDto(user, null);
            assertFullUser(dto);
            assertThat(dto.roleIds()).isNull();
        }

        @Test
        void toDtoNullIdAndListCopy() {
            User user = fullUser();
            user.setId(null);
            List<String> roleIds = List.of("1", "2");
            UserDto dto = userMapper.toDto(user, roleIds);
            assertThat(dto.id()).isNull();
            assertThat(dto.roleIds()).containsExactly("1", "2");
            // 拷贝独立性：修改返回集合不影响源
            dto.roleIds().add("mutated");
            assertThat(roleIds).containsExactly("1", "2");
        }

        @Test
        void updateNull() {
            User user = fullUser();
            userMapper.update(null, user);
            assertFullUser(userMapper.toDto(user, null));
        }

        @Test
        void updateFull() {
            User user = new User();
            userMapper.update(new UserUpdateRequest("NewNick", false, "newpass", List.of("3")), user);
            assertThat(user.getNickname()).isEqualTo("NewNick");
            assertThat(user.isEnabled()).isFalse();
        }

        @Test
        void updateNullFieldsKeepsOriginal() {
            User user = fullUser();
            userMapper.update(new UserUpdateRequest(null, null, "ignored", null), user);
            UserDto dto = userMapper.toDto(user, null);
            assertThat(dto.nickname()).isEqualTo("Alice Nick");
            assertThat(dto.enabled()).isTrue();
        }

        private User fullUser() {
            User user = new User();
            user.setId(42L);
            user.setUsername("alice");
            user.setNickname("Alice Nick");
            user.setEmail("alice@example.com");
            user.setEnabled(true);
            user.setCreatedAt(NOW);
            return user;
        }

        private void assertFullUser(UserDto dto) {
            assertThat(dto.id()).isEqualTo("42");
            assertThat(dto.username()).isEqualTo("alice");
            assertThat(dto.nickname()).isEqualTo("Alice Nick");
            assertThat(dto.email()).isEqualTo("alice@example.com");
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.createdAt()).isEqualTo(NOW);
        }
    }

    @Nested
    @DisplayName("RoleMapper")
    class RoleMapperCases {

        @Test
        void toEntityNull() {
            assertThat(roleMapper.toEntity(null)).isNull();
        }

        @Test
        void toEntityFull() {
            Role role = roleMapper.toEntity(new RoleCreateRequest("admin", "Administrator", true, List.of("1")));
            assertThat(role.getCode()).isEqualTo("admin");
            assertThat(role.getName()).isEqualTo("Administrator");
            assertThat(role.isEnabled()).isTrue();
        }

        @Test
        void toDtoBothNull() {
            assertThat(roleMapper.toDto(null, null)).isNull();
        }

        @Test
        void toDtoEntityNull() {
            RoleDto dto = roleMapper.toDto(null, List.of("7"));
            assertThat(dto).isNotNull();
            assertThat(dto.id()).isNull();
            assertThat(dto.code()).isNull();
            assertThat(dto.menuIds()).containsExactly("7");
        }

        @Test
        void toDtoListNull() {
            Role role = fullRole();
            RoleDto dto = roleMapper.toDto(role, null);
            assertThat(dto.id()).isEqualTo("7");
            assertThat(dto.code()).isEqualTo("admin");
            assertThat(dto.name()).isEqualTo("Administrator");
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.menuIds()).isNull();
        }

        @Test
        void toDtoNullIdAndListCopy() {
            Role role = fullRole();
            role.setId(null);
            List<String> menuIds = List.of("1", "2");
            RoleDto dto = roleMapper.toDto(role, menuIds);
            assertThat(dto.id()).isNull();
            assertThat(dto.menuIds()).containsExactly("1", "2");
        }

        @Test
        void updateNull() {
            Role role = fullRole();
            roleMapper.update(null, role);
            assertThat(role.getName()).isEqualTo("Administrator");
        }

        @Test
        void updateFull() {
            Role role = new Role();
            roleMapper.update(new RoleUpdateRequest("Renamed", false, List.of("9")), role);
            assertThat(role.getName()).isEqualTo("Renamed");
            assertThat(role.isEnabled()).isFalse();
        }

        @Test
        void updateNullFieldsKeepsOriginal() {
            Role role = fullRole();
            roleMapper.update(new RoleUpdateRequest(null, null, null), role);
            assertThat(role.getName()).isEqualTo("Administrator");
            assertThat(role.isEnabled()).isTrue();
        }

        private Role fullRole() {
            Role role = new Role();
            role.setId(7L);
            role.setCode("admin");
            role.setName("Administrator");
            role.setEnabled(true);
            return role;
        }
    }

    @Nested
    @DisplayName("MenuMapper")
    class MenuMapperCases {

        @Test
        void toEntityNull() {
            assertThat(menuMapper.toEntity(null)).isNull();
        }

        @Test
        void toEntityParentIdNumeric() {
            Menu menu = menuMapper.toEntity(fullMenuRequest("10"));
            assertThat(menu.getParentId()).isEqualTo(10L);
            assertThat(menu.getName()).isEqualTo("用户管理");
            assertThat(menu.getType()).isEqualTo("menu");
            assertThat(menu.getPath()).isEqualTo("/system/user");
            assertThat(menu.getComponent()).isEqualTo("system/User");
            assertThat(menu.getPermission()).isEqualTo("system:user:list");
            assertThat(menu.getSortOrder()).isEqualTo(5);
            assertThat(menu.isVisible()).isTrue();
            assertThat(menu.isEnabled()).isTrue();
        }

        @Test
        void toEntityParentIdNull() {
            MenuCreateRequest request = fullMenuRequest(null);
            Menu menu = menuMapper.toEntity(request);
            assertThat(menu.getParentId()).isNull();
        }

        @Test
        void toEntityParentIdBlank() {
            Menu menu = menuMapper.toEntity(fullMenuRequest("   "));
            assertThat(menu.getParentId()).isNull();
        }

        @Test
        void toDtoNull() {
            assertThat(menuMapper.toDto(null)).isNull();
        }

        @Test
        void toDtoFull() {
            Menu menu = fullMenu();
            MenuDto dto = menuMapper.toDto(menu);
            assertThat(dto.id()).isEqualTo("3");
            assertThat(dto.parentId()).isEqualTo("10");
            assertThat(dto.name()).isEqualTo("用户管理");
            assertThat(dto.type()).isEqualTo("menu");
            assertThat(dto.path()).isEqualTo("/system/user");
            assertThat(dto.component()).isEqualTo("system/User");
            assertThat(dto.permission()).isEqualTo("system:user:list");
            assertThat(dto.sortOrder()).isEqualTo(5);
            assertThat(dto.visible()).isTrue();
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.children()).isEmpty();
        }

        @Test
        void toDtoNullIds() {
            Menu menu = fullMenu();
            menu.setId(null);
            menu.setParentId(null);
            MenuDto dto = menuMapper.toDto(menu);
            assertThat(dto.id()).isNull();
            assertThat(dto.parentId()).isNull();
            assertThat(dto.name()).isEqualTo("用户管理");
        }

        @Test
        void updateNull() {
            Menu menu = fullMenu();
            menuMapper.update(null, menu);
            assertThat(menu.getName()).isEqualTo("用户管理");
        }

        @Test
        void updateFull() {
            Menu menu = new Menu();
            menuMapper.update(new MenuUpdateRequest("20", "改名", "button", "/p", "comp", "perm", 9, false, false), menu);
            assertThat(menu.getParentId()).isEqualTo(20L);
            assertThat(menu.getName()).isEqualTo("改名");
            assertThat(menu.getType()).isEqualTo("button");
            assertThat(menu.getPath()).isEqualTo("/p");
            assertThat(menu.getComponent()).isEqualTo("comp");
            assertThat(menu.getPermission()).isEqualTo("perm");
            assertThat(menu.getSortOrder()).isEqualTo(9);
            assertThat(menu.isVisible()).isFalse();
            assertThat(menu.isEnabled()).isFalse();
        }

        @Test
        void updateNullFieldsKeepsOriginal() {
            Menu menu = fullMenu();
            menuMapper.update(new MenuUpdateRequest(null, null, null, null, null, null, null, null, null), menu);
            assertThat(menu.getName()).isEqualTo("用户管理");
            assertThat(menu.getParentId()).isEqualTo(10L);
            assertThat(menu.getSortOrder()).isEqualTo(5);
        }

        private MenuCreateRequest fullMenuRequest(String parentId) {
            return new MenuCreateRequest(
                    parentId, "用户管理", "menu", "/system/user", "system/User", "system:user:list", 5, true, true);
        }

        private Menu fullMenu() {
            Menu menu = new Menu();
            menu.setId(3L);
            menu.setParentId(10L);
            menu.setName("用户管理");
            menu.setType("menu");
            menu.setPath("/system/user");
            menu.setComponent("system/User");
            menu.setPermission("system:user:list");
            menu.setSortOrder(5);
            menu.setVisible(true);
            menu.setEnabled(true);
            return menu;
        }
    }
}
