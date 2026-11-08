// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.demo;

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
import dev.strata.system.api.dto.ProductDto;
import dev.strata.system.api.dto.ProductQuery;
import dev.strata.system.api.entity.DemoProduct;
import dev.strata.system.api.request.ProductCreateRequest;
import dev.strata.system.api.request.ProductUpdateRequest;
import dev.strata.system.biz.repository.DemoProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "varargs"})
class ProductAppServiceTest {

    @Mock
    private DemoProductRepository demoProductRepository;

    @Mock
    private ProductMapper productMapper;

    private ProductAppService service;

    private DemoProduct product;
    private ProductDto productDto;

    @BeforeEach
    void setUp() {
        service = new ProductAppService(demoProductRepository, productMapper);

        product = new DemoProduct();
        product.setId(7L);
        product.setName("Keyboard");
        product.setSku("KB-001");
        product.setPrice(new BigDecimal("99.50"));
        product.setStock(10);
        product.setDescription("Mechanical keyboard");
        product.setEnabled(true);

        productDto =
                new ProductDto("7", "Keyboard", "KB-001", new BigDecimal("99.50"), 10, "Mechanical keyboard", true);
    }

    @Nested
    class PageTests {

        @Test
        void page_WithFilters_ReturnsMappedPage() {
            Page<DemoProduct> repoPage = new PageImpl<>(List.of(product));
            when(demoProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(repoPage);
            when(productMapper.toDto(product)).thenReturn(productDto);

            ProductQuery query = new ProductQuery("key", "KB-001", true, 0, 20);
            PageResult<ProductDto> result = service.page(query);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0)).isEqualTo(productDto);
            assertThat(result.totalElements()).isEqualTo(1);
        }

        @Test
        void page_NullFilters_ReturnsEmptyPage() {
            when(demoProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            ProductQuery query = new ProductQuery(null, null, null, null, null);
            PageResult<ProductDto> result = service.page(query);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }

        @Test
        void page_NegativePaging_DefaultsApplied() {
            when(demoProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            service.page(new ProductQuery(null, null, null, -5, -10));

            verify(demoProductRepository).findAll(any(Specification.class), captor.capture());
            Pageable pageable = captor.getValue();
            assertThat(pageable.getPageNumber()).isZero();
            assertThat(pageable.getPageSize()).isEqualTo(20);
        }
    }

    @Nested
    class GetTests {

        @Test
        void get_Exists_ReturnsDto() {
            when(demoProductRepository.findById(7L)).thenReturn(Optional.of(product));
            when(productMapper.toDto(product)).thenReturn(productDto);

            ProductDto result = service.get(7L);

            assertThat(result).isEqualTo(productDto);
        }

        @Test
        void get_NotExists_ThrowsNotFound() {
            when(demoProductRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(404L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    class CreateTests {

        @Test
        void create_SkuAvailable_Succeeds() {
            ProductCreateRequest request =
                    new ProductCreateRequest("Mouse", "MS-001", new BigDecimal("25.00"), 5, "Wireless", true);
            when(demoProductRepository.existsBySku("MS-001")).thenReturn(false);
            when(productMapper.toEntity(request)).thenReturn(product);
            when(demoProductRepository.save(product)).thenReturn(product);
            when(productMapper.toDto(product)).thenReturn(productDto);

            ProductDto result = service.create(request);

            assertThat(result).isEqualTo(productDto);
            verify(demoProductRepository).save(product);
        }

        @Test
        void create_SkuExists_ThrowsConflict() {
            ProductCreateRequest request =
                    new ProductCreateRequest("Dup", "DUP-1", new BigDecimal("1.00"), 1, null, true);
            when(demoProductRepository.existsBySku("DUP-1")).thenReturn(true);

            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.CONFLICT));

            verify(demoProductRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateTests {

        @Test
        void update_Exists_Succeeds() {
            ProductUpdateRequest request =
                    new ProductUpdateRequest("Keyboard Pro", "KB-001", new BigDecimal("109.00"), 8, null, true);
            when(demoProductRepository.findById(7L)).thenReturn(Optional.of(product));
            when(demoProductRepository.existsBySkuAndIdNot("KB-001", 7L)).thenReturn(false);
            when(demoProductRepository.save(product)).thenReturn(product);
            when(productMapper.toDto(product)).thenReturn(productDto);

            ProductDto result = service.update(7L, request);

            assertThat(result).isEqualTo(productDto);
            verify(productMapper).update(eq(request), eq(product));
            verify(demoProductRepository).save(product);
        }

        @Test
        void update_NotExists_ThrowsNotFound() {
            ProductUpdateRequest request = new ProductUpdateRequest("x", "SKU", new BigDecimal("1.00"), 1, null, true);
            when(demoProductRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(404L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        void update_SkuConflicts_ThrowsConflict() {
            ProductUpdateRequest request =
                    new ProductUpdateRequest("Keyboard", "KB-OTHER", new BigDecimal("99.50"), 10, null, true);
            when(demoProductRepository.findById(7L)).thenReturn(Optional.of(product));
            when(demoProductRepository.existsBySkuAndIdNot("KB-OTHER", 7L)).thenReturn(true);

            assertThatThrownBy(() -> service.update(7L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.CONFLICT));

            verify(demoProductRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void delete_Exists_Succeeds() {
            when(demoProductRepository.findById(7L)).thenReturn(Optional.of(product));

            service.delete(7L);

            verify(demoProductRepository).delete(product);
        }

        @Test
        void delete_NotExists_ThrowsNotFound() {
            when(demoProductRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(404L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(demoProductRepository, never()).delete(any(DemoProduct.class));
        }
    }

    @Nested
    class ListTests {

        @Test
        void list_ReturnsMappedList() {
            when(demoProductRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                    .thenReturn(List.of(product));
            when(productMapper.toDto(product)).thenReturn(productDto);

            List<ProductDto> result = service.list();

            assertThat(result).containsExactly(productDto);
        }

        @Test
        void list_Empty_ReturnsEmptyList() {
            when(demoProductRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                    .thenReturn(List.of());

            List<ProductDto> result = service.list();

            assertThat(result).isNotNull().isEmpty();
        }
    }
}
