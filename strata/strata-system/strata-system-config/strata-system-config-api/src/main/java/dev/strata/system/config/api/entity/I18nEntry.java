// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "sys_i18n_entry")
@Data
@EqualsAndHashCode(callSuper = true)
public class I18nEntry extends AuditingEntity {

    @Column(name = "message_key", nullable = false, unique = true, length = 128)
    private String messageKey;

    @Column(length = 512)
    private String zh;

    @Column(length = 512)
    private String en;

    @Column(nullable = false)
    private boolean deleted = false;
}
