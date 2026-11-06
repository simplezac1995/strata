// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity.compliance;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Currency exchange rate row ({@code exchange_rate}). */
@Entity
@Table(
        name = "exchange_rate",
        indexes = {@Index(name = "idx_er_pair", columnList = "from_currency, to_currency", unique = true)})
public class ExchangeRate extends TimestampedEntity {

    @Column(name = "from_currency", nullable = false, length = 8)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 8)
    private String toCurrency;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal rate;

    public String getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(String fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(String toCurrency) {
        this.toCurrency = toCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }
}
