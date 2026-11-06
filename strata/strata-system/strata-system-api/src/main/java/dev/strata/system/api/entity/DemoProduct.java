// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "demo_product")
@Data
@EqualsAndHashCode(callSuper = true)
public class DemoProduct extends AuditingEntity {

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock = 0;

    @Column(length = 512)
    private String description;

    @Column(nullable = false)
    private boolean enabled = true;
}
