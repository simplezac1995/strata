// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import dev.strata.system.api.user.UserLookup.UserReference;
import dev.strata.system.rbac.api.entity.User;
import dev.strata.system.rbac.biz.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserLookupAppService 单元测试")
class UserLookupAppServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserLookupAppService userLookupAppService;

    @Test
    @DisplayName("按用户名查到未删除用户时返回引用")
    void findByUsername_Found_ReturnsReference() {
        User user = new User();
        user.setId(123L);
        user.setUsername("alice");
        when(userRepository.findByUsernameAndDeletedFalse("alice")).thenReturn(Optional.of(user));

        Optional<UserReference> result = userLookupAppService.findByUsername("alice");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(123L);
        assertThat(result.get().username()).isEqualTo("alice");
    }

    @Test
    @DisplayName("用户不存在或已删除时返回空")
    void findByUsername_Missing_ReturnsEmpty() {
        when(userRepository.findByUsernameAndDeletedFalse("ghost")).thenReturn(Optional.empty());

        Optional<UserReference> result = userLookupAppService.findByUsername("ghost");

        assertThat(result).isEmpty();
    }
}
