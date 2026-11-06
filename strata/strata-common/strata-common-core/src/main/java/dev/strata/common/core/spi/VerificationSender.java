// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.spi;

/**
 * Sends email/SMS verification codes on behalf of the framework (registration, password reset, contact binding).
 *
 * <p>The framework has no mail or SMS transport of its own. The default bean logs and drops the request; a
 * deployment plugs in its own implementation by declaring a {@code VerificationSender} bean, which then replaces the
 * default one (ADR-0021 style module-owned extension point).
 */
public interface VerificationSender {

    /** Sends an email verification code. Implementations must not log the code itself. */
    void sendEmailVerificationCode(String email, String code);

    /** Sends an SMS verification code. Implementations must not log the code itself. */
    void sendSmsVerificationCode(String phone, String code);
}
