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
        name = "sys_ai_model",
        indexes = {
            @Index(name = "idx_ai_model_provider_id", columnList = "provider_id"),
            @Index(name = "idx_ai_model_code", columnList = "code"),
            @Index(name = "idx_ai_model_enabled", columnList = "enabled"),
            @Index(name = "idx_ai_model_provider_enabled", columnList = "provider_id, enabled")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class AiModel extends AuditingEntity {

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false)
    private boolean enabled = true;
}
