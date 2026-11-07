// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.system.api.dto.SearchResultsDto;
import dev.strata.system.rbac.api.entity.Role;
import dev.strata.system.rbac.api.entity.User;
import dev.strata.system.rbac.biz.repository.RoleRepository;
import dev.strata.system.rbac.biz.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SearchAppServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private SearchAppService service;

    private static User user(long id, String username, String nickname) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setNickname(nickname);
        return u;
    }

    private static Role role(long id, String code, String name) {
        Role r = new Role();
        r.setId(id);
        r.setCode(code);
        r.setName(name);
        return r;
    }

    @Test
    void search_AggregatesUsersAndRoles() {
        when(userRepository.search(eq("admin"), any(Pageable.class)))
                .thenReturn(List.of(user(1L, "admin", "Administrator"), user(2L, "admin2", null)));
        when(roleRepository.search(eq("admin"), any(Pageable.class)))
                .thenReturn(List.of(role(10L, "ROLE_ADMIN", "管理员")));

        SearchResultsDto result = service.search("admin", 10);

        assertThat(result.users()).hasSize(2);
        assertThat(result.users().get(0).id()).isEqualTo("1");
        assertThat(result.users().get(0).label()).isEqualTo("Administrator");
        assertThat(result.users().get(1).label()).isEqualTo("admin2");
        assertThat(result.roles()).hasSize(1);
        assertThat(result.roles().get(0).subtitle()).isEqualTo("ROLE_ADMIN");
        assertThat(result.orders()).isEmpty();
        assertThat(result.pages()).isNotEmpty();
    }

    @Test
    void search_BlankKeyword_OnlyPages() {
        SearchResultsDto result = service.search("   ", 10);

        assertThat(result.users()).isEmpty();
        assertThat(result.roles()).isEmpty();
        assertThat(result.pages()).isNotEmpty();
    }

    @Test
    void search_LimitTooLarge_Throws() {
        assertThatThrownBy(() -> service.search("x", 100)).isInstanceOf(BusinessException.class);
    }

    @Test
    void search_NonPositiveLimit_Defaults() {
        when(userRepository.search(any(), any(Pageable.class))).thenReturn(List.of());
        when(roleRepository.search(any(), any(Pageable.class))).thenReturn(List.of());

        SearchResultsDto result = service.search("x", 0);

        assertThat(result.users()).isEmpty();
    }

    @Test
    void search_PagesStaticCatalog() {
        SearchResultsDto result = service.search("", 10);
        assertThat(result.pages()).isNotEmpty();
        assertThat(result.pages().stream().anyMatch(p -> p.route().equals("/system/users")))
                .isTrue();
        assertThat(result.pages().stream().anyMatch(p -> p.route().equals("/system/monitor")))
                .isTrue();
    }
}
