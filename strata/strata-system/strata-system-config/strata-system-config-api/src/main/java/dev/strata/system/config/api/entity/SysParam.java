// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "sys_param")
@Data
@EqualsAndHashCode(callSuper = true)
public class SysParam extends AuditingEntity {

    @Column(name = "param_key", nullable = false, unique = true, length = 128)
    private String key;

    @Column(name = "param_value", nullable = false, length = 512)
    private String value;

    @Column(nullable = false, length = 32)
    private String type = "string";

    @Column(length = 256)
    private String remark;
}
