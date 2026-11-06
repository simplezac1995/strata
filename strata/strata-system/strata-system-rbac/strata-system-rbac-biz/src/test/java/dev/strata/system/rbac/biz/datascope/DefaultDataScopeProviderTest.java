// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.datascope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.security.datascope.DataScopeType;
import dev.strata.system.rbac.api.entity.Dept;
import dev.strata.system.rbac.api.entity.Role;
import dev.strata.system.rbac.api.entity.User;
import dev.strata.system.rbac.biz.repository.DeptRepository;
import dev.strata.system.rbac.biz.repository.RoleRepository;
import dev.strata.system.rbac.biz.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultDataScopeProvider 数据范围解析")
class DefaultDataScopeProviderTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private DeptRepository deptRepository;

    private DefaultDataScopeProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DefaultDataScopeProvider(userRepository, roleRepository, deptRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static Role role(String code) {
        Role r = new Role();
        r.setCode(code);
        return r;
    }

    private static User user(long id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private static Dept dept(long id) {
        Dept d = new Dept();
        d.setId(id);
        return d;
    }

    @Nested
    @DisplayName("resolveScopeFromRoles 角色到范围映射")
    class RoleMapping {

        @Test
        @DisplayName("null 或空角色列表默认仅本人")
        void emptyRolesDefaultToSelfOnly() {
            assertThat(provider.resolveScopeFromRoles(null)).isEqualTo(DataScopeType.SELF_ONLY);
            assertThat(provider.resolveScopeFromRoles(List.of())).isEqualTo(DataScopeType.SELF_ONLY);
        }

        @Test
        @DisplayName("管理员类角色（大小写不敏感）获得全部数据")
        void adminRolesGetAll() {
            assertThat(provider.resolveScopeFromRoles(List.of(role("ROLE_ADMIN"))))
                    .isEqualTo(DataScopeType.ALL);
            assertThat(provider.resolveScopeFromRoles(List.of(role("super_admin"))))
                    .isEqualTo(DataScopeType.ALL);
            assertThat(provider.resolveScopeFromRoles(List.of(role("SYSTEM")))).isEqualTo(DataScopeType.ALL);
        }

        @Test
        @DisplayName("部门负责人类角色获得本部门及以下")
        void managerRolesGetDeptAndChildren() {
            assertThat(provider.resolveScopeFromRoles(List.of(role("ROLE_DEPT_MANAGER"))))
                    .isEqualTo(DataScopeType.DEPT_AND_CHILDREN);
            assertThat(provider.resolveScopeFromRoles(List.of(role("DEPT_HEAD"))))
                    .isEqualTo(DataScopeType.DEPT_AND_CHILDREN);
            assertThat(provider.resolveScopeFromRoles(List.of(role("MANAGER"))))
                    .isEqualTo(DataScopeType.DEPT_AND_CHILDREN);
        }

        @Test
        @DisplayName("部门成员类角色获得仅本部门")
        void memberRolesGetDeptOnly() {
            assertThat(provider.resolveScopeFromRoles(List.of(role("DEPT_VIEWER"))))
                    .isEqualTo(DataScopeType.DEPT_ONLY);
            assertThat(provider.resolveScopeFromRoles(List.of(role("DEPT_MEMBER"))))
                    .isEqualTo(DataScopeType.DEPT_ONLY);
        }

        @Test
        @DisplayName("普通角色仅本人")
        void plainRoleGetsSelfOnly() {
            assertThat(provider.resolveScopeFromRoles(List.of(role("ROLE_USER"))))
                    .isEqualTo(DataScopeType.SELF_ONLY);
        }

        @Test
        @DisplayName("角色 code 为 null 时不抛异常，落到仅本人")
        void nullCodeDoesNotThrow() {
            assertThat(provider.resolveScopeFromRoles(List.of(role(null), role("ROLE_USER"))))
                    .isEqualTo(DataScopeType.SELF_ONLY);
        }

        @Test
        @DisplayName("管理员优先级高于部门负责人")
        void adminTakesPrecedenceOverManager() {
            List<Role> roles = List.of(role("ROLE_DEPT_MANAGER"), role("ROLE_ADMIN"));
            assertThat(provider.resolveScopeFromRoles(roles)).isEqualTo(DataScopeType.ALL);
        }
    }

    @Nested
    @DisplayName("resolveScope 用户范围解析（fail-safe）")
    class ResolveScope {

        @Test
        @DisplayName("用户 id 为 null 时直接仅本人，不访问仓库")
        void nullUserIdDefaultsToSelf() {
            assertThat(provider.resolveScope(null)).isEqualTo(DataScopeType.SELF_ONLY);
            verify(userRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("用户不存在时仅本人")
        void missingUserDefaultsToSelf() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());
            assertThat(provider.resolveScope(1L)).isEqualTo(DataScopeType.SELF_ONLY);
        }

        @Test
        @DisplayName("用户存在时按其角色解析范围")
        void presentUserResolvesFromRoles() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1)));
            when(roleRepository.findAll()).thenReturn(List.of(role("ROLE_ADMIN")));

            assertThat(provider.resolveScope(1L)).isEqualTo(DataScopeType.ALL);
        }

        @Test
        @DisplayName("仓库抛异常时安全降级为仅本人")
        void exceptionDefaultsToSelf() {
            when(userRepository.findById(1L)).thenThrow(new RuntimeException("db down"));
            assertThat(provider.resolveScope(1L)).isEqualTo(DataScopeType.SELF_ONLY);
        }
    }

    @Nested
    @DisplayName("部门与自定义范围")
    class DeptResolution {

        @Test
        @DisplayName("resolveDeptId 用户 id 为 null 返回 null")
        void nullUserHasNoDept() {
            assertThat(provider.resolveDeptId(null)).isNull();
        }

        @Test
        @DisplayName("resolveDeptId 当前实现对存在用户也返回 null（尚未关联部门）")
        void presentUserHasNoDeptYet() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1)));
            assertThat(provider.resolveDeptId(1L)).isNull();
        }

        @Test
        @DisplayName("resolveDeptId 仓库异常时返回 null")
        void exceptionReturnsNull() {
            when(userRepository.findById(1L)).thenThrow(new RuntimeException("db down"));
            assertThat(provider.resolveDeptId(1L)).isNull();
        }

        @Test
        @DisplayName("resolveCustomDeptIds 默认实现返回空集")
        void customDeptIdsEmpty() {
            assertThat(provider.resolveCustomDeptIds(1L)).isEmpty();
        }

        @Test
        @DisplayName("resolveDeptAndChildren 入参为 null 返回空集")
        void nullDeptReturnsEmpty() {
            assertThat(provider.resolveDeptAndChildren(null)).isEmpty();
        }

        @Test
        @DisplayName("resolveDeptAndChildren 递归收集自身与全部子部门")
        void recursivelyCollectsTree() {
            when(deptRepository.findByParentIdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of(dept(2), dept(3)));
            when(deptRepository.findByParentIdOrderBySortOrderAscIdAsc(2L)).thenReturn(List.of(dept(4)));
            when(deptRepository.findByParentIdOrderBySortOrderAscIdAsc(3L)).thenReturn(List.of());
            when(deptRepository.findByParentIdOrderBySortOrderAscIdAsc(4L)).thenReturn(List.of());

            Set<Long> result = provider.resolveDeptAndChildren(1L);

            assertThat(result).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        }

        @Test
        @DisplayName("递归查询失败时至少保留自身部门（fail-safe）")
        void treeQueryFailureKeepsSelf() {
            when(deptRepository.findByParentIdOrderBySortOrderAscIdAsc(1L)).thenThrow(new RuntimeException("db down"));

            assertThat(provider.resolveDeptAndChildren(1L)).containsExactly(1L);
        }
    }

    @Nested
    @DisplayName("getCurrentUserId 安全上下文解析")
    class CurrentUser {

        private void authenticate(Boolean authenticated, Object principal, String name) {
            org.springframework.security.core.Authentication auth =
                    org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
            when(auth.isAuthenticated()).thenReturn(authenticated);
            if (principal != null) {
                when(auth.getPrincipal()).thenReturn(principal);
            }
            if (name != null) {
                when(auth.getName()).thenReturn(name);
            }
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
        }

        @Test
        @DisplayName("无认证信息返回 null")
        void noAuthenticationReturnsNull() {
            assertThat(provider.getCurrentUserId()).isNull();
        }

        @Test
        @DisplayName("未认证返回 null")
        void unauthenticatedReturnsNull() {
            authenticate(false, null, null);
            assertThat(provider.getCurrentUserId()).isNull();
        }

        @Test
        @DisplayName("principal 直接是用户 id 时返回该 id")
        void longPrincipalReturnsId() {
            authenticate(true, 77L, null);
            assertThat(provider.getCurrentUserId()).isEqualTo(77L);
        }

        @Test
        @DisplayName("principal 非 id 时按用户名查用户")
        void usernameResolvesToUserId() {
            authenticate(true, "principal-string", "alice");
            when(userRepository.findByUsernameAndDeletedFalse("alice")).thenReturn(Optional.of(user(5)));

            assertThat(provider.getCurrentUserId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("anonymousUser 不查库直接返回 null")
        void anonymousUserSkipped() {
            authenticate(true, "anonymousUser", "anonymousUser");
            assertThat(provider.getCurrentUserId()).isNull();
            verify(userRepository, never()).findByUsernameAndDeletedFalse(any());
        }

        @Test
        @DisplayName("查库异常时返回 null")
        void lookupFailureReturnsNull() {
            authenticate(true, "principal-string", "alice");
            when(userRepository.findByUsernameAndDeletedFalse("alice")).thenThrow(new RuntimeException("db down"));
            assertThat(provider.getCurrentUserId()).isNull();
        }
    }
}
