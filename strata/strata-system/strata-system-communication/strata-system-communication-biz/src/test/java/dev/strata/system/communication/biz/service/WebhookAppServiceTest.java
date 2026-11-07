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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
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
    class QueryTests {

        @Test
        void list_MapsAll() {
            when(webhookRepository.findAllByOrderByIdAsc()).thenReturn(List.of(wh(1L, "hash")));
            when(webhookMapper.toDto(any(Webhook.class))).thenReturn(dto());

            List<WebhookDto> result = webhookAppService.list();

            assertThat(result).hasSize(1);
        }

        @Test
        void page_ReturnsPaged() {
            when(webhookRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(wh(1L, "hash"))));
            when(webhookMapper.toDto(any(Webhook.class))).thenReturn(dto());

            PageResult<WebhookDto> result = webhookAppService.page(0, 10);

            assertThat(result.content()).hasSize(1);
        }

        @Test
        void get_Found() {
            when(webhookRepository.findById(1L)).thenReturn(Optional.of(wh(1L, "hash")));
            when(webhookMapper.toDto(any(Webhook.class))).thenReturn(dto());

            WebhookDto result = webhookAppService.get(1L);

            assertThat(result.name()).isEqualTo("deploy");
        }

        @Test
        void get_NotFound_Throws() {
            when(webhookRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> webhookAppService.get(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    class CreateTests {

        @Test
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
    class UpdateTests {

        @Test
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
    class DeleteTests {

        @Test
        void delete_Success() {
            when(webhookRepository.findById(1L)).thenReturn(Optional.of(wh(1L, "hash")));

            webhookAppService.delete(1L);

            verify(webhookRepository).delete(any(Webhook.class));
        }

        @Test
        void delete_NotFound_Throws() {
            when(webhookRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> webhookAppService.delete(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }
}
