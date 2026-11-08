// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.entity;

import dev.strata.common.data.entity.AuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Demo payment order (W5) 閳?status advanced by PaymentEvent from the payment plugin. */
@Entity
@Table(name = "demo_payment")
@Data
@EqualsAndHashCode(callSuper = true)
public class DemoPayment extends AuditingEntity {

    @Column(name = "order_no", nullable = false, unique = true, length = 64)
    private String orderNo;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "session_id", length = 128)
    private String sessionId;
}
