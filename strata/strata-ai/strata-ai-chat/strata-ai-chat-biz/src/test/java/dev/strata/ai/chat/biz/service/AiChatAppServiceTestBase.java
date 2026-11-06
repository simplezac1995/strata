// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.chat.biz.service;

import dev.strata.ai.chat.api.entity.AiChatSession;
import dev.strata.ai.chat.biz.config.AiChatProperties;
import dev.strata.ai.chat.biz.mapper.AiChatMapper;
import dev.strata.ai.chat.biz.repository.AiChatMessageRepository;
import dev.strata.ai.chat.biz.repository.AiChatSessionRepository;
import dev.strata.ai.config.biz.repository.AiModelRepository;
import dev.strata.ai.config.biz.repository.AiProviderRepository;
import dev.strata.common.security.jwt.JwtPrincipal;
import dev.strata.plugin.api.llm.LlmGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * {@link AiChatAppService} 单测共享夹具：Mock 依赖、认证上下文、无资源事务管理器与实体/LLM 结果构造器。
 * 用例按主题拆到子类（会话 CRUD / 消息流）。
 */
abstract class AiChatAppServiceTestBase {

    protected static final long USER_ID = 7L;

    @Mock
    protected AiChatSessionRepository sessionRepository;

    @Mock
    protected AiChatMessageRepository messageRepository;

    @Mock
    protected AiModelRepository modelRepository;

    @Mock
    protected AiProviderRepository providerRepository;

    @Mock
    protected LlmGateway llmGateway;

    @Mock
    protected AiChatMapper mapper;

    @Mock
    protected Authentication authentication;

    protected AiChatAppService service;

    @BeforeEach
    void setUpAuth() {
        org.mockito.Mockito.lenient()
                .when(authentication.getPrincipal())
                .thenReturn(new JwtPrincipal(USER_ID, "tester"));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // null prompt repository / data source fall back to the default system prompt and no usage persistence,
        // matching what the previous in-service assembly did.
        AiChatToolDispatcher toolDispatcher =
                new AiChatToolDispatcher(null, llmGateway, new AiChatProperties("", 30), null);
        service = new AiChatAppService(
                sessionRepository,
                messageRepository,
                modelRepository,
                providerRepository,
                toolDispatcher,
                llmGateway,
                mapper,
                null,
                new AiChatProperties("", 30),
                noOpTransactionManager());
    }

    /**
     * A transaction manager that runs the {@code TransactionTemplate} callback synchronously with no real
     * resource — the service only needs the programmatic boundaries, and unit tests assert repo/LLM interactions,
     * not isolation semantics.
     */
    protected static PlatformTransactionManager noOpTransactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
                // no-op
            }

            @Override
            public void rollback(TransactionStatus status) {
                // no-op
            }
        };
    }

    /** Build a service whose public attachment origin is overridden, for the absolute-URL tests. */
    protected AiChatAppService serviceWithPublicBase(String publicBaseUrl) {
        AiChatProperties properties = new AiChatProperties(publicBaseUrl, 30);
        return new AiChatAppService(
                sessionRepository,
                messageRepository,
                modelRepository,
                providerRepository,
                new AiChatToolDispatcher(null, llmGateway, properties, null),
                llmGateway,
                mapper,
                null,
                properties,
                noOpTransactionManager());
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    /** Wrap a canned assistant reply in a usage-less {@link LlmChatResult} for gateway stubs. */
    protected static dev.strata.plugin.api.llm.LlmChatResult aiResult(String content) {
        return new dev.strata.plugin.api.llm.LlmChatResult(content, null, null, null, null, null);
    }

    protected AiChatSession session(long id, Long modelId) {
        AiChatSession s = new AiChatSession();
        s.setId(id);
        s.setUserId(USER_ID);
        s.setModelId(modelId);
        s.setTitle("t");
        return s;
    }
}
