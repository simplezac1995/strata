// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.MessageChannelDto;
import dev.strata.system.api.request.MessageChannelUpdateRequest;
import dev.strata.system.communication.api.entity.MessageChannel;
import dev.strata.system.communication.biz.mapper.MessageChannelMapper;
import dev.strata.system.communication.biz.repository.MessageChannelRepository;
import java.util.List;
import java.util.Optional;
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
@DisplayName("MessageChannelAppService 单元测试")
class MessageChannelAppServiceTest {

    @Mock
    private MessageChannelRepository messageChannelRepository;

    @Mock
    private MessageChannelMapper messageChannelMapper;

    @InjectMocks
    private MessageChannelAppService messageChannelAppService;

    private MessageChannel channel(String type) {
        MessageChannel c = new MessageChannel();
        c.setId(1L);
        c.setChannelType(type);
        c.setConfigJson("{}");
        c.setEnabled(true);
        return c;
    }

    private MessageChannelDto dto() {
        return new MessageChannelDto("1", "email", "{}", true);
    }

    @Nested
    @DisplayName("list / page - 查询")
    class QueryTests {

        @Test
        @DisplayName("list 返回全部通道")
        void list_MapsAll() {
            when(messageChannelRepository.findAllByOrderByIdAsc()).thenReturn(List.of(channel("email")));
            when(messageChannelMapper.toDto(any(MessageChannel.class))).thenReturn(dto());

            List<MessageChannelDto> result = messageChannelAppService.list();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("page 分页查询")
        void page_ReturnsPaged() {
            when(messageChannelRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(channel("email"))));
            when(messageChannelMapper.toDto(any(MessageChannel.class))).thenReturn(dto());

            PageResult<MessageChannelDto> result = messageChannelAppService.page(0, 10);

            assertThat(result.content()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("updateByType - 按类型更新")
    class UpdateByTypeTests {

        @Test
        @DisplayName("更新存在通道的配置与启用状态")
        void updateByType_Found_AppliesNonNullFields() {
            MessageChannel existing = channel("email");
            when(messageChannelRepository.findByChannelType("email")).thenReturn(Optional.of(existing));
            when(messageChannelRepository.save(existing)).thenReturn(existing);
            when(messageChannelMapper.toDto(existing)).thenReturn(dto());

            messageChannelAppService.updateByType("email", new MessageChannelUpdateRequest("{\"host\":\"x\"}", false));

            assertThat(existing.getConfigJson()).isEqualTo("{\"host\":\"x\"}");
            assertThat(existing.isEnabled()).isFalse();
            verify(messageChannelRepository).save(existing);
        }

        @Test
        @DisplayName("字段为 null 时不覆盖")
        void updateByType_NullFields_Kept() {
            MessageChannel existing = channel("email");
            existing.setConfigJson("{\"kept\":true}");
            when(messageChannelRepository.findByChannelType("email")).thenReturn(Optional.of(existing));
            when(messageChannelRepository.save(existing)).thenReturn(existing);
            when(messageChannelMapper.toDto(existing)).thenReturn(dto());

            messageChannelAppService.updateByType("email", new MessageChannelUpdateRequest(null, null));

            assertThat(existing.getConfigJson()).isEqualTo("{\"kept\":true}");
            assertThat(existing.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("通道类型不存在抛出 NOT_FOUND")
        void updateByType_Missing_Throws() {
            when(messageChannelRepository.findByChannelType("sms")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                            messageChannelAppService.updateByType("sms", new MessageChannelUpdateRequest("{}", true)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(messageChannelRepository, never()).save(any(MessageChannel.class));
        }
    }
}
