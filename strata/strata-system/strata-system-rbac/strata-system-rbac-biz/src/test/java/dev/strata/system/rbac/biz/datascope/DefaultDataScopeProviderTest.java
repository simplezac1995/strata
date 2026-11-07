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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
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
    class RoleMapping {

        @Test
        void emptyRolesDefaultToSelfOnly() {
            assertThat(provider.resolveScopeFromRoles(null)).isEqualTo(DataScopeType.SELF_ONLY);
            assertThat(provider.resolveScopeFromRoles(List.of())).isEqualTo(DataScopeType.SELF_ONLY);
        }

        @Test
        void adminRolesGetAll() {
            assertThat(provider.resolveScopeFromRoles(List.of(role("ROLE_ADMIN"))))
                    .isEqualTo(DataScopeType.ALL);
            assertThat(provider.resolveScopeFromRoles(List.of(role("super_admin"))))
                    .isEqualTo(DataScopeType.ALL);
            assertThat(provider.resolveScopeFromRoles(List.of(role("SYSTEM")))).isEqualTo(DataScopeType.ALL);
        }

        @Test
        void managerRolesGetDeptAndChildren() {
            assertThat(provider.resolveScopeFromRoles(List.of(role("ROLE_DEPT_MANAGER"))))
                    .isEqualTo(DataScopeType.DEPT_AND_CHILDREN);
            assertThat(provider.resolveScopeFromRoles(List.of(role("DEPT_HEAD"))))
                    .isEqualTo(DataScopeType.DEPT_AND_CHILDREN);
            assertThat(provider.resolveScopeFromRoles(List.of(role("MANAGER"))))
                    .isEqualTo(DataScopeType.DEPT_AND_CHILDREN);
        }

        @Test
        void memberRolesGetDeptOnly() {
            assertThat(provider.resolveScopeFromRoles(List.of(role("DEPT_VIEWER"))))
                    .isEqualTo(DataScopeType.DEPT_ONLY);
            assertThat(provider.resolveScopeFromRoles(List.of(role("DEPT_MEMBER"))))
                    .isEqualTo(DataScopeType.DEPT_ONLY);
        }

        @Test
        void plainRoleGetsSelfOnly() {
            assertThat(provider.resolveScopeFromRoles(List.of(role("ROLE_USER"))))
                    .isEqualTo(DataScopeType.SELF_ONLY);
        }

        @Test
        void nullCodeDoesNotThrow() {
            assertThat(provider.resolveScopeFromRoles(List.of(role(null), role("ROLE_USER"))))
                    .isEqualTo(DataScopeType.SELF_ONLY);
        }

        @Test
        void adminTakesPrecedenceOverManager() {
            List<Role> roles = List.of(role("ROLE_DEPT_MANAGER"), role("ROLE_ADMIN"));
            assertThat(provider.resolveScopeFromRoles(roles)).isEqualTo(DataScopeType.ALL);
        }
    }

    @Nested
    class ResolveScope {

        @Test
        void nullUserIdDefaultsToSelf() {
            assertThat(provider.resolveScope(null)).isEqualTo(DataScopeType.SELF_ONLY);
            verify(userRepository, never()).findById(anyLong());
        }

        @Test
        void missingUserDefaultsToSelf() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());
            assertThat(provider.resolveScope(1L)).isEqualTo(DataScopeType.SELF_ONLY);
        }

        @Test
        void presentUserResolvesFromRoles() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1)));
            when(roleRepository.findAll()).thenReturn(List.of(role("ROLE_ADMIN")));

            assertThat(provider.resolveScope(1L)).isEqualTo(DataScopeType.ALL);
        }

        @Test
        void exceptionDefaultsToSelf() {
            when(userRepository.findById(1L)).thenThrow(new RuntimeException("db down"));
            assertThat(provider.resolveScope(1L)).isEqualTo(DataScopeType.SELF_ONLY);
        }
    }

    @Nested
    class DeptResolution {

        @Test
        void nullUserHasNoDept() {
            assertThat(provider.resolveDeptId(null)).isNull();
        }

        @Test
        void presentUserHasNoDeptYet() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1)));
            assertThat(provider.resolveDeptId(1L)).isNull();
        }

        @Test
        void exceptionReturnsNull() {
            when(userRepository.findById(1L)).thenThrow(new RuntimeException("db down"));
            assertThat(provider.resolveDeptId(1L)).isNull();
        }

        @Test
        void customDeptIdsEmpty() {
            assertThat(provider.resolveCustomDeptIds(1L)).isEmpty();
        }

        @Test
        void nullDeptReturnsEmpty() {
            assertThat(provider.resolveDeptAndChildren(null)).isEmpty();
        }

        @Test
        void recursivelyCollectsTree() {
            when(deptRepository.findByParentIdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of(dept(2), dept(3)));
            when(deptRepository.findByParentIdOrderBySortOrderAscIdAsc(2L)).thenReturn(List.of(dept(4)));
            when(deptRepository.findByParentIdOrderBySortOrderAscIdAsc(3L)).thenReturn(List.of());
            when(deptRepository.findByParentIdOrderBySortOrderAscIdAsc(4L)).thenReturn(List.of());

            Set<Long> result = provider.resolveDeptAndChildren(1L);

            assertThat(result).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        }

        @Test
        void treeQueryFailureKeepsSelf() {
            when(deptRepository.findByParentIdOrderBySortOrderAscIdAsc(1L)).thenThrow(new RuntimeException("db down"));

            assertThat(provider.resolveDeptAndChildren(1L)).containsExactly(1L);
        }
    }

    @Nested
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
        void noAuthenticationReturnsNull() {
            assertThat(provider.getCurrentUserId()).isNull();
        }

        @Test
        void unauthenticatedReturnsNull() {
            authenticate(false, null, null);
            assertThat(provider.getCurrentUserId()).isNull();
        }

        @Test
        void longPrincipalReturnsId() {
            authenticate(true, 77L, null);
            assertThat(provider.getCurrentUserId()).isEqualTo(77L);
        }

        @Test
        void usernameResolvesToUserId() {
            authenticate(true, "principal-string", "alice");
            when(userRepository.findByUsernameAndDeletedFalse("alice")).thenReturn(Optional.of(user(5)));

            assertThat(provider.getCurrentUserId()).isEqualTo(5L);
        }

        @Test
        void anonymousUserSkipped() {
            authenticate(true, "anonymousUser", "anonymousUser");
            assertThat(provider.getCurrentUserId()).isNull();
            verify(userRepository, never()).findByUsernameAndDeletedFalse(any());
        }

        @Test
        void lookupFailureReturnsNull() {
            authenticate(true, "principal-string", "alice");
            when(userRepository.findByUsernameAndDeletedFalse("alice")).thenThrow(new RuntimeException("db down"));
            assertThat(provider.getCurrentUserId()).isNull();
        }
    }
}
