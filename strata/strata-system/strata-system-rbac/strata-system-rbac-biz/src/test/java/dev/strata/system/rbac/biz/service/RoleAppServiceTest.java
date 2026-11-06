// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.security.tenant.TenantProperties;
import dev.strata.system.api.dto.RoleDto;
import dev.strata.system.api.request.RoleCreateRequest;
import dev.strata.system.api.request.RoleUpdateRequest;
import dev.strata.system.rbac.api.entity.Role;
import dev.strata.system.rbac.biz.mapper.RoleMapper;
import dev.strata.system.rbac.biz.metrics.RbacMetrics;
import dev.strata.system.rbac.biz.repository.RoleMenuRepository;
import dev.strata.system.rbac.biz.repository.RoleRepository;
import dev.strata.system.rbac.biz.repository.UserRoleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleAppService 单元测试")
@SuppressWarnings({"unchecked", "varargs"})
class RoleAppServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleMenuRepository roleMenuRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private TenantProperties tenantProperties;

    @Mock
    private RbacMetrics rbacMetrics;

    @InjectMocks
    private RoleAppService roleAppService;

    private Role testRole;
    private RoleDto testRoleDto;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setId(1L);
        testRole.setCode("ADMIN");
        testRole.setName("管理员");
        testRole.setEnabled(true);

        testRoleDto = new RoleDto("1", "ADMIN", "管理员", true, List.of("1", "2"));
    }

    @Nested
    @DisplayName("create - 创建角色")
    class CreateRoleTests {

        @Test
        @DisplayName("创建角色成功")
        void createRole_Success() {
            RoleCreateRequest request = new RoleCreateRequest("USER", "普通用户", true, List.of("1"));

            when(roleRepository.findByCodeAndDeletedFalse("USER")).thenReturn(Optional.empty());
            when(roleMapper.toEntity(request)).thenReturn(testRole);
            when(roleRepository.save(any(Role.class))).thenReturn(testRole);
            when(roleMenuRepository.findByRoleId(1L)).thenReturn(List.of());
            when(roleMapper.toDto(any(Role.class), anyList())).thenReturn(testRoleDto);

            RoleDto result = roleAppService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.code()).isEqualTo("ADMIN");
            verify(roleRepository).save(any(Role.class));
            verify(roleMenuRepository).deleteByRoleId(1L);
            verify(roleMenuRepository).saveAll(anyList());
            verify(rbacMetrics).recordRoleCreated();
        }

        @Test
        @DisplayName("角色编码已存在时抛出异常")
        void createRole_CodeExists_ThrowsException() {
            RoleCreateRequest request = new RoleCreateRequest("EXISTING", "已存在", true, List.of());

            when(roleRepository.findByCodeAndDeletedFalse("EXISTING")).thenReturn(Optional.of(testRole));

            assertThatThrownBy(() -> roleAppService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.errorCode()).isEqualTo(ErrorCode.CONFLICT);
                    });

            verify(roleRepository, never()).save(any(Role.class));
        }
    }

    @Nested
    @DisplayName("update - 更新角色")
    class UpdateRoleTests {

        @Test
        @DisplayName("更新角色成功")
        void updateRole_Success() {
            RoleUpdateRequest request = new RoleUpdateRequest("更新后的角色", true, List.of("2", "3"));

            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(roleRepository.save(any(Role.class))).thenReturn(testRole);
            when(roleMenuRepository.findByRoleId(1L)).thenReturn(List.of());
            when(roleMapper.toDto(any(Role.class), anyList())).thenReturn(testRoleDto);

            RoleDto result = roleAppService.update(1L, request);

            assertThat(result).isNotNull();
            verify(roleMapper).update(any(RoleUpdateRequest.class), any(Role.class));
            verify(roleRepository).save(any(Role.class));
            verify(roleMenuRepository).deleteByRoleId(1L);
            verify(roleMenuRepository).saveAll(anyList());
            verify(rbacMetrics).recordRoleUpdated();
        }

        @Test
        @DisplayName("角色不存在时抛出异常")
        void updateRole_NotFound_ThrowsException() {
            RoleUpdateRequest request = new RoleUpdateRequest("更新", true, List.of());

            when(roleRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleAppService.update(999L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("delete - 删除角色")
    class DeleteRoleTests {

        @Test
        @DisplayName("删除角色成功（软删除）")
        void deleteRole_Success() {
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(roleRepository.save(any(Role.class))).thenReturn(testRole);

            roleAppService.delete(1L);

            assertThat(testRole.isDeleted()).isTrue();
            verify(roleRepository).save(testRole);
            verify(rbacMetrics).recordRoleDeleted();
        }

        @Test
        @DisplayName("角色不存在时抛出异常")
        void deleteRole_NotFound_ThrowsException() {
            when(roleRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleAppService.delete(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("get - 查询角色")
    class GetRoleTests {

        @Test
        @DisplayName("根据ID查询角色成功")
        void getRole_Success() {
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(roleMenuRepository.findByRoleId(1L)).thenReturn(List.of());
            when(roleMapper.toDto(any(Role.class), anyList())).thenReturn(testRoleDto);

            RoleDto result = roleAppService.get(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo("1");
            assertThat(result.code()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("角色不存在时抛出异常")
        void getRole_NotFound_ThrowsException() {
            when(roleRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleAppService.get(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("list - 角色列表")
    class ListRolesTests {

        @Test
        @DisplayName("查询角色列表成功")
        void listRoles_Success() {
            Role role2 = new Role();
            role2.setId(2L);
            role2.setCode("USER");
            role2.setName("普通用户");
            role2.setEnabled(true);

            RoleDto dto2 = new RoleDto("2", "USER", "普通用户", true, List.of());

            when(roleRepository.findByDeletedFalseOrderByIdAsc()).thenReturn(List.of(testRole, role2));
            when(roleMenuRepository.findByRoleIdIn(anyList())).thenReturn(List.of());
            when(roleMapper.toDto(any(Role.class), anyList())).thenReturn(testRoleDto, dto2);

            List<RoleDto> result = roleAppService.list();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).code()).isEqualTo("ADMIN");
            assertThat(result.get(1).code()).isEqualTo("USER");
        }
    }

    @Nested
    @DisplayName("page - 分页查询")
    class PageRolesTests {

        @Test
        @DisplayName("分页查询角色成功")
        void pageRoles_Success() {
            Page<Role> rolePage = new PageImpl<>(List.of(testRole));

            when(roleRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(rolePage);
            when(roleMenuRepository.findByRoleIdIn(anyList())).thenReturn(List.of());
            when(roleMapper.toDto(any(Role.class), anyList())).thenReturn(testRoleDto);

            PageResult<RoleDto> result = roleAppService.page("admin", 0, 10, "id", "asc");

            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }
    }
}
