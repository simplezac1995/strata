// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(
        name = "sys_ai_provider",
        indexes = {
            @Index(name = "idx_ai_provider_code", columnList = "code"),
            @Index(name = "idx_ai_provider_enabled", columnList = "enabled"),
            @Index(name = "idx_ai_provider_type", columnList = "type")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class AiProvider extends AuditingEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(name = "base_url", length = 256)
    private String baseUrl;

    @ToString.Exclude
    @Column(name = "api_key", length = 256)
    private String apiKey;

    @Column(nullable = false)
    private boolean enabled = true;
}
