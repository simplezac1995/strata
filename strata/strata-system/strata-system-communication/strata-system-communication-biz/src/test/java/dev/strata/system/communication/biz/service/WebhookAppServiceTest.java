// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.service;

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
import dev.strata.system.api.dto.WebhookDto;
import dev.strata.system.api.request.WebhookCreateRequest;
import dev.strata.system.api.request.WebhookUpdateRequest;
import dev.strata.system.communication.api.entity.Webhook;
import dev.strata.system.communication.biz.mapper.WebhookMapper;
import dev.strata.system.communication.biz.repository.WebhookRepository;
import java.util.List;
import java.util.Optional;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookAppService 单元测试")
class WebhookAppServiceTest {

    @Mock
    private WebhookRepository webhookRepository;

    @Mock
    private WebhookMapper webhookMapper;

    @InjectMocks
    private WebhookAppService webhookAppService;

    private Webhook wh(long id, String secret) {
        Webhook w = new Webhook();
        w.setId(id);
        w.setName("deploy");
        w.setUrl("https://example.com/hook");
        w.setSecret(secret);
        w.setEnabled(true);
        return w;
    }

    private WebhookDto dto() {
        return new WebhookDto("1", "deploy", "https://example.com/hook", null, true);
    }

    @Nested
    @DisplayName("list / page / get - 查询")
    class QueryTests {

        @Test
        @DisplayName("list 返回全部 webhook")
        void list_MapsAll() {
            when(webhookRepository.findAllByOrderByIdAsc()).thenReturn(List.of(wh(1L, "hash")));
            when(webhookMapper.toDto(any(Webhook.class))).thenReturn(dto());

            List<WebhookDto> result = webhookAppService.list();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("page 分页查询")
        void page_ReturnsPaged() {
            when(webhookRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(wh(1L, "hash"))));
            when(webhookMapper.toDto(any(Webhook.class))).thenReturn(dto());

            PageResult<WebhookDto> result = webhookAppService.page(0, 10);

            assertThat(result.content()).hasSize(1);
        }

        @Test
        @DisplayName("get 存在时返回 DTO")
        void get_Found() {
            when(webhookRepository.findById(1L)).thenReturn(Optional.of(wh(1L, "hash")));
            when(webhookMapper.toDto(any(Webhook.class))).thenReturn(dto());

            WebhookDto result = webhookAppService.get(1L);

            assertThat(result.name()).isEqualTo("deploy");
        }

        @Test
        @DisplayName("get 不存在抛出 NOT_FOUND")
        void get_NotFound_Throws() {
            when(webhookRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> webhookAppService.get(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("create - 创建 webhook")
    class CreateTests {

        @Test
        @DisplayName("提供密钥时只存哈希")
        void create_WithCommand_StoresHash() {
            WebhookCreateRequest request =
                    new WebhookCreateRequest("deploy", "https://example.com/hook", "plain", true);
            Webhook entity = wh(0L, "stub");
            when(webhookMapper.toEntity(request)).thenReturn(entity);
            when(webhookRepository.save(entity)).thenReturn(entity);
            when(webhookMapper.toDto(entity)).thenReturn(dto());

            webhookAppService.create(request);

            assertThat(entity.getSecret()).isEqualTo(DigestUtils.sha256Hex("plain"));
        }

        @Test
        @DisplayName("密钥为 null 时不覆盖 mapper 生成值")
        void create_NullSecret_KeepsMapperValue() {
            WebhookCreateRequest request = new WebhookCreateRequest("deploy", "https://example.com/hook", null, true);
            Webhook entity = wh(0L, "mapper-set");
            when(webhookMapper.toEntity(request)).thenReturn(entity);
            when(webhookRepository.save(entity)).thenReturn(entity);
            when(webhookMapper.toDto(entity)).thenReturn(dto());

            webhookAppService.create(request);

            assertThat(entity.getSecret()).isEqualTo("mapper-set");
        }
    }

    @Nested
    @DisplayName("update - 更新 webhook")
    class UpdateTests {

        @Test
        @DisplayName("提供新密钥时轮换哈希")
        void update_NewSecret_RotatesHash() {
            Webhook existing = wh(1L, "old-hash");
            WebhookUpdateRequest request =
                    new WebhookUpdateRequest("deploy", "https://example.com/hook", "new-plain", true);
            when(webhookRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(webhookRepository.save(existing)).thenReturn(existing);
            when(webhookMapper.toDto(existing)).thenReturn(dto());

            webhookAppService.update(1L, request);

            assertThat(existing.getSecret()).isEqualTo(DigestUtils.sha256Hex("new-plain"));
            verify(webhookMapper).update(eq(request), any(Webhook.class));
        }

        @Test
        @DisplayName("密钥为空白时保持原哈希")
        void update_BlankSecret_KeepsHash() {
            Webhook existing = wh(1L, "old-hash");
            WebhookUpdateRequest request = new WebhookUpdateRequest("deploy", "https://example.com/hook", "  ", true);
            when(webhookRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(webhookRepository.save(existing)).thenReturn(existing);
            when(webhookMapper.toDto(existing)).thenReturn(dto());

            webhookAppService.update(1L, request);

            assertThat(existing.getSecret()).isEqualTo("old-hash");
        }

        @Test
        @DisplayName("更新不存在的 webhook 抛出 NOT_FOUND")
        void update_NotFound_Throws() {
            WebhookUpdateRequest request = new WebhookUpdateRequest("n", "u", null, true);
            when(webhookRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> webhookAppService.update(99L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(webhookRepository, never()).save(any(Webhook.class));
        }
    }

    @Nested
    @DisplayName("delete - 删除 webhook")
    class DeleteTests {

        @Test
        @DisplayName("删除成功")
        void delete_Success() {
            when(webhookRepository.findById(1L)).thenReturn(Optional.of(wh(1L, "hash")));

            webhookAppService.delete(1L);

            verify(webhookRepository).delete(any(Webhook.class));
        }

        @Test
        @DisplayName("删除不存在的 webhook 抛出 NOT_FOUND")
        void delete_NotFound_Throws() {
            when(webhookRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> webhookAppService.delete(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }
}
