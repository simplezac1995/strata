// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(
        name = "sys_ai_agent",
        indexes = {@Index(name = "idx_ai_agent_enabled", columnList = "enabled")})
@Data
@EqualsAndHashCode(callSuper = true)
public class AiAgent extends AuditingEntity {

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 64)
    private String model;

    @Column(length = 2000)
    private String prompt;

    @Column(nullable = false)
    private boolean enabled = true;
}
