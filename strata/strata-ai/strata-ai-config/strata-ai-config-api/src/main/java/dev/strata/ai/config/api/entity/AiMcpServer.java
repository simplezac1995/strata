// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "sys_ai_mcp_server")
@Data
@EqualsAndHashCode(callSuper = true)
public class AiMcpServer extends AuditingEntity {

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 256)
    private String url;

    @Column(name = "auth_type", length = 32)
    private String authType;

    @ToString.Exclude
    @Column(name = "auth_token", length = 256)
    private String authToken;

    @Column(nullable = false)
    private boolean enabled = true;
}
