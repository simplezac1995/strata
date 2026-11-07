// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigInteger;
import java.time.LocalDate;

/** Daily reconciliation snapshot row ({@code reconciliation_daily}). */
@Entity
@Table(
        name = "reconciliation_daily",
        indexes = {
            @Index(name = "idx_rd_biz_date", columnList = "biz_date", unique = true),
            @Index(name = "idx_rd_status", columnList = "status")
        })
public class ReconciliationDaily extends TimestampedEntity {

    @Column(name = "biz_date", nullable = false)
    private LocalDate bizDate;

    @Column(name = "total_recharge_minor", nullable = false)
    private BigInteger totalRechargeMinor = BigInteger.ZERO;

    @Column(name = "total_withdraw_minor", nullable = false)
    private BigInteger totalWithdrawMinor = BigInteger.ZERO;

    @Column(name = "total_fee_minor", nullable = false)
    private BigInteger totalFeeMinor = BigInteger.ZERO;

    @Column(name = "player_balance_minor", nullable = false)
    private BigInteger playerBalanceMinor = BigInteger.ZERO;

    /** pending | matched | diff. */
    @Column(nullable = false, length = 16)
    private String status = "pending";

    @Column(name = "diff_detail", length = 2048)
    private String diffDetail;

    public LocalDate getBizDate() {
        return bizDate;
    }

    public void setBizDate(LocalDate bizDate) {
        this.bizDate = bizDate;
    }

    public BigInteger getTotalRechargeMinor() {
        return totalRechargeMinor;
    }

    public void setTotalRechargeMinor(BigInteger totalRechargeMinor) {
        this.totalRechargeMinor = totalRechargeMinor;
    }

    public BigInteger getTotalWithdrawMinor() {
        return totalWithdrawMinor;
    }

    public void setTotalWithdrawMinor(BigInteger totalWithdrawMinor) {
        this.totalWithdrawMinor = totalWithdrawMinor;
    }

    public BigInteger getTotalFeeMinor() {
        return totalFeeMinor;
    }

    public void setTotalFeeMinor(BigInteger totalFeeMinor) {
        this.totalFeeMinor = totalFeeMinor;
    }

    public BigInteger getPlayerBalanceMinor() {
        return playerBalanceMinor;
    }

    public void setPlayerBalanceMinor(BigInteger playerBalanceMinor) {
        this.playerBalanceMinor = playerBalanceMinor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDiffDetail() {
        return diffDetail;
    }

    public void setDiffDetail(String diffDetail) {
        this.diffDetail = diffDetail;
    }
}
