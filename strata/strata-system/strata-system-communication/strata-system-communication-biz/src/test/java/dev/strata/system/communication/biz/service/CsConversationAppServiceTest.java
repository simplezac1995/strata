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
import dev.strata.system.api.dto.ConversationDto;
import dev.strata.system.api.dto.CsMessageDto;
import dev.strata.system.api.request.CsReplyRequest;
import dev.strata.system.communication.api.entity.CsConversation;
import dev.strata.system.communication.api.entity.CsMessage;
import dev.strata.system.communication.biz.mapper.CsConversationMapper;
import dev.strata.system.communication.biz.mapper.CsMessageMapper;
import dev.strata.system.communication.biz.repository.CsConversationRepository;
import dev.strata.system.communication.biz.repository.CsMessageRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CsConversationAppServiceTest {

    @Mock
    private CsConversationRepository conversationRepository;

    @Mock
    private CsMessageRepository messageRepository;

    @Mock
    private CsConversationMapper conversationMapper;

    @Mock
    private CsMessageMapper messageMapper;

    @InjectMocks
    private CsConversationAppService service;

    private ConversationDto convDto() {
        return new ConversationDto("1", "100", "200", "open", Instant.now(), Instant.now(), Instant.now());
    }

    private CsMessageDto msgDto() {
        return new CsMessageDto("9", "1", "staff", "hi", null, null, Instant.now());
    }

    @Test
    void page_NoStatus_ListsAll() {
        when(conversationRepository.findByDeletedFalse(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new CsConversation())));
        when(conversationMapper.toDto(any(CsConversation.class))).thenReturn(convDto());

        PageResult<ConversationDto> result = service.page(null, 0, 10);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void messages_NotFound_Throws() {
        when(conversationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.messages(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex -> assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void open_CreatesOpenConversation() {
        CsConversation saved = new CsConversation();
        when(conversationRepository.save(any(CsConversation.class))).thenReturn(saved);
        when(conversationMapper.toDto(any(CsConversation.class))).thenReturn(convDto());

        ConversationDto result = service.open(100L);

        assertThat(result.status()).isEqualTo("open");
        verify(conversationRepository).save(any(CsConversation.class));
    }

    @Test
    void reply_PersistsMessageAndAssigns() {
        CsConversation conversation = new CsConversation();
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(CsMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conversationRepository.save(conversation)).thenReturn(conversation);
        when(messageMapper.toDto(any(CsMessage.class))).thenReturn(msgDto());

        CsMessageDto result = service.reply(1L, 200L, new CsReplyRequest("hi", null));

        assertThat(result.senderType()).isEqualTo("staff");
        assertThat(conversation.getStatus()).isEqualTo("assigned");
        assertThat(conversation.getStaffId()).isEqualTo(200L);
    }

    @Test
    void close_MarksClosed() {
        CsConversation conversation = new CsConversation();
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(conversation)).thenReturn(conversation);
        when(conversationMapper.toDto(conversation)).thenReturn(convDto());

        service.close(1L);

        assertThat(conversation.getStatus()).isEqualTo("closed");
    }

    @Test
    void close_NotFound_Throws() {
        when(conversationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.close(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex -> assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

        verify(conversationRepository, never()).save(any(CsConversation.class));
    }
}
