// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

/**
 * Branch arms of {@link Money} not in {@link MoneyTest}: signum predicates, minus/abs, rescale up and
 * out-of-range target, decimals==0 rendering, "+"/empty/non-digit parse paths, and compareTo/toString.
 */
class MoneyBranchTest {

    @Test
    void factories() {
        assertThat(Money.zero(6)).isEqualTo(new Money(BigInteger.ZERO, 6));
        assertThat(Money.ofMinor(BigInteger.valueOf(123), 6)).isEqualTo(Money.ofMinor(123L, 6));
    }

    @Test
    void signumPredicates() {
        Money z = Money.zero(6);
        assertThat(z.isZero()).isTrue();
        assertThat(z.isNegative()).isFalse();
        assertThat(z.isPositive()).isFalse();

        Money pos = Money.ofMinor(5, 6);
        assertThat(pos.isZero()).isFalse();
        assertThat(pos.isNegative()).isFalse();
        assertThat(pos.isPositive()).isTrue();

        Money neg = Money.ofMinor(-5, 6);
        assertThat(neg.isZero()).isFalse();
        assertThat(neg.isNegative()).isTrue();
        assertThat(neg.isPositive()).isFalse();
    }

    @Test
    void minusAndAbs() {
        assertThat(Money.ofMinor(10, 6).minus(Money.ofMinor(3, 6))).isEqualTo(Money.ofMinor(7, 6));
        assertThat(Money.ofMinor(-7, 6).abs()).isEqualTo(Money.ofMinor(7, 6));
        Money positive = Money.ofMinor(7, 6);
        assertThat(positive.abs()).isSameAs(positive);
    }

    @Test
    void parseEdges() {
        assertThat(Money.parse("+5", 6)).isEqualTo(Money.ofMinor(5_000_000, 6));
        assertThatThrownBy(() -> Money.parse("  ", 6)).hasMessageContaining("empty");
        assertThatThrownBy(() -> Money.parse(".", 6)).hasMessageContaining("no digits");
        assertThatThrownBy(() -> Money.parse("1a.5", 6)).hasMessageContaining("not a number");
        assertThatThrownBy(() -> Money.parse("1.5x", 6)).hasMessageContaining("not a number");
    }

    @Test
    void rescaleArms() {
        // 6 -> 18 升精度
        assertThat(Money.ofMinor(12, 6).rescale(18)).isEqualTo(Money.ofMinor(12_000_000_000_000L, 18));
        assertThatThrownBy(() -> Money.ofMinor(1, 6).rescale(19)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.ofMinor(1, 6).rescale(-1)).isInstanceOf(IllegalArgumentException.class);
        // 同精度直接返回 this
        Money same = Money.ofMinor(1, 6);
        assertThat(same.rescale(6)).isSameAs(same);
    }

    @Test
    void plainStringArms() {
        assertThat(Money.ofMinor(123, 0).toPlainString()).isEqualTo("123");
        assertThat(Money.ofMinor(-456, 0).toPlainString()).isEqualTo("-456");
        assertThat(Money.ofMinor(5, 6).toPlainString()).isEqualTo("0.000005");
    }

    @Test
    void compareAndToString() {
        assertThat(Money.ofMinor(2, 6).compareTo(Money.ofMinor(1, 6))).isPositive();
        assertThat(Money.ofMinor(1, 6).toString()).isEqualTo("0.000001");
    }
}
