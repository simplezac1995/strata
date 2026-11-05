// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "sys_ai_skill")
@Data
@EqualsAndHashCode(callSuper = true)
public class AiSkill extends AuditingEntity {

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 16)
    private String type = "skill";

    @Column(length = 128)
    private String path;

    @Column(length = 256)
    private String description;

    @Column(nullable = false)
    private boolean enabled = true;
}
