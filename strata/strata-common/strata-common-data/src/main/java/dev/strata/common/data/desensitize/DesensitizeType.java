// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.data.desensitize;

/**
 * Predefined desensitization strategies for common sensitive data types.
 *
 * <p>Used with {@link Desensitize} annotation to automatically mask sensitive
 * information in API responses.
 */
public enum DesensitizeType {

    /**
     * Custom desensitization with configurable keep-left and keep-right.
     */
    CUSTOM,

    /**
     * Phone number: keep first 3 and last 4 digits.
     * Example: 138****1234
     */
    PHONE,

    /**
     * Email address: keep first character and domain.
     * Example: u***@example.com
     */
    EMAIL,

    /**
     * ID card number: keep first 6 and last 4 digits.
     * Example: 110101********1234
     */
    ID_CARD,

    /**
     * Bank card number: keep first 4 and last 4 digits.
     * Example: 6222 **** **** 1234
     */
    BANK_CARD,

    /**
     * Name: keep first character.
     * Example: 张*
     */
    NAME,

    /**
     * Address: keep first 6 characters.
     * Example: 北京市朝阳区****
     */
    ADDRESS,

    /**
     * Password: always return fixed mask.
     * Example: ******
     */
    PASSWORD,

    /**
     * Full mask: replace all characters.
     */
    FULL_MASK,

    /**
     * API Key: keep first 8 and last 4 characters.
     * Example: sk-abc123****wxyz
     */
    API_KEY,

    /**
     * Secret Key: keep first 4 characters, mask the rest.
     * Example: abcd****
     */
    SECRET_KEY,

    /**
     * IP address: keep first two octets.
     * Example: 192.168.*.*
     */
    IP_ADDRESS,

    /**
     * MAC address: keep first 3 octets.
     * Example: 00:1A:2B:**:**:**
     */
    MAC_ADDRESS
}
