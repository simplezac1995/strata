// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "sys_email_template")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmailTemplate extends AuditingEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 128)
    private String subject;

    @Column(length = 2000)
    private String body;

    @Column(nullable = false)
    private boolean enabled = true;
}
