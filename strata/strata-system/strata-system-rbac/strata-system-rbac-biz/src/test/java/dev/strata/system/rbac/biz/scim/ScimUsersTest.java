// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.scim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.strata.system.rbac.api.entity.User;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("SCIM Users 服务测试")
class ScimUsersTest {

    private final ObjectMapper json = new ObjectMapper();

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private ScimUsers users;

    @BeforeEach
    void setUp() {
        users = new ScimUsers(
                userRepository, userRoleRepository, roleRepository, passwordEncoder, new ScimProperties());
    }

    private User user(long id, String username, String email, boolean enabled) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(enabled);
        return user;
    }

    @Test
    @DisplayName("创建用户：用户名/邮箱/active 映射并保存")
    void create_mapsProfile() throws Exception {
        when(userRepository.existsByUsernameAndDeletedFalse("alice")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("bcrypt");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });
        var body = json.readTree("{\"userName\":\"alice\",\"active\":true,\"name\":{\"formatted\":\"Alice A\"},"
                + "\"emails\":[{\"value\":\"Alice@Example.com\",\"primary\":true}]}");

        Map<String, Object> resource = users.create(body);

        assertThat(resource)
                .containsEntry("id", "7")
                .containsEntry("userName", "alice")
                .containsEntry("active", true);
        assertThat(resource.get("emails").toString()).contains("alice@example.com");
    }

    @Test
    @DisplayName("复杂 filter（co/and）返回匹配资源")
    void list_complexFilter() {
        when(userRepository.findByDeletedFalseOrderByIdAsc())
                .thenReturn(List.of(user(7L, "alice", "a@b.c", true), user(8L, "bob", "b@b.c", false)));
        when(userRoleRepository.findByUserIdIn(any())).thenReturn(List.of());
        when(roleRepository.findByDeletedFalseOrderByIdAsc()).thenReturn(List.of());

        Map<String, Object> response = users.list("userName co \"ali\" and active eq true", 1, 10);

        assertThat(response).containsEntry("totalResults", 1);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resources = (List<Map<String, Object>>) response.get("Resources");
        assertThat(resources).hasSize(1);
        assertThat(resources.get(0)).containsEntry("userName", "alice");
    }

    @Test
    @DisplayName("PATCH active=false 停用账号")
    void patch_deactivates() throws Exception {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "alice", "a@b.c", true)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var body = json.readTree("{\"Operations\":[{\"op\":\"replace\",\"path\":\"active\",\"value\":false}]}");

        Map<String, Object> resource = users.patch("7", body);

        assertThat(resource).containsEntry("active", false);
    }

    @Test
    @DisplayName("删除用户：软删除并清理角色关联")
    void delete_softDeletes() {
        User user = user(7L, "alice", "a@b.c", true);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        users.delete("7");

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.isEnabled()).isFalse();
        verify(userRoleRepository).deleteByUserId(7L);
    }

    @Test
    @DisplayName("不支持的 filter：400 invalidFilter")
    void list_unsupportedFilter_rejected() {
        assertThatThrownBy(() -> users.list("userName xx \"ali\"", 1, 10))
                .isInstanceOf(ScimException.class)
                .extracting(exception -> ((ScimException) exception).status())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("未知用户：404")
    void get_unknown_rejected() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> users.get("99"))
                .isInstanceOf(ScimException.class)
                .extracting(exception -> ((ScimException) exception).status())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(userRoleRepository, never()).findByUserIdIn(any());
    }
}
