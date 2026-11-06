// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.api.entity;

import dev.strata.common.core.util.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * One node of an agent's execution graph ({@code ai_agent_node}). {@code nodeType} is {@code llm}/{@code tool}/
 * {@code branch}; {@code config} holds node-specific settings (prompt template, branch condition...) serialized
 * as JSON, and {@code nextNodeKeys} lists the downstream edges as a JSON array of node keys. {@code positionX/Y}
 * are editor layout hints only.
 */
@Entity
@Table(
        name = "ai_agent_node",
        indexes = {@Index(name = "idx_ai_agent_node_agent", columnList = "agent_id")})
public class AiAgentNode {

    @Id
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "node_key", nullable = false, length = 64)
    private String nodeKey;

    @Column(name = "node_type", nullable = false, length = 16)
    private String nodeType;

    @Column(length = 128)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String config;

    @Column(name = "position_x")
    private BigDecimal positionX;

    @Column(name = "position_y")
    private BigDecimal positionY;

    @Column(name = "next_node_keys", columnDefinition = "TEXT")
    private String nextNodeKeys;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = IdGenerator.next();
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

    public String getNodeKey() {
        return nodeKey;
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public BigDecimal getPositionX() {
        return positionX;
    }

    public void setPositionX(BigDecimal positionX) {
        this.positionX = positionX;
    }

    public BigDecimal getPositionY() {
        return positionY;
    }

    public void setPositionY(BigDecimal positionY) {
        this.positionY = positionY;
    }

    public String getNextNodeKeys() {
        return nextNodeKeys;
    }

    public void setNextNodeKeys(String nextNodeKeys) {
        this.nextNodeKeys = nextNodeKeys;
    }
}
