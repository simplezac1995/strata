// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.scim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.strata.system.rbac.api.entity.Role;
import dev.strata.system.rbac.api.entity.User;
import dev.strata.system.rbac.api.entity.UserRole;
import dev.strata.system.rbac.biz.repository.RoleRepository;
import dev.strata.system.rbac.biz.repository.UserRepository;
import dev.strata.system.rbac.biz.repository.UserRoleRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("SCIM Groups 服务测试")
class ScimGroupsTest {

    private final ObjectMapper json = new ObjectMapper();

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserRepository userRepository;

    private ScimGroups groups;

    @BeforeEach
    void setUp() {
        groups = new ScimGroups(roleRepository, userRoleRepository, userRepository, new ScimProperties());
    }

    @Test
    @DisplayName("创建组：displayName 映射为角色名，成员建立角色关联")
    void create_mapsRoleAndMembers() throws Exception {
        when(roleRepository.findByCodeAndDeletedFalse("scim-platform-admins")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(5L);
            return role;
        });
        User member = new User();
        member.setId(7L);
        member.setUsername("alice");
        when(userRepository.findById(7L)).thenReturn(Optional.of(member));
        var body = json.readTree("{\"displayName\":\"Platform Admins\",\"members\":[{\"value\":\"7\"}]}");

        Map<String, Object> resource = groups.create(body);

        assertThat(resource).containsEntry("displayName", "Platform Admins");
        ArgumentCaptor<UserRole> link = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(link.capture());
        assertThat(link.getValue().getUserId()).isEqualTo(7L);
        assertThat(link.getValue().getRoleId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("PATCH add members 追加成员")
    void patch_addMembers() throws Exception {
        Role role = new Role();
        role.setId(5L);
        role.setName("Platform Admins");
        when(roleRepository.findById(5L)).thenReturn(Optional.of(role));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRoleRepository.findByRoleId(5L)).thenReturn(List.of());
        User member = new User();
        member.setId(7L);
        member.setUsername("alice");
        when(userRepository.findById(7L)).thenReturn(Optional.of(member));
        var body =
                json.readTree("{\"Operations\":[{\"op\":\"add\",\"path\":\"members\",\"value\":[{\"value\":\"7\"}]}]}");

        groups.patch("5", body);

        verify(userRoleRepository).deleteByRoleId(5L);
        ArgumentCaptor<UserRole> link = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(link.capture());
        assertThat(link.getValue().getUserId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("未知成员：400")
    void create_unknownMember_rejected() throws Exception {
        when(roleRepository.findByCodeAndDeletedFalse(any())).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(5L);
            return role;
        });
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        var body = json.readTree("{\"displayName\":\"Ghosts\",\"members\":[{\"value\":\"99\"}]}");

        assertThatThrownBy(() -> groups.create(body))
                .isInstanceOf(ScimException.class)
                .extracting(exception -> ((ScimException) exception).status())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("删除组：软删除角色并清理关联")
    void delete_softDeletes() {
        Role role = new Role();
        role.setId(5L);
        role.setName("Platform Admins");
        when(roleRepository.findById(5L)).thenReturn(Optional.of(role));
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        groups.delete("5");

        assertThat(role.isDeleted()).isTrue();
        verify(userRoleRepository).deleteByRoleId(5L);
    }
}
