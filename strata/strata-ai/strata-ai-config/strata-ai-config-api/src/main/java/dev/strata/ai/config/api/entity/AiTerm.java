// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "sys_ai_term")
@Data
@EqualsAndHashCode(callSuper = true)
public class AiTerm extends AuditingEntity {

    @Column(nullable = false, unique = true, length = 128)
    private String term;

    @Column(nullable = false, length = 128)
    private String translation;

    @Column(length = 256)
    private String notes;

    @Column(nullable = false)
    private boolean enabled = true;
}
