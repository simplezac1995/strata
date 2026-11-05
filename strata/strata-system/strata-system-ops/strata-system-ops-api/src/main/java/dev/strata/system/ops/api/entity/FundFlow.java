// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigInteger;

/**
 * Operations-facing fund flow ledger row ({@code fund_flow}).
 *
 * <p>The casino wallet already keeps its own ledger (LedgerEntry/LedgerLine in casino-wallet-api); this
 * read-model table is owned by the ops reconciliation center because ops-biz must not depend on casino-api.
 */
@Entity
@Table(
        name = "fund_flow",
        indexes = {
            @Index(name = "idx_ff_user_id", columnList = "user_id"),
            @Index(name = "idx_ff_biz_type", columnList = "biz_type"),
            @Index(name = "idx_ff_created_at", columnList = "created_at")
        })
public class FundFlow extends TimestampedEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "account_type", length = 32)
    private String accountType;

    @Column(name = "change_amount_minor", nullable = false)
    private BigInteger changeAmountMinor = BigInteger.ZERO;

    @Column(name = "balance_after_minor", nullable = false)
    private BigInteger balanceAfterMinor = BigInteger.ZERO;

    @Column(name = "biz_type", length = 32)
    private String bizType;

    @Column(name = "biz_id")
    private Long bizId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigInteger getChangeAmountMinor() {
        return changeAmountMinor;
    }

    public void setChangeAmountMinor(BigInteger changeAmountMinor) {
        this.changeAmountMinor = changeAmountMinor;
    }

    public BigInteger getBalanceAfterMinor() {
        return balanceAfterMinor;
    }

    public void setBalanceAfterMinor(BigInteger balanceAfterMinor) {
        this.balanceAfterMinor = balanceAfterMinor;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public Long getBizId() {
        return bizId;
    }

    public void setBizId(Long bizId) {
        this.bizId = bizId;
    }
}
