// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.compliance;

/**
 * Compliance gate the casino wallet delegates to the ops/compliance module.
 *
 * <p>The interface lives in the framework's ops API on purpose: the wallet is a business module and may depend on
 * framework contracts, while the framework (system-ops) must never depend on a business module. The wallet injects
 * the bean as an optional {@code ObjectProvider}, and system-ops provides the implementation that queries
 * {@code responsible_gambling_limit} / {@code aml_rule} and writes {@code aml_report} rows.
 *
 * <p>When the bean is absent (e.g. a stripped build) the checks are no-ops.
 */
public interface WalletCompliancePort {

    /**
     * Synchronous gate before a deposit intent is created.
     *
     * @throws dev.strata.common.core.exception.BusinessException when the amount would breach the player's
     *     responsible-gambling limit
     */
    void assertDepositAllowed(long playerId, long amountMinor);

    /**
     * Synchronous gate before a withdrawal is applied.
     *
     * @throws dev.strata.common.core.exception.BusinessException when the amount would breach the player's
     *     responsible-gambling limit
     */
    void assertWithdrawalAllowed(long playerId, long amountMinor);

    /** Best-effort post-settlement AML scan of a completed deposit; must never break the credit path. */
    void inspectDepositSettled(long playerId, long amountMinor, long intentId);

    /** Best-effort post-settlement AML scan of an approved withdrawal; must never break the payout path. */
    void inspectWithdrawalSettled(long playerId, long amountMinor, long intentId);
}
