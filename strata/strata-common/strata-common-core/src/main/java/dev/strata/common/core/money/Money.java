// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.money;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * An exact amount in an asset's smallest unit, together with the scale that gives it meaning
 * (USDT 6, TRX 6, BNB 18, BTC 8 — ADR-0026 §4).
 *
 * <p>Two rules this type exists to enforce:
 *
 * <ol>
 *   <li><b>No floating point.</b> {@code double} cannot represent 0.1 exactly, so a ledger built on it drifts.
 *       Every conversion here is integer or {@link BigInteger} arithmetic; parsing rejects anything that is not
 *       an exact decimal.
 *   <li><b>No silent scale mixing.</b> Adding 1 USDT (6 decimals) to 1 BNB (18 decimals) is a bug, not a
 *       number: operations require the same scale and fail loudly otherwise. Converting between scales is an
 *       explicit call ({@link #rescale(int)}), and it refuses to lose precision.
 * </ol>
 *
 * <p>The stored form in every table is the minor amount alone ({@code DECIMAL(38,0)}); the scale comes from the
 * asset registry, so this type never guesses it.
 */
public record Money(BigInteger minor, int decimals) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(minor, "minor");
        if (decimals < 0 || decimals > 18) {
            throw new IllegalArgumentException("decimals must be between 0 and 18, got " + decimals);
        }
    }

    public static Money zero(int decimals) {
        return new Money(BigInteger.ZERO, decimals);
    }

    public static Money ofMinor(long minor, int decimals) {
        return new Money(BigInteger.valueOf(minor), decimals);
    }

    public static Money ofMinor(BigInteger minor, int decimals) {
        return new Money(minor, decimals);
    }

    /**
     * Parses an exact decimal string ("12.34", "0.000001", "-5") into minor units.
     *
     * @throws IllegalArgumentException when the value is malformed or carries more decimals than the asset has,
     *     because rounding someone's money silently is never the right default
     */
    public static Money parse(String value, int decimals) {
        Objects.requireNonNull(value, "value");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("amount is empty");
        }
        boolean negative = trimmed.startsWith("-");
        String unsigned = negative ? trimmed.substring(1) : trimmed.startsWith("+") ? trimmed.substring(1) : trimmed;
        int dot = unsigned.indexOf('.');
        String whole = dot < 0 ? unsigned : unsigned.substring(0, dot);
        String fraction = dot < 0 ? "" : unsigned.substring(dot + 1);
        if (whole.isEmpty() && fraction.isEmpty()) {
            throw new IllegalArgumentException("amount '" + value + "' has no digits");
        }
        if (!whole.isEmpty() && !whole.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("amount '" + value + "' is not a number");
        }
        if (!fraction.isEmpty() && !fraction.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("amount '" + value + "' is not a number");
        }
        if (fraction.length() > decimals) {
            throw new IllegalArgumentException(
                    "amount '" + value + "' has " + fraction.length() + " decimals, the asset has " + decimals);
        }
        String padded = fraction + "0".repeat(decimals - fraction.length());
        BigInteger minor = new BigInteger((whole.isEmpty() ? "0" : whole) + padded);
        return new Money(negative ? minor.negate() : minor, decimals);
    }

    public boolean isZero() {
        return minor.signum() == 0;
    }

    public boolean isNegative() {
        return minor.signum() < 0;
    }

    public boolean isPositive() {
        return minor.signum() > 0;
    }

    public Money plus(Money other) {
        requireSameScale(other);
        return new Money(minor.add(other.minor), decimals);
    }

    public Money minus(Money other) {
        requireSameScale(other);
        return new Money(minor.subtract(other.minor), decimals);
    }

    public Money negate() {
        return new Money(minor.negate(), decimals);
    }

    public Money abs() {
        return minor.signum() < 0 ? negate() : this;
    }

    /** Multiplies by an exact integer factor (a bonus multiplier, a fee in basis points over 10000, …). */
    public Money times(BigInteger factor) {
        Objects.requireNonNull(factor, "factor");
        return new Money(minor.multiply(factor), decimals);
    }

    /**
     * Re-expresses the same amount at another scale.
     *
     * @throws ArithmeticException when the amount cannot be represented exactly at the target scale
     */
    public Money rescale(int targetDecimals) {
        if (targetDecimals == decimals) {
            return this;
        }
        if (targetDecimals < 0 || targetDecimals > 18) {
            throw new IllegalArgumentException("decimals must be between 0 and 18, got " + targetDecimals);
        }
        BigInteger factor = BigInteger.TEN.pow(Math.abs(targetDecimals - decimals));
        if (targetDecimals > decimals) {
            return new Money(minor.multiply(factor), targetDecimals);
        }
        BigInteger[] divided = minor.divideAndRemainder(factor);
        if (divided[1].signum() != 0) {
            throw new ArithmeticException(
                    "amount " + toPlainString() + " cannot be expressed with " + targetDecimals + " decimals");
        }
        return new Money(divided[0], targetDecimals);
    }

    /** The asset's smallest unit as a plain decimal string, e.g. {@code 12345678} at 6 decimals is "12.345678". */
    public String toPlainString() {
        BigInteger[] divided = minor.abs().divideAndRemainder(BigInteger.TEN.pow(decimals));
        String whole = divided[0].toString();
        String fraction = divided[1].toString();
        if (decimals == 0) {
            return (minor.signum() < 0 ? "-" : "") + whole;
        }
        String padded = "0".repeat(decimals - fraction.length()) + fraction;
        return (minor.signum() < 0 ? "-" : "") + whole + "." + padded;
    }

    /**
     * The same amount as an exact {@link BigDecimal} — for serialising a balance to a client that expects a
     * number, and for reporting. The conversion is a scale change, not a division, so nothing is lost; day-to-day
     * arithmetic still belongs on {@link #minor()} or through this type.
     */
    public BigDecimal toBigDecimal() {
        return new BigDecimal(minor, decimals);
    }

    /**
     * Builds an amount from a decimal value.
     *
     * @throws ArithmeticException when the value carries more decimals than the asset has, since rounding a
     *     balance silently is exactly what this type exists to prevent
     */
    public static Money of(BigDecimal value, int decimals) {
        Objects.requireNonNull(value, "value");
        return new Money(value.movePointRight(decimals).toBigIntegerExact(), decimals);
    }

    @Override
    public int compareTo(Money other) {
        requireSameScale(other);
        return minor.compareTo(other.minor);
    }

    @Override
    public String toString() {
        return toPlainString();
    }

    private void requireSameScale(Money other) {
        Objects.requireNonNull(other, "other");
        if (other.decimals != decimals) {
            throw new IllegalArgumentException("cannot combine " + decimals + "-decimal and " + other.decimals
                    + "-decimal amounts; rescale explicitly (ADR-0026 §4)");
        }
    }
}
