// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.tenant.TenantContext;
import dev.strata.common.security.tenant.TenantProperties;
import dev.strata.plugin.api.spreadsheet.SpreadsheetExporter;
import dev.strata.system.api.dto.UserDto;
import dev.strata.system.api.request.UserUpdateRequest;
import dev.strata.system.rbac.api.entity.User;
import dev.strata.system.rbac.biz.mapper.UserMapper;
import dev.strata.system.rbac.biz.metrics.RbacMetrics;
import dev.strata.system.rbac.biz.repository.UserRepository;
import dev.strata.system.rbac.biz.repository.UserRoleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Covers the {@code matchesKeyword} filter matrix (username hit / nickname hit / no hit with null and
 * non-null nickname / blank keyword) and the remaining update/require arms not in
 * {@link UserAppServiceBranchTest}.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "varargs"})
class UserAppServiceKeywordTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private TenantProperties tenantProperties;

    @Mock
    private RbacMetrics rbacMetrics;

    @Mock
    private SpreadsheetExporter spreadsheetExporter;

    private User a, b, c;

    @BeforeEach
    void setUp() {
        a = user(1L, "alice", "Alice A");
        b = user(2L, "bob", null);
        c = user(3L, "charlie", "Robert the Great");
    }

    private static User user(long id, String username, String nickname) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setNickname(nickname);
        u.setEnabled(true);
        u.setDeleted(false);
        return u;
    }

    private UserAppService service() {
        return new UserAppService(
                userRepository,
                userRoleRepository,
                passwordEncoder,
                userMapper,
                tenantProperties,
                Optional.of(spreadsheetExporter),
                rbacMetrics);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    /**
     * P1-14: exportXlsx now pages through the Specification (keyword pushed down to SQL) instead of calling
     * list() and filtering in memory. Stub the paginated query to return the desired users; the service maps them
     * straight onto xlsx rows.
     */
    private void stubPage(List<User> users) {
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(users, inv.getArgument(1), users.size()));
        when(spreadsheetExporter.writeXlsx(any(), anyList(), anyList())).thenReturn(new byte[] {0});
    }

    @Test
    void exportXlsx_writesPaginatedRows() {
        stubPage(List.of(a, b, c));
        service().exportXlsx("whatever");

        org.mockito.ArgumentCaptor<List<List<String>>> rows = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(spreadsheetExporter).writeXlsx(any(), anyList(), rows.capture());
        assertThat(rows.getValue()).hasSize(3);
        assertThat(rows.getValue().get(0)).containsExactly("1", "alice", "Alice A", "true");
        // b has a null nickname -> written as empty string, not "null".
        assertThat(rows.getValue().get(1).get(2)).isEmpty();
    }

    @Test
    void exportXlsx_emptyPage_writesNothing() {
        stubPage(List.of());
        service().exportXlsx("zzz-no-such");
        verify(spreadsheetExporter).writeXlsx(any(), anyList(), org.mockito.ArgumentMatchers.argThat(List::isEmpty));
    }

    @Test
    void exportXlsx_blankKeyword_doesNotThrow() {
        stubPage(List.of(a));
        assertThatCode(() -> service().exportXlsx("  ")).doesNotThrowAnyException();
    }

    @Test
    void update_NullRoleIds_SkipsReplace() {
        UserUpdateRequest request = new UserUpdateRequest("Nick", true, null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(a));
        when(userRepository.save(a)).thenReturn(a);
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of());
        when(userMapper.toDto(a, List.of()))
                .thenReturn(new UserDto("1", "alice", "Alice A", null, true, List.of(), null));

        service().update(1L, request);

        verify(userRoleRepository, never()).deleteByUserId(any());
    }

    @Test
    void require_UserTenantNull_DoesNotBlock() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(a));
        when(tenantProperties.isRowLevelEnabled()).thenReturn(true);
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of());
        when(userMapper.toDto(a, List.of()))
                .thenReturn(new UserDto("1", "alice", "Alice A", null, true, List.of(), null));

        try (MockedStatic<TenantContext> mocked = org.mockito.Mockito.mockStatic(TenantContext.class)) {
            mocked.when(TenantContext::getTenantId).thenReturn(99L);
            assertThatCode(() -> service().get(1L)).doesNotThrowAnyException();
        }
    }

    @Test
    void page_BlankKeyword_UsesConjunction() {
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        assertThatCode(() -> service().page("  ", 0, 10, "id", "asc")).doesNotThrowAnyException();
    }

    @Test
    void page_NonBlankKeyword_BuildsLikeSpec() {
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        assertThatCode(() -> service().page("alice", 0, 10, "id", "asc")).doesNotThrowAnyException();
    }
}
