// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.entity;

import dev.strata.common.core.util.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigInteger;
import java.time.Instant;

/**
 * One row in the per-call LLM usage ledger ({@code ai_llm_usage}): the token counts reported by a provider
 * for a single chat completion. {@code action} tags the caller ({@code chat}/{@code office}/{@code qa}/
 * {@code sql}/{@code agent}); {@code sessionId} is nullable and ties office/tool calls back to a chat session
 * when one exists. {@code costMinor} is minor units (1/100) and defaults to 0 until a per-model price table
 * lands.
 */
@Entity
@Table(
        name = "ai_llm_usage",
        indexes = {
            @Index(name = "idx_ai_llm_usage_provider_created", columnList = "provider, created_at"),
            @Index(name = "idx_ai_llm_usage_session", columnList = "session_id"),
            @Index(name = "idx_ai_llm_usage_action_created", columnList = "action, created_at")
        })
public class AiLlmUsage {

    @Id
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(nullable = false, length = 64)
    private String provider;

    @Column(nullable = false, length = 128)
    private String model;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "prompt_tokens", nullable = false)
    private int promptTokens;

    @Column(name = "completion_tokens", nullable = false)
    private int completionTokens;

    @Column(name = "total_tokens", nullable = false)
    private int totalTokens;

    @Column(name = "cost_minor", nullable = false)
    private BigInteger costMinor = BigInteger.ZERO;

    @Column(nullable = false, length = 32)
    private String action;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = IdGenerator.next();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    public BigInteger getCostMinor() {
        return costMinor;
    }

    public void setCostMinor(BigInteger costMinor) {
        this.costMinor = costMinor;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
