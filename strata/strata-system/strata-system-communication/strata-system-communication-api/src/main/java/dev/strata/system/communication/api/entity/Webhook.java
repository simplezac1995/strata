// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "sys_webhook")
@Data
@EqualsAndHashCode(callSuper = true)
public class Webhook extends AuditingEntity {

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 256)
    private String url;

    @ToString.Exclude
    @Column(length = 128)
    private String secret;

    @Column(nullable = false)
    private boolean enabled = true;
}
