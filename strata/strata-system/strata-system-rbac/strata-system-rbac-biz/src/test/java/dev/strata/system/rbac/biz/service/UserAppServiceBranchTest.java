// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
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
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * UserAppService 分支缺口补充：exportXlsx、空白密码、null 批量删除、租户越权。
 * 与既有 {@link UserAppServiceTest} 互补。
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "varargs"})
class UserAppServiceBranchTest {

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

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEnabled(true);
        testUser.setDeleted(false);
    }

    private UserAppService serviceWithExporter() {
        return new UserAppService(
                userRepository,
                userRoleRepository,
                passwordEncoder,
                userMapper,
                tenantProperties,
                Optional.of(spreadsheetExporter),
                rbacMetrics);
    }

    private UserAppService serviceWithoutExporter() {
        return new UserAppService(
                userRepository,
                userRoleRepository,
                passwordEncoder,
                userMapper,
                tenantProperties,
                Optional.empty(),
                rbacMetrics);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void exportXlsx_NoExporter_ThrowsNotFound() {
        assertThatThrownBy(() -> serviceWithoutExporter().exportXlsx(null))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex -> assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void exportXlsx_WithExporter_NullNickname() {
        testUser.setNickname(null);
        // P1-14: export pages through the Specification and maps rows straight from the User entity.
        when(userRepository.findAll(
                        any(org.springframework.data.jpa.domain.Specification.class),
                        any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(testUser)));
        when(spreadsheetExporter.writeXlsx(any(), anyList(), anyList())).thenReturn(new byte[] {1, 2, 3});

        byte[] result = serviceWithExporter().exportXlsx(null);

        assertThat(result).hasSize(3);
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<List<String>>> rows = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(spreadsheetExporter).writeXlsx(any(), anyList(), rows.capture());
        // columns: id, username, nickname, enabled — null nickname must be written as empty string.
        assertThat(rows.getValue().get(0).get(2)).isEmpty();
    }

    @Test
    void update_BlankPassword_DoesNotEncode() {
        UserUpdateRequest request = new UserUpdateRequest("New Nick", true, "   ", List.of());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of());
        when(userMapper.toDto(any(User.class), anyList()))
                .thenReturn(new UserDto("1", "testuser", "New Nick", null, true, List.of(), null));

        serviceWithExporter().update(1L, request);

        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void deleteBatch_NullList_Noop() {
        serviceWithExporter().deleteBatch(null);

        verify(userRepository, never()).findById(any());
    }

    @Test
    void get_TenantMismatch_ThrowsNotFound() {
        testUser.setTenantId(7L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tenantProperties.isRowLevelEnabled()).thenReturn(true);

        try (MockedStatic<TenantContext> mocked = org.mockito.Mockito.mockStatic(TenantContext.class)) {
            mocked.when(TenantContext::getTenantId).thenReturn(99L);

            assertThatThrownBy(() -> serviceWithExporter().get(1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Test
    void get_TenantMatch_Returns() {
        testUser.setTenantId(7L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tenantProperties.isRowLevelEnabled()).thenReturn(true);
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of());
        when(userMapper.toDto(any(User.class), anyList()))
                .thenReturn(new UserDto("1", "testuser", null, null, true, List.of(), null));

        try (MockedStatic<TenantContext> mocked = org.mockito.Mockito.mockStatic(TenantContext.class)) {
            mocked.when(TenantContext::getTenantId).thenReturn(7L);

            assertThatCode(() -> serviceWithExporter().get(1L)).doesNotThrowAnyException();
        }
    }

    @Test
    void get_TenantContextNull_DoesNotBlock() {
        testUser.setTenantId(7L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tenantProperties.isRowLevelEnabled()).thenReturn(true);
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of());
        when(userMapper.toDto(any(User.class), anyList()))
                .thenReturn(new UserDto("1", "testuser", null, null, true, List.of(), null));

        try (MockedStatic<TenantContext> mocked = org.mockito.Mockito.mockStatic(TenantContext.class)) {
            mocked.when(TenantContext::getTenantId).thenReturn(null);

            assertThatCode(() -> serviceWithExporter().get(1L)).doesNotThrowAnyException();
        }
    }
}
