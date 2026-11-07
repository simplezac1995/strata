// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Staff-side customer-service workbench. Conversations and messages are plain rows for now (no WebSocket
 * push on this batch); the player-facing H5 surface is a separate chain and is out of scope here.
 */
@Service
public class CsConversationAppService {

    private static final Logger log = LoggerFactory.getLogger(CsConversationAppService.class);

    private static final String STATUS_OPEN = "open";
    private static final String STATUS_ASSIGNED = "assigned";
    private static final String STATUS_CLOSED = "closed";
    private static final String SENDER_STAFF = "staff";

    private final CsConversationRepository conversationRepository;
    private final CsMessageRepository messageRepository;
    private final CsConversationMapper conversationMapper;
    private final CsMessageMapper messageMapper;

    public CsConversationAppService(
            CsConversationRepository conversationRepository,
            CsMessageRepository messageRepository,
            CsConversationMapper conversationMapper,
            CsMessageMapper messageMapper) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<ConversationDto> page(String status, int page, int size) {
        Sort sort = Sort.by(Sort.Order.desc("lastMessageAt"), Sort.Order.desc("id"));
        Page<CsConversation> result = (status == null || status.isBlank())
                ? conversationRepository.findByDeletedFalse(PageRequests.of(page, size, sort))
                : conversationRepository.findByStatusAndDeletedFalse(status, PageRequests.of(page, size, sort));
        return PageResult.from(result.map(conversationMapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<CsMessageDto> messages(Long conversationId) {
        require(conversationId);
        return messageRepository.findByConversationIdOrderByIdAsc(conversationId).stream()
                .map(messageMapper::toDto)
                .toList();
    }

    @OperationLog(module = "system:cs", action = "create")
    @Transactional
    public ConversationDto open(Long playerId) {
        CsConversation conversation = new CsConversation();
        conversation.setPlayerId(playerId);
        conversation.setStatus(STATUS_OPEN);
        conversation.setDeleted(false);
        conversationRepository.save(conversation);
        return conversationMapper.toDto(conversation);
    }

    @OperationLog(module = "system:cs", action = "create")
    @Transactional
    public CsMessageDto reply(Long conversationId, Long staffId, CsReplyRequest request) {
        CsConversation conversation = require(conversationId);
        Instant now = Instant.now();

        CsMessage message = new CsMessage();
        message.setConversationId(conversationId);
        message.setSenderType(SENDER_STAFF);
        message.setContent(request.content());
        message.setAttachments(request.attachments());
        messageRepository.save(message);

        conversation.setStaffId(staffId);
        conversation.setStatus(STATUS_ASSIGNED);
        conversation.setLastMessageAt(now);
        conversationRepository.save(conversation);
        return messageMapper.toDto(message);
    }

    @OperationLog(module = "system:cs", action = "update")
    @Transactional
    public ConversationDto close(Long conversationId) {
        CsConversation conversation = require(conversationId);
        conversation.setStatus(STATUS_CLOSED);
        conversationRepository.save(conversation);
        return conversationMapper.toDto(conversation);
    }

    private CsConversation require(Long conversationId) {
        return conversationRepository
                .findById(conversationId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
    }
}
