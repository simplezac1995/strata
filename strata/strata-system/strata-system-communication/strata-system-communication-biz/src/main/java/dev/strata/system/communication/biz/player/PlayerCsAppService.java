// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.player;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.communication.api.entity.CsConversation;
import dev.strata.system.communication.api.entity.CsMessage;
import dev.strata.system.communication.biz.repository.CsConversationRepository;
import dev.strata.system.communication.biz.repository.CsMessageRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Player-facing customer-service tickets on the JWT player REST surface. Self-scoped: a player can only open,
 * list and read their own conversations, and only ever writes messages as {@code player}.
 *
 * <p>{@code category}/{@code title} are accepted on open but not yet persisted (the {@code cs_conversation} table
 * has no such columns); the first message carries the body.
 */
@Service
public class PlayerCsAppService {

    private static final String STATUS_OPEN = "open";
    private static final String SENDER_PLAYER = "player";

    private final CsConversationRepository conversations;
    private final CsMessageRepository messages;

    public PlayerCsAppService(CsConversationRepository conversations, CsMessageRepository messages) {
        this.conversations = conversations;
        this.messages = messages;
    }

    /** Opens a new ticket and posts the player's first message. */
    @Transactional
    public TicketView open(Long playerId, String category, String title, String content) {
        Instant now = Instant.now();
        CsConversation conversation = new CsConversation();
        conversation.setPlayerId(playerId);
        conversation.setStatus(STATUS_OPEN);
        conversation.setDeleted(false);
        conversation.setLastMessageAt(now);
        conversations.save(conversation);

        // TODO: persist category/title once cs_conversation gains the columns.
        postMessage(conversation.getId(), content);
        return toTicket(conversation);
    }

    /** The player's own tickets, newest first. */
    @Transactional(readOnly = true)
    public PageResult<TicketView> myTickets(Long playerId, int page, int size) {
        Sort sort = Sort.by(Sort.Order.desc("lastMessageAt"), Sort.Order.desc("id"));
        Page<CsConversation> result = conversations.findByPlayerIdAndDeletedFalseOrderByLastMessageAtDesc(
                playerId, PageRequests.of(page, size, sort));
        return PageResult.from(result.map(this::toTicket));
    }

    /** The messages of one of the player's tickets. */
    @Transactional(readOnly = true)
    public List<TicketMessageView> messages(Long playerId, Long ticketId) {
        CsConversation conversation = requireOwned(playerId, ticketId);
        return messages.findByConversationIdOrderByIdAsc(conversation.getId()).stream()
                .map(this::toMessage)
                .toList();
    }

    /** Appends a player message to one of the player's tickets. */
    @Transactional
    public TicketMessageView reply(Long playerId, Long ticketId, String content) {
        CsConversation conversation = requireOwned(playerId, ticketId);
        TicketMessageView view = postMessage(conversation.getId(), content);
        conversation.setLastMessageAt(Instant.now());
        conversations.save(conversation);
        return view;
    }

    private TicketMessageView postMessage(Long conversationId, String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Message content is required");
        }
        CsMessage message = new CsMessage();
        message.setConversationId(conversationId);
        message.setSenderType(SENDER_PLAYER);
        message.setContent(content.trim());
        messages.save(message);
        return toMessage(message);
    }

    private CsConversation requireOwned(Long playerId, Long ticketId) {
        CsConversation conversation = conversations
                .findById(ticketId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new NotFoundException("Ticket " + ticketId + " not found"));
        if (!conversation.getPlayerId().equals(playerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Ticket " + ticketId + " does not belong to you");
        }
        return conversation;
    }

    private TicketView toTicket(CsConversation conversation) {
        return new TicketView(conversation.getId(), conversation.getStatus(), conversation.getLastMessageAt());
    }

    private TicketMessageView toMessage(CsMessage message) {
        return new TicketMessageView(
                message.getId(), message.getSenderType(), message.getContent(), message.getCreatedAt());
    }

    /** One ticket row. */
    public record TicketView(Long id, String status, Instant lastMessageAt) {}

    /** One ticket message. */
    public record TicketMessageView(Long id, String senderType, String content, Instant createdAt) {}
}
