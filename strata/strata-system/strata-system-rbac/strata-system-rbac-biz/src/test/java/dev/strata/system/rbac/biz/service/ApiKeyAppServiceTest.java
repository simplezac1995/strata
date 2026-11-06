// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.ApiKeyCreatedDto;
import dev.strata.system.api.dto.ApiKeyDto;
import dev.strata.system.api.dto.ApiKeySummaryDto;
import dev.strata.system.api.request.ApiKeyCreateRequest;
import dev.strata.system.api.request.ApiKeyUpdateRequest;
import dev.strata.system.rbac.api.entity.ApiKey;
import dev.strata.system.rbac.biz.mapper.ApiKeyMapper;
import dev.strata.system.rbac.biz.repository.ApiKeyRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyAppService 单元测试")
class ApiKeyAppServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ApiKeyMapper apiKeyMapper;

    @InjectMocks
    private ApiKeyAppService apiKeyAppService;

    private ApiKey sampleKey(boolean enabled, boolean deleted) {
        ApiKey key = new ApiKey();
        key.setId(7L);
        key.setName("ci-key");
        key.setKeyPrefix("sk_abc123");
        key.setKeyHash("deadbeef");
        key.setScopes("read:api");
        key.setEnabled(enabled);
        key.setDeleted(deleted);
        return key;
    }

    private ApiKeyDto sampleDto() {
        return new ApiKeyDto("7", "ci-key", "sk_abc123", "read:api", true, null, null);
    }

    @Nested
    @DisplayName("list / page / summary - 查询")
    class QueryTests {

        @Test
        @DisplayName("list 返回全部未删除密钥的 DTO")
        void list_MapsAll() {
            when(apiKeyRepository.findByDeletedFalseOrderByIdAsc()).thenReturn(List.of(sampleKey(true, false)));
            when(apiKeyMapper.toDto(any(ApiKey.class))).thenReturn(sampleDto());

            List<ApiKeyDto> result = apiKeyAppService.list();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("ci-key");
        }

        @Test
        @DisplayName("list 空集合返回空列表")
        void list_Empty_ReturnsEmpty() {
            when(apiKeyRepository.findByDeletedFalseOrderByIdAsc()).thenReturn(List.of());

            List<ApiKeyDto> result = apiKeyAppService.list();

            assertThat(result).isEmpty();
            verify(apiKeyMapper, never()).toDto(any(ApiKey.class));
        }

        @Test
        @DisplayName("page 分页查询返回 DTO 页")
        void page_ReturnsPagedDtos() {
            when(apiKeyRepository.findByDeletedFalse(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleKey(true, false))));
            when(apiKeyMapper.toDto(any(ApiKey.class))).thenReturn(sampleDto());

            PageResult<ApiKeyDto> result = apiKeyAppService.page(0, 10);

            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("summary 汇总总数与启用数")
        void summary_ReportsTotalAndActive() {
            when(apiKeyRepository.countByDeletedFalse()).thenReturn(5L);
            when(apiKeyRepository.countByDeletedFalseAndEnabledTrue()).thenReturn(3L);

            ApiKeySummaryDto result = apiKeyAppService.summary();

            assertThat(result.total()).isEqualTo(5L);
            assertThat(result.active()).isEqualTo(3L);
        }

        @Test
        @DisplayName("get 按 id 查询成功")
        void get_Found_ReturnsDto() {
            when(apiKeyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(sampleKey(true, false)));
            when(apiKeyMapper.toDto(any(ApiKey.class))).thenReturn(sampleDto());

            ApiKeyDto result = apiKeyAppService.get(7L);

            assertThat(result.id()).isEqualTo("7");
        }

        @Test
        @DisplayName("get 不存在抛出 NOT_FOUND")
        void get_NotFound_Throws() {
            when(apiKeyRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> apiKeyAppService.get(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("create - 创建密钥")
    class CreateTests {

        @Test
        @DisplayName("创建成功返回明文一次性密钥并落库哈希")
        void create_Success_ReturnsPlaintextAndStoresHash() {
            when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> {
                ApiKey k = inv.getArgument(0);
                k.setId(42L);
                return k;
            });

            ApiKeyCreatedDto result = apiKeyAppService.create(new ApiKeyCreateRequest("deploy", "write:*", true));

            assertThat(result.id()).isEqualTo("42");
            assertThat(result.name()).isEqualTo("deploy");
            assertThat(result.plaintextKey()).startsWith("sk_");
            assertThat(result.keyPrefix()).isEqualTo(result.plaintextKey().substring(0, 10));
            assertThat(result.plaintextKey()).hasSizeGreaterThan(10);
            assertThat(result.enabled()).isTrue();

            ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
            verify(apiKeyRepository).save(captor.capture());
            ApiKey saved = captor.getValue();
            assertThat(saved.getKeyPrefix()).hasSize(10);
            assertThat(saved.getKeyHash()).isNotBlank();
            assertThat(saved.getName()).isEqualTo("deploy");
            assertThat(saved.getScopes()).isEqualTo("write:*");
        }

        @Test
        @DisplayName("enabled 为 null 时默认启用")
        void create_NullEnabled_DefaultsEnabled() {
            when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> {
                ApiKey k = inv.getArgument(0);
                k.setId(1L);
                return k;
            });

            ApiKeyCreatedDto result = apiKeyAppService.create(new ApiKeyCreateRequest("n", "s", null));

            assertThat(result.enabled()).isTrue();
        }

        @Test
        @DisplayName("enabled 为 false 时保持禁用")
        void create_ExplicitDisabled() {
            when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> {
                ApiKey k = inv.getArgument(0);
                k.setId(1L);
                return k;
            });

            ApiKeyCreatedDto result = apiKeyAppService.create(new ApiKeyCreateRequest("n", "s", false));

            assertThat(result.enabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("update - 更新密钥")
    class UpdateTests {

        @Test
        @DisplayName("提供全部字段时更新并落库")
        void update_AllFields_Updates() {
            ApiKey existing = sampleKey(true, false);
            when(apiKeyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(existing));
            when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(existing);
            when(apiKeyMapper.toDto(any(ApiKey.class))).thenReturn(sampleDto());

            apiKeyAppService.update(7L, new ApiKeyUpdateRequest("renamed", "read:only", false));

            assertThat(existing.getName()).isEqualTo("renamed");
            assertThat(existing.getScopes()).isEqualTo("read:only");
            assertThat(existing.isEnabled()).isFalse();
            verify(apiKeyRepository).save(existing);
        }

        @Test
        @DisplayName("name 为空白时不覆盖名称")
        void update_BlankName_KeepsName() {
            ApiKey existing = sampleKey(true, false);
            existing.setName("kept");
            when(apiKeyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(existing));
            when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(existing);
            when(apiKeyMapper.toDto(any(ApiKey.class))).thenReturn(sampleDto());

            apiKeyAppService.update(7L, new ApiKeyUpdateRequest("   ", "sc", true));

            assertThat(existing.getName()).isEqualTo("kept");
        }

        @Test
        @DisplayName("scopes/enabled 为 null 时不覆盖")
        void update_NullOptional_KeepsFields() {
            ApiKey existing = sampleKey(true, false);
            existing.setScopes("keep:scopes");
            when(apiKeyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(existing));
            when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(existing);
            when(apiKeyMapper.toDto(any(ApiKey.class))).thenReturn(sampleDto());

            apiKeyAppService.update(7L, new ApiKeyUpdateRequest("new", null, null));

            assertThat(existing.getScopes()).isEqualTo("keep:scopes");
            assertThat(existing.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("更新不存在的密钥抛出 NOT_FOUND")
        void update_NotFound_Throws() {
            when(apiKeyRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> apiKeyAppService.update(99L, new ApiKeyUpdateRequest("n", "s", true)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(apiKeyRepository, never()).save(any(ApiKey.class));
        }
    }

    @Nested
    @DisplayName("delete - 删除密钥")
    class DeleteTests {

        @Test
        @DisplayName("删除成功执行软删除")
        void delete_SoftDeletes() {
            ApiKey existing = sampleKey(true, false);
            when(apiKeyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(existing));
            when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(existing);

            apiKeyAppService.delete(7L);

            assertThat(existing.isDeleted()).isTrue();
            verify(apiKeyRepository).save(existing);
        }

        @Test
        @DisplayName("删除不存在的密钥抛出 NOT_FOUND")
        void delete_NotFound_Throws() {
            when(apiKeyRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> apiKeyAppService.delete(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(apiKeyRepository, never()).save(any(ApiKey.class));
        }
    }
}
