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
        name = "sys_ai_prompt",
        indexes = {
            @Index(name = "idx_ai_prompt_code", columnList = "code"),
            @Index(name = "idx_ai_prompt_enabled", columnList = "enabled")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class AiPrompt extends AuditingEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 2000)
    private String template;

    @Column(nullable = false)
    private boolean enabled = true;
}
