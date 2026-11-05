// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.player;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.system.communication.api.entity.CsConversation;
import dev.strata.system.communication.api.entity.CsMessage;
import dev.strata.system.communication.biz.repository.CsConversationRepository;
import dev.strata.system.communication.biz.repository.CsMessageRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerCsAppService 单元测试")
class PlayerCsAppServiceTest {

    @Mock
    private CsConversationRepository conversations;

    @Mock
    private CsMessageRepository messages;

    @InjectMocks
    private PlayerCsAppService service;

    private CsConversation conversationOf(Long playerId) {
        CsConversation conversation = new CsConversation();
        conversation.setId(11L);
        conversation.setPlayerId(playerId);
        conversation.setStatus("open");
        conversation.setDeleted(false);
        return conversation;
    }

    @Test
    @DisplayName("open 建会话并落第一条玩家消息")
    void open_CreatesConversationAndFirstMessage() {
        when(conversations.save(any(CsConversation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.open(100L, "payment", "title", "help");

        verify(conversations).save(any(CsConversation.class));
        verify(messages).save(any(CsMessage.class));
    }

    @Test
    @DisplayName("messages 访问他人工单被拒")
    void messages_OfOtherPlayer_IsForbidden() {
        when(conversations.findById(11L)).thenReturn(Optional.of(conversationOf(200L)));

        assertThatThrownBy(() -> service.messages(100L, 11L)).isInstanceOf(BusinessException.class);
    }
}
