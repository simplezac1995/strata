// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.chat.biz.service;

import dev.strata.ai.api.request.AiChatAttachment;
import dev.strata.ai.api.request.AiChatMessageCreateRequest;
import dev.strata.ai.api.request.AiChatSessionCreateRequest;
import dev.strata.ai.chat.api.dto.AiChatMessageDto;
import dev.strata.ai.chat.api.dto.AiChatSessionDto;
import dev.strata.ai.chat.api.entity.AiChatMessage;
import dev.strata.ai.chat.api.entity.AiChatSession;
import dev.strata.ai.chat.biz.config.AiChatProperties;
import dev.strata.ai.chat.biz.mapper.AiChatMapper;
import dev.strata.ai.chat.biz.repository.AiChatMessageRepository;
import dev.strata.ai.chat.biz.repository.AiChatSessionRepository;
import dev.strata.ai.config.api.entity.AiModel;
import dev.strata.ai.config.api.entity.AiProvider;
import dev.strata.ai.config.biz.repository.AiModelRepository;
import dev.strata.ai.config.biz.repository.AiProviderRepository;
import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.security.jwt.JwtPrincipal;
import dev.strata.plugin.api.llm.LlmChatResult;
import dev.strata.plugin.api.llm.LlmGateway;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AiChatAppService {

    private static final Logger log = LoggerFactory.getLogger(AiChatAppService.class);

    /** Safety cap on sessions returned by the unscoped list endpoint (P2-22). */
    private static final int MAX_SESSIONS_RETURNED = 100;

    private final AiChatSessionRepository aiChatSessionRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final AiModelRepository aiModelRepository;
    private final AiProviderRepository aiProviderRepository;
    private final LlmGateway llmGateway;
    private final AiChatMapper aiChatMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AiChatProperties chatProperties;
    /** Tool-call prompt assembly, parsing and dispatch, split out so this service stays focused on chat turns. */
    private final AiChatToolDispatcher toolDispatcher;
    /** Short read-only txn for loading session + history; short write txn for persisting the exchange. */
    private final TransactionTemplate readOnlyTx;

    private final TransactionTemplate writeTx;

    public AiChatAppService(
            AiChatSessionRepository aiChatSessionRepository,
            AiChatMessageRepository aiChatMessageRepository,
            AiModelRepository aiModelRepository,
            AiProviderRepository aiProviderRepository,
            AiChatToolDispatcher toolDispatcher,
            LlmGateway llmGateway,
            AiChatMapper aiChatMapper,
            ApplicationEventPublisher eventPublisher,
            AiChatProperties chatProperties,
            PlatformTransactionManager transactionManager) {
        this.aiChatSessionRepository = aiChatSessionRepository;
        this.aiChatMessageRepository = aiChatMessageRepository;
        this.aiModelRepository = aiModelRepository;
        this.aiProviderRepository = aiProviderRepository;
        this.llmGateway = llmGateway;
        this.aiChatMapper = aiChatMapper;
        this.eventPublisher = eventPublisher;
        this.chatProperties = chatProperties;
        this.toolDispatcher = toolDispatcher;
        TransactionTemplate readOnly = new TransactionTemplate(transactionManager);
        readOnly.setReadOnly(true);
        readOnly.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        this.readOnlyTx = readOnly;
        this.writeTx = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public List<AiChatSessionDto> listSessions() {
        Long userId = currentUserId();
        // P2-22: cap the unbounded list at the most recent MAX_SESSIONS_RETURNED sessions.
        return aiChatSessionRepository.findTop100ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(aiChatMapper::toSessionDto)
                .toList();
    }

    @Transactional
    @OperationLog(module = "ai:chat", action = "create")
    public AiChatSessionDto createSession(AiChatSessionCreateRequest request) {
        AiChatSession session = new AiChatSession();
        session.setTitle(request.title());
        session.setUserId(currentUserId());
        if (request.modelId() != null && !request.modelId().isBlank()) {
            session.setModelId(Long.parseLong(request.modelId()));
        } else {
            aiModelRepository.findFirstByEnabledTrueOrderByIdAsc().ifPresent(m -> session.setModelId(m.getId()));
        }
        aiChatSessionRepository.save(session);
        return aiChatMapper.toSessionDto(session);
    }

    @Transactional
    @OperationLog(module = "ai:chat", action = "update")
    public AiChatSessionDto renameSession(Long id, String title) {
        AiChatSession session = requireSession(id);
        if (title != null && !title.isBlank()) {
            session.setTitle(title.trim());
            aiChatSessionRepository.save(session);
        }
        return aiChatMapper.toSessionDto(session);
    }

    @Transactional
    @OperationLog(module = "ai:chat", action = "delete")
    public void deleteSession(Long id) {
        AiChatSession session = requireSession(id);
        aiChatMessageRepository.deleteBySessionId(session.getId());
        aiChatSessionRepository.delete(session);
    }

    @Transactional(readOnly = true)
    public List<AiChatMessageDto> listMessages(Long sessionId) {
        requireSession(sessionId);
        // P1-13: the history endpoint is bounded to the same window fed to the model.
        List<AiChatMessage> recent = latestHistory(sessionId);
        List<AiChatMessage> ascending = new ArrayList<>(recent);
        Collections.reverse(ascending);
        return ascending.stream().map(aiChatMapper::toMessageDto).toList();
    }

    /**
     * Sends the user's message to the model and persists the exchange.
     *
     * <p><b>P1-5 transaction split:</b> a chat turn wraps a remote LLM HTTP call that can take seconds. Holding a
     * DB connection open for that whole call — as a single {@code @Transactional} did — pins a connection from a
     * small pool across a network round-trip and can exhaust the pool under concurrent chats. The turn now runs
     * three bounded steps instead:
     *
     * <ol>
     *   <li>a short <b>read-only</b> txn loads the session, its model/provider and the recent history window;
     *   <li>the {@code llmGateway.chat(...)} call happens <b>outside any transaction</b>, holding no connection;
     *   <li>a short <b>write</b> txn persists the user and assistant messages together (still atomic with each
     *       other, only the LLM call is excluded).
     * </ol>
     *
     * Caller-visible semantics are unchanged: a missing session/model/provider still rejects before any LLM call,
     * and the assistant reply is returned.
     */
    @OperationLog(module = "ai:chat", action = "create")
    public AiChatMessageDto sendMessage(Long sessionId, AiChatMessageCreateRequest request) {
        LoadedChat loaded = readOnlyTx.execute(status -> loadForSend(sessionId));

        String content = request.content() == null ? "" : request.content().trim();
        List<AiChatAttachment> attachments = request.attachments() == null ? List.of() : request.attachments();
        if (content.isBlank() && attachments.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Message content or attachments must not be empty");
        }

        String assistantContent;
        try {
            String systemPrompt = toolDispatcher.withToolsManifest(toolDispatcher.resolveSystemPrompt());

            // Confirmation approval from the UI: execute the pending write tool directly without re-calling the LLM.
            String[] approval = toolDispatcher.parseApproval(content);
            if (approval != null) {
                String result = toolDispatcher.dispatchTool(approval[0], approval[1]);
                assistantContent = "✅ 已执行 [" + approval[0] + "]:\n" + result;
                return writeTx.execute(
                        status -> persistExchange(loaded.sessionId(), content, attachments, assistantContent));
            }

            LlmChatResult llmResult = llmGateway.chatWithUsage(LlmGatewaySupport.toCommand(
                    loaded.provider(),
                    loaded.model(),
                    loaded.history(),
                    content,
                    attachments,
                    chatProperties.publicBaseUrl(),
                    systemPrompt));
            toolDispatcher.recordUsage(loaded, llmResult, "chat", sessionId);
            assistantContent =
                    toolDispatcher.maybeExecuteTool(llmResult.content(), loaded, content, attachments, systemPrompt);
        } catch (RuntimeException failure) {
            // P2-13: the user's message must survive an LLM outage. Without this catch a thrown gateway error
            // leaves the user's prompt unsaved (the write tx below never runs) and looks like the message never
            // reached us. Persist the prompt plus an assistant error placeholder in its own write tx.
            log.error(
                    "LLM chat failed for session {}; persisting user message with an error placeholder",
                    sessionId,
                    failure);
            return writeTx.execute(status -> persistErrorExchange(loaded.sessionId(), content, attachments));
        }

        return writeTx.execute(status -> persistExchange(loaded.sessionId(), content, attachments, assistantContent));
    }

    /**
     * Streaming chat: returns an SSE emitter that pushes content chunks as the LLM generates them.
     * The full response is persisted when the stream completes.
     */
    @OperationLog(module = "ai:chat", action = "create")
    public SseEmitter sendMessageStream(Long sessionId, AiChatMessageCreateRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        LoadedChat loaded = readOnlyTx.execute(status -> loadForSend(sessionId));
        String content = request.content() == null ? "" : request.content().trim();
        List<AiChatAttachment> attachments = request.attachments() == null ? List.of() : request.attachments();
        if (content.isBlank() && attachments.isEmpty()) {
            emitter.completeWithError(new BusinessException(ErrorCode.BAD_REQUEST, "Message must not be empty"));
            return emitter;
        }

        SecurityContext ctx = SecurityContextHolder.getContext();
        Thread.startVirtualThread(() -> {
            SecurityContextHolder.setContext(ctx);
            try {
                // Confirmation approval: execute the pending write tool directly without calling the LLM.
                String[] approval = toolDispatcher.parseApproval(content);
                if (approval != null) {
                    String executed = toolDispatcher.dispatchTool(approval[0], approval[1]);
                    String result = "✅ 已执行 [" + approval[0] + "]:\n" + executed;
                    emitter.send(SseEmitter.event().name("delta").data(result));
                    String finalContent = result;
                    writeTx.execute(status -> persistExchange(loaded.sessionId(), content, attachments, finalContent));
                    emitter.complete();
                    return;
                }
                StringBuilder full = new StringBuilder();
                String systemPrompt = toolDispatcher.withToolsManifest(toolDispatcher.resolveSystemPrompt());
                LlmChatResult result = llmGateway.chatStream(
                        LlmGatewaySupport.toCommand(
                                loaded.provider(),
                                loaded.model(),
                                loaded.history(),
                                content,
                                attachments,
                                chatProperties.publicBaseUrl(),
                                systemPrompt),
                        chunk -> {
                            try {
                                emitter.send(SseEmitter.event().name("delta").data(chunk));
                                full.append(chunk);
                            } catch (Exception e) {
                                log.debug("SSE send failed: {}", e.getMessage());
                            }
                        });
                toolDispatcher.recordUsage(loaded, result, "chat", sessionId);
                String finalContent = full.toString();
                // If the streamed reply contains a [TOOL:...] marker, execute it and stream the final answer.
                if (finalContent.matches("(?s).*\\[TOOL:[a-z_]+.*")) {
                    try {
                        String toolResult = toolDispatcher.maybeExecuteTool(
                                finalContent, loaded, content, attachments, systemPrompt);
                        // Push the final natural-language answer as additional deltas.
                        for (int i = 0; i < toolResult.length(); i += 80) {
                            String chunk = toolResult.substring(i, Math.min(i + 80, toolResult.length()));
                            emitter.send(SseEmitter.event().name("delta").data(chunk));
                        }
                        finalContent = toolResult;
                    } catch (Exception toolEx) {
                        log.error("Stream tool execution failed", toolEx);
                    }
                }
                String persisted = finalContent;
                writeTx.execute(status -> persistExchange(loaded.sessionId(), content, attachments, persisted));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception ex) {
                log.error("Stream chat failed", ex);
                try {
                    writeTx.execute(status -> persistErrorExchange(loaded.sessionId(), content, attachments));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    /** Read the session, its model/provider and the bounded recent-history window inside a short read-only txn. */
    private LoadedChat loadForSend(Long sessionId) {
        AiChatSession session = requireSession(sessionId);
        if (session.getModelId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Chat session has no model configured");
        }
        AiModel model = aiModelRepository
                .findById(session.getModelId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "AI model not found"));
        AiProvider provider = aiProviderRepository
                .findById(model.getProviderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "AI provider not found"));

        List<AiChatMessage> recent = new ArrayList<>(latestHistory(sessionId));
        Collections.reverse(recent);
        return new LoadedChat(session.getId(), provider, model, recent);
    }

    /** The {@code historyLimit} most-recent messages, newest first. */
    private List<AiChatMessage> latestHistory(Long sessionId) {
        int limit = Math.max(1, chatProperties.historyLimit());
        return aiChatMessageRepository.findBySessionIdOrderByCreatedAtDesc(sessionId, PageRequest.of(0, limit));
    }

    /** Persists the user message and the assistant reply in one short write txn, returning the reply DTO. */
    private AiChatMessageDto persistExchange(
            Long sessionId, String content, List<AiChatAttachment> attachments, String assistantContent) {
        saveUserMessage(sessionId, content, attachments);

        AiChatMessage assistantMsg = new AiChatMessage();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(assistantContent);
        aiChatMessageRepository.save(assistantMsg);

        return aiChatMapper.toMessageDto(assistantMsg);
    }

    /**
     * Persists the user prompt plus an assistant error placeholder after the LLM call threw (P2-13). The user's
     * message stays in history and the placeholder tells them to retry, rather than the prompt vanishing.
     */
    private AiChatMessageDto persistErrorExchange(Long sessionId, String content, List<AiChatAttachment> attachments) {
        saveUserMessage(sessionId, content, attachments);

        AiChatMessage assistantMsg = new AiChatMessage();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent("LLM unavailable, please retry");
        aiChatMessageRepository.save(assistantMsg);

        return aiChatMapper.toMessageDto(assistantMsg);
    }

    private void saveUserMessage(Long sessionId, String content, List<AiChatAttachment> attachments) {
        AiChatMessage userMsg = new AiChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(persistableContent(content, attachments));
        aiChatMessageRepository.save(userMsg);
    }

    private static String persistableContent(String content, List<AiChatAttachment> attachments) {
        if (!content.isBlank()) {
            return content;
        }
        String names = attachments.stream()
                .map(a -> a.fileName() != null && !a.fileName().isBlank() ? a.fileName() : a.key())
                .collect(java.util.stream.Collectors.joining(", "));
        return "Attached: " + names;
    }

    private AiChatSession requireSession(Long id) {
        return aiChatSessionRepository
                .findByIdAndUserId(id, currentUserId())
                .orElseThrow(() -> new NotFoundException("Chat session not found"));
    }

    private static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Not authenticated");
        }
        return principal.userId();
    }
}
