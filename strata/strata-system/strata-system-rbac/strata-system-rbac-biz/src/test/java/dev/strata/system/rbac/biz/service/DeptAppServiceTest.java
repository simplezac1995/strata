// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.tenant.TenantContext;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.security.tenant.TenantProperties;
import dev.strata.system.api.dto.DeptDto;
import dev.strata.system.api.request.DeptCreateRequest;
import dev.strata.system.api.request.DeptUpdateRequest;
import dev.strata.system.rbac.api.entity.Dept;
import dev.strata.system.rbac.biz.mapper.DeptMapper;
import dev.strata.system.rbac.biz.repository.DeptRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class DeptAppServiceTest {

    @Mock
    private DeptRepository deptRepository;

    @Mock
    private DeptMapper deptMapper;

    @Mock
    private TenantProperties tenantProperties;

    @InjectMocks
    private DeptAppService deptAppService;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    private Dept dept(Long id, Long parentId, String name, int sortOrder) {
        Dept d = new Dept();
        d.setId(id);
        d.setParentId(parentId);
        d.setName(name);
        d.setSortOrder(sortOrder);
        d.setEnabled(true);
        return d;
    }

    private DeptDto dto(Long id, String parentId, int sortOrder) {
        return new DeptDto(String.valueOf(id), parentId, "dept-" + id, sortOrder, true, new java.util.ArrayList<>());
    }

    @Nested
    class QueryTests {

        @Test
        void list_BuildsTree() {
            Dept root = dept(1L, null, "root", 1);
            Dept child = dept(2L, 1L, "child", 2);
            Dept orphan = dept(3L, 99L, "orphan", 3);
            when(deptRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(root, child, orphan));
            when(deptMapper.toDto(root)).thenReturn(dto(1L, null, 1));
            when(deptMapper.toDto(child)).thenReturn(dto(2L, "1", 2));
            when(deptMapper.toDto(orphan)).thenReturn(dto(3L, "99", 3));

            List<DeptDto> result = deptAppService.list();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo("1");
            assertThat(result.get(0).children()).hasSize(1);
            assertThat(result.get(0).children().get(0).id()).isEqualTo("2");
            assertThat(result.get(1).id()).isEqualTo("3");
        }

        @Test
        void listChildren_NullParent_ReturnsRoots() {
            when(deptRepository.findByParentIdIsNullOrderBySortOrderAscIdAsc())
                    .thenReturn(List.of(dept(1L, null, "root", 1)));
            when(deptMapper.toDto(any(Dept.class))).thenReturn(dto(1L, null, 1));

            List<DeptDto> result = deptAppService.listChildren(null);

            assertThat(result).hasSize(1);
            verify(deptRepository, never()).findByParentIdOrderBySortOrderAscIdAsc(any());
        }

        @Test
        void listChildren_WithParent_ReturnsChildren() {
            when(deptRepository.findByParentIdOrderBySortOrderAscIdAsc(1L))
                    .thenReturn(List.of(dept(2L, 1L, "child", 2)));
            when(deptMapper.toDto(any(Dept.class))).thenReturn(dto(2L, "1", 2));

            List<DeptDto> result = deptAppService.listChildren(1L);

            assertThat(result).hasSize(1);
            verify(deptRepository, never()).findByParentIdIsNullOrderBySortOrderAscIdAsc();
        }

        @Test
        void pageFlat_ReturnsPaged() {
            when(deptRepository.search(any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(dept(1L, null, "root", 1))));
            when(deptMapper.toDto(any(Dept.class))).thenReturn(dto(1L, null, 1));

            PageResult<DeptDto> result = deptAppService.pageFlat("root", 0, 10);

            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }

        @Test
        void get_Found() {
            when(deptRepository.findById(1L)).thenReturn(Optional.of(dept(1L, null, "root", 1)));
            when(deptMapper.toDto(any(Dept.class))).thenReturn(dto(1L, null, 1));

            DeptDto result = deptAppService.get(1L);

            assertThat(result.id()).isEqualTo("1");
        }

        @Test
        void get_NotFound_Throws() {
            when(deptRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deptAppService.get(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    class CreateTests {

        @Test
        void create_NoParent_Success() {
            DeptCreateRequest request = new DeptCreateRequest(null, "eng", 1, true);
            Dept entity = dept(null, null, "eng", 1);
            when(deptMapper.toEntity(request)).thenReturn(entity);
            when(deptRepository.save(entity)).thenReturn(entity);
            when(deptMapper.toDto(entity)).thenReturn(dto(1L, null, 1));

            DeptDto result = deptAppService.create(request);

            assertThat(result.name()).isEqualTo("dept-1");
            verify(deptRepository, never()).findById(any());
            verify(deptRepository).save(entity);
        }

        @Test
        void create_WithParent_ValidatesParent() {
            DeptCreateRequest request = new DeptCreateRequest("5", "sub", 1, true);
            Dept parent = dept(5L, null, "parent", 0);
            Dept entity = dept(null, 5L, "sub", 1);
            when(deptRepository.findById(5L)).thenReturn(Optional.of(parent));
            when(deptMapper.toEntity(request)).thenReturn(entity);
            when(deptRepository.save(entity)).thenReturn(entity);
            when(deptMapper.toDto(entity)).thenReturn(dto(2L, "5", 1));

            deptAppService.create(request);

            verify(deptRepository).findById(5L);
            verify(deptRepository).save(entity);
        }

        @Test
        void create_ParentMissing_Throws() {
            DeptCreateRequest request = new DeptCreateRequest("404", "sub", 1, true);
            when(deptRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deptAppService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(deptRepository, never()).save(any(Dept.class));
        }

        @Test
        void create_RowLevelOn_AppliesTenantId() {
            TenantContext.setTenantId(42L);
            when(tenantProperties.isRowLevelEnabled()).thenReturn(true);
            DeptCreateRequest request = new DeptCreateRequest(null, "eng", 1, true);
            Dept entity = dept(null, null, "eng", 1);
            when(deptMapper.toEntity(request)).thenReturn(entity);
            when(deptRepository.save(entity)).thenReturn(entity);
            when(deptMapper.toDto(entity)).thenReturn(dto(1L, null, 1));

            deptAppService.create(request);

            assertThat(entity.getTenantId()).isEqualTo(42L);
        }
    }

    @Nested
    class UpdateTests {

        @Test
        void update_Success() {
            Dept existing = dept(1L, null, "old", 1);
            DeptUpdateRequest request = new DeptUpdateRequest(null, "new", 2, true);
            when(deptRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(deptRepository.save(existing)).thenReturn(existing);
            when(deptMapper.toDto(existing)).thenReturn(dto(1L, null, 2));

            DeptDto result = deptAppService.update(1L, request);

            assertThat(result.id()).isEqualTo("1");
            verify(deptMapper).update(eq(request), any(Dept.class));
            verify(deptRepository).save(existing);
        }

        @Test
        void update_SelfParent_ThrowsBadRequest() {
            Dept existing = dept(1L, null, "self", 1);
            DeptUpdateRequest request = new DeptUpdateRequest("1", "self", 1, true);
            when(deptRepository.findById(1L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> deptAppService.update(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));

            verify(deptRepository, never()).save(any(Dept.class));
        }

        @Test
        void update_NewParent_MustExist() {
            Dept existing = dept(1L, null, "child", 1);
            DeptUpdateRequest request = new DeptUpdateRequest("77", "child", 1, true);
            when(deptRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(deptRepository.findById(77L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deptAppService.update(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(deptRepository, never()).save(any(Dept.class));
        }

        @Test
        void update_NotFound_Throws() {
            DeptUpdateRequest request = new DeptUpdateRequest(null, "x", 1, true);
            when(deptRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deptAppService.update(99L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void delete_NoChildren_Success() {
            when(deptRepository.findById(1L)).thenReturn(Optional.of(dept(1L, null, "leaf", 1)));
            when(deptRepository.countByParentId(1L)).thenReturn(0L);

            deptAppService.delete(1L);

            verify(deptRepository).deleteById(1L);
        }

        @Test
        void delete_HasChildren_ThrowsConflict() {
            when(deptRepository.findById(1L)).thenReturn(Optional.of(dept(1L, null, "parent", 1)));
            when(deptRepository.countByParentId(1L)).thenReturn(2L);

            assertThatThrownBy(() -> deptAppService.delete(1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.CONFLICT));

            verify(deptRepository, never()).deleteById(any());
        }

        @Test
        void delete_NotFound_Throws() {
            when(deptRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deptAppService.delete(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    class BuildTreeTests {

        @Test
        void buildTree_SortsAndNests() {
            Dept root = dept(1L, null, "root", 5);
            Dept childB = dept(2L, 1L, "childB", 9);
            Dept childA = dept(3L, 1L, "childA", 3);
            when(deptMapper.toDto(root)).thenReturn(dto(1L, null, 5));
            when(deptMapper.toDto(childB)).thenReturn(dto(2L, "1", 9));
            when(deptMapper.toDto(childA)).thenReturn(dto(3L, "1", 3));

            List<DeptDto> roots = deptAppService.buildTree(List.of(root, childB, childA));

            assertThat(roots).hasSize(1);
            assertThat(roots.get(0).children()).extracting(DeptDto::id).containsExactly("3", "2");
        }
    }
}
