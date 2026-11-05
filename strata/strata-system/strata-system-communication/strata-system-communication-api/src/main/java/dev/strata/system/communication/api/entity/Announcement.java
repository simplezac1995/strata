// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(
        name = "sys_announcement",
        indexes = {@Index(name = "idx_ann_enabled", columnList = "enabled")})
@Data
@EqualsAndHashCode(callSuper = true)
public class Announcement extends AuditingEntity {

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 2000)
    private String content;

    @Column(nullable = false)
    private boolean enabled = true;
}
