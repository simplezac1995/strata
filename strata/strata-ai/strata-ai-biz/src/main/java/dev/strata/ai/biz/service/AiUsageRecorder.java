// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.service;

import dev.strata.ai.biz.entity.AiLlmUsage;
import dev.strata.ai.biz.repository.AiLlmUsageRepository;
import dev.strata.plugin.api.llm.LlmChatResult;
import java.math.BigInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes one {@code ai_llm_usage} row per LLM call. Every caller that invokes an {@code LlmGateway} should
 * hand the result here so token usage is metered even when the caller's own transaction rolls back.
 *
 * <p>Runs in a <b>REQUIRES_NEW</b> transaction: usage accounting must survive the surrounding business txn
 * (e.g. an office-generation failure), and a ledger insert failure must not abort the user-facing call.
 */
@Service
public class AiUsageRecorder {

    private static final Logger log = LoggerFactory.getLogger(AiUsageRecorder.class);

    private final AiLlmUsageRepository usageRepository;

    public AiUsageRecorder(AiLlmUsageRepository usageRepository) {
        this.usageRepository = usageRepository;
    }

    /**
     * Persist one usage row.
     *
     * @param provider provider code (e.g. {@code deepseek})
     * @param model model code actually used
     * @param sessionId optional chat session id (nullable)
     * @param action caller tag ({@code chat}/{@code office}/{@code qa}/{@code sql}/{@code agent})
     * @param result the gateway result carrying token counts; never {@code null}
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String provider, String model, String sessionId, String action, LlmChatResult result) {
        try {
            AiLlmUsage row = new AiLlmUsage();
            row.setProvider(provider == null ? "unknown" : provider);
            row.setModel(model == null ? "unknown" : model);
            row.setSessionId(sessionId);
            row.setAction(action == null ? "chat" : action);
            row.setPromptTokens(result.promptTokensOrZero());
            row.setCompletionTokens(result.completionTokensOrZero());
            row.setTotalTokens(result.totalTokensOrZero());
            // TODO: compute from a per-model price table once billing prices are configured.
            row.setCostMinor(BigInteger.ZERO);
            usageRepository.save(row);
        } catch (RuntimeException ex) {
            // Metering must never break the user-facing LLM call; log and move on.
            log.warn("Failed to record LLM usage for provider={} model={} action={}", provider, model, action, ex);
        }
    }
}
