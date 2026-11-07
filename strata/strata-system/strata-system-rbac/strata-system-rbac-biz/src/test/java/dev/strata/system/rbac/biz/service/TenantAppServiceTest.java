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
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.TenantDto;
import dev.strata.system.api.request.TenantCreateRequest;
import dev.strata.system.api.request.TenantUpdateRequest;
import dev.strata.system.rbac.api.entity.Tenant;
import dev.strata.system.rbac.biz.mapper.TenantMapper;
import dev.strata.system.rbac.biz.repository.TenantRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TenantAppServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantMapper tenantMapper;

    @InjectMocks
    private TenantAppService tenantAppService;

    private Tenant tenant(long id, String code) {
        Tenant t = new Tenant();
        t.setId(id);
        t.setCode(code);
        t.setName("Tenant " + code);
        t.setDomain(code + ".example.com");
        t.setStatus("active");
        t.setPlan("pro");
        return t;
    }

    private TenantDto dto() {
        return new TenantDto("1", "acme", "Acme", "acme.example.com", "active", "pro");
    }

    @Nested
    class QueryTests {

        @Test
        void list_MapsAll() {
            when(tenantRepository.findAllByOrderByIdAsc()).thenReturn(List.of(tenant(1L, "acme")));
            when(tenantMapper.toDto(any(Tenant.class))).thenReturn(dto());

            List<TenantDto> result = tenantAppService.list();

            assertThat(result).hasSize(1);
        }

        @Test
        void page_ReturnsPaged() {
            when(tenantRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(tenant(1L, "acme"))));
            when(tenantMapper.toDto(any(Tenant.class))).thenReturn(dto());

            PageResult<TenantDto> result = tenantAppService.page(0, 10);

            assertThat(result.content()).hasSize(1);
        }

        @Test
        void get_Found() {
            when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant(1L, "acme")));
            when(tenantMapper.toDto(any(Tenant.class))).thenReturn(dto());

            TenantDto result = tenantAppService.get(1L);

            assertThat(result.code()).isEqualTo("acme");
        }

        @Test
        void get_NotFound_Throws() {
            when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenantAppService.get(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    class CreateTests {

        @Test
        void create_CodeFree_Success() {
            TenantCreateRequest request = new TenantCreateRequest("acme", "Acme", "acme.example.com", "active", "pro");
            Tenant entity = tenant(0L, "acme");
            when(tenantRepository.existsByCode("acme")).thenReturn(false);
            when(tenantMapper.toEntity(request)).thenReturn(entity);
            when(tenantRepository.save(entity)).thenReturn(entity);
            when(tenantMapper.toDto(entity)).thenReturn(dto());

            TenantDto result = tenantAppService.create(request);

            assertThat(result.code()).isEqualTo("acme");
            verify(tenantRepository).save(entity);
        }

        @Test
        void create_DuplicateCode_Throws() {
            TenantCreateRequest request = new TenantCreateRequest("dup", "Dup", null, "active", "free");
            when(tenantRepository.existsByCode("dup")).thenReturn(true);

            assertThatThrownBy(() -> tenantAppService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.CONFLICT));

            verify(tenantRepository, never()).save(any(Tenant.class));
        }
    }

    @Nested
    class UpdateTests {

        @Test
        void update_Success() {
            Tenant existing = tenant(1L, "acme");
            TenantUpdateRequest request = new TenantUpdateRequest("Acme Inc", "acme.new.com", "active", "free");
            when(tenantRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(tenantRepository.save(existing)).thenReturn(existing);
            when(tenantMapper.toDto(existing)).thenReturn(dto());

            TenantDto result = tenantAppService.update(1L, request);

            assertThat(result.code()).isEqualTo("acme");
            verify(tenantMapper).update(eq(request), any(Tenant.class));
            verify(tenantRepository).save(existing);
        }

        @Test
        void update_NotFound_Throws() {
            TenantUpdateRequest request = new TenantUpdateRequest("X", null, "active", "free");
            when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenantAppService.update(99L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(tenantRepository, never()).save(any(Tenant.class));
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void delete_Success() {
            when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant(1L, "acme")));

            tenantAppService.delete(1L);

            verify(tenantRepository).delete(any(Tenant.class));
        }

        @Test
        void delete_NotFound_Throws() {
            when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenantAppService.delete(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(tenantRepository, never()).delete(any(Tenant.class));
        }
    }
}
