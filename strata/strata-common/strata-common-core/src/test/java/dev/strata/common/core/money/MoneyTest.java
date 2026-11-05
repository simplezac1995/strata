// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

/** Money is where a rounding bug becomes a reconciliation incident, so the edges are pinned here. */
class MoneyTest {

    @Test
    void parses_and_renders_exact_decimals() {
        assertThat(Money.parse("12.345678", 6).minor()).isEqualTo(new BigInteger("12345678"));
        assertThat(Money.parse("0.000001", 6).minor()).isEqualTo(BigInteger.ONE);
        assertThat(Money.parse("100", 6).minor()).isEqualTo(new BigInteger("100000000"));
        assertThat(Money.parse(".5", 6).minor()).isEqualTo(new BigInteger("500000"));
        assertThat(Money.parse("-3.25", 6).minor()).isEqualTo(new BigInteger("-3250000"));

        assertThat(Money.ofMinor(12345678, 6).toPlainString()).isEqualTo("12.345678");
        assertThat(Money.ofMinor(5, 6).toPlainString()).isEqualTo("0.000005");
        assertThat(Money.ofMinor(0, 6).toPlainString()).isEqualTo("0.000000");
        assertThat(Money.ofMinor(-2500000, 6).toPlainString()).isEqualTo("-2.500000");
        assertThat(Money.ofMinor(7, 0).toPlainString()).isEqualTo("7");
    }

    @Test
    void refuses_to_round_away_a_player_s_cent() {
        assertThatThrownBy(() -> Money.parse("0.1234567", 6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("decimals");
        assertThatThrownBy(() -> Money.parse("12,34", 6)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.parse("", 6)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.parse("abc", 6)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void never_mixes_scales_by_accident() {
        Money usdt = Money.ofMinor(1_000_000, 6);
        Money bnb = Money.ofMinor(1_000_000_000_000_000_000L, 18);

        assertThatThrownBy(() -> usdt.plus(bnb))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rescale explicitly");
        assertThatThrownBy(() -> usdt.compareTo(bnb)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rescales_only_when_the_amount_is_exact() {
        assertThat(Money.ofMinor(1_500_000, 6).rescale(4).minor()).isEqualTo(new BigInteger("15000"));
        assertThat(Money.ofMinor(1_500_000, 6).rescale(6)).isEqualTo(Money.ofMinor(1_500_000, 6));
        assertThat(Money.ofMinor(1_000_000, 6).rescale(0).minor()).isEqualTo(BigInteger.ONE);

        // 0.000005 USDT has no whole-unit representation: losing it silently would be a rounding bug.
        assertThatThrownBy(() -> Money.ofMinor(5, 6).rescale(0)).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void arithmetic_stays_integer_exact() {
        Money a = Money.parse("0.1", 6);
        Money b = Money.parse("0.2", 6);
        assertThat(a.plus(b).toPlainString())
                .as("the classic float trap: 0.1 + 0.2 must be exactly 0.300000")
                .isEqualTo("0.300000");

        assertThat(Money.ofMinor(1_000_000, 6).times(BigInteger.valueOf(3)).toPlainString())
                .isEqualTo("3.000000");
        assertThat(Money.ofMinor(-5, 6).abs().minor()).isEqualTo(BigInteger.valueOf(5));
        assertThat(Money.ofMinor(-5, 6).negate().minor()).isEqualTo(BigInteger.valueOf(5));
    }

    @Test
    void holds_eighteen_decimal_amounts_that_a_long_cannot() {
        // 1 BNB = 10^18 wei, and a 1000 BNB deposit is 10^21: a long overflows, BigInteger does not.
        Money thousandBnb = Money.parse("1000", 18);
        assertThat(thousandBnb.minor()).isEqualTo(new BigInteger("1000000000000000000000"));
        assertThat(thousandBnb.toPlainString()).isEqualTo("1000.000000000000000000");
    }

    @Test
    void converts_to_a_decimal_only_when_the_scale_is_exact() {
        assertThat(Money.ofMinor(12_345_678, 6).toBigDecimal()).isEqualByComparingTo("12.345678");
        assertThat(Money.ofMinor(1_000_000, 6).toBigDecimal().toPlainString()).isEqualTo("1.000000");
        assertThat(Money.of(new java.math.BigDecimal("12.5"), 6).minor()).isEqualTo(new BigInteger("12500000"));

        assertThatThrownBy(() -> Money.of(new java.math.BigDecimal("1.1234567"), 6))
                .as("a balance is never rounded on the way in or out")
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void rejects_an_impossible_scale() {
        assertThatThrownBy(() -> Money.ofMinor(1, 19)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.ofMinor(1, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
