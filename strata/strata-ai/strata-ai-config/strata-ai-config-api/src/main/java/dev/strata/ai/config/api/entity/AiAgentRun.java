// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.api.entity;

import dev.strata.common.core.util.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One execution of an agent's graph ({@code ai_agent_run}). {@code status} is {@code running}/{@code completed}/
 * {@code failed}; {@code context} accumulates per-node outputs as a JSON object; {@code error} records the
 * failure message when the run did not complete.
 */
@Entity
@Table(
        name = "ai_agent_run",
        indexes = {@Index(name = "idx_ai_agent_run_agent", columnList = "agent_id")})
public class AiAgentRun {

    @Id
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(nullable = false, length = 16)
    private String status = "running";

    @Column(columnDefinition = "TEXT")
    private String context;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(columnDefinition = "TEXT")
    private String error;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = IdGenerator.next();
        }
        if (startedAt == null) {
            startedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
