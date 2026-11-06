// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service.compliance;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.system.ops.api.compliance.WalletCompliancePort;
import dev.strata.system.ops.api.entity.compliance.AmlReport;
import dev.strata.system.ops.api.entity.compliance.AmlRule;
import dev.strata.system.ops.api.entity.compliance.ResponsibleGamblingLimit;
import dev.strata.system.ops.biz.repository.compliance.AmlReportRepository;
import dev.strata.system.ops.biz.repository.compliance.AmlRuleRepository;
import dev.strata.system.ops.biz.repository.compliance.ResponsibleGamblingLimitRepository;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Ops-side implementation of the wallet compliance port. Reads {@code responsible_gambling_limit} to gate
 * deposits/withdrawals, and, when a settled transaction crosses an enabled {@code aml_rule} threshold, writes an
 * {@code aml_report} row for review.
 *
 * <p>Settlement scans are best-effort: they never throw back into the money path, so a reporting failure can't
 * block a credit or a payout.
 */
@Component
public class WalletCompliancePortAdapter implements WalletCompliancePort {

    private static final Logger log = LoggerFactory.getLogger(WalletCompliancePortAdapter.class);
    private static final String LIMIT_DEPOSIT = "deposit";
    private static final String RULE_LARGE_DEPOSIT = "large_deposit";
    private static final String RULE_LARGE_WITHDRAW = "large_withdraw";

    private final ResponsibleGamblingLimitRepository limits;
    private final AmlRuleRepository amlRules;
    private final AmlReportRepository amlReports;

    public WalletCompliancePortAdapter(
            ResponsibleGamblingLimitRepository limits, AmlRuleRepository amlRules, AmlReportRepository amlReports) {
        this.limits = limits;
        this.amlRules = amlRules;
        this.amlReports = amlReports;
    }

    @Override
    public void assertDepositAllowed(long playerId, long amountMinor) {
        assertWithinDepositLimit(playerId, amountMinor);
    }

    @Override
    public void assertWithdrawalAllowed(long playerId, long amountMinor) {
        // Conservative: a configured deposit limit also gates withdrawals, so a player cannot cash out more than
        // their responsible-gambling cap once it is set.
        assertWithinDepositLimit(playerId, amountMinor);
    }

    private void assertWithinDepositLimit(long playerId, long amountMinor) {
        Instant now = Instant.now();
        for (ResponsibleGamblingLimit limit : limits.findByUserIdOrderByCreatedAtDesc(playerId)) {
            if (!LIMIT_DEPOSIT.equals(limit.getLimitType()) || limit.getAmountMinor() == null) {
                continue;
            }
            if (limit.getStartDate() != null && limit.getStartDate().isAfter(now)) {
                continue;
            }
            if (limit.getEndDate() != null && limit.getEndDate().isBefore(now)) {
                continue;
            }
            if (BigInteger.valueOf(amountMinor).compareTo(limit.getAmountMinor()) > 0) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_RULE_VIOLATION,
                        "Amount " + amountMinor + " minor exceeds your responsible-gambling deposit limit of "
                                + limit.getAmountMinor() + " minor");
            }
        }
    }

    @Override
    public void inspectDepositSettled(long playerId, long amountMinor, long intentId) {
        scanAml(playerId, amountMinor, intentId, RULE_LARGE_DEPOSIT, "deposit");
    }

    @Override
    public void inspectWithdrawalSettled(long playerId, long amountMinor, long intentId) {
        scanAml(playerId, amountMinor, intentId, RULE_LARGE_WITHDRAW, "withdrawal");
    }

    private void scanAml(long playerId, long amountMinor, long intentId, String ruleType, String direction) {
        try {
            List<AmlRule> rules = amlRules.findByRuleTypeAndEnabledTrue(ruleType);
            for (AmlRule rule : rules) {
                if (rule.getThresholdMinor() != null
                        && BigInteger.valueOf(amountMinor).compareTo(rule.getThresholdMinor()) >= 0) {
                    AmlReport report = new AmlReport();
                    report.setUserId(playerId);
                    report.setReportType("large_transaction");
                    report.setAmountMinor(BigInteger.valueOf(amountMinor));
                    report.setDescription(String.format(
                            "Large %s intent %d: %d minor crosses rule '%s' threshold %s minor",
                            direction, intentId, amountMinor, rule.getName(), rule.getThresholdMinor()));
                    report.setStatus("pending");
                    amlReports.save(report);
                    log.warn(
                            "AML report created for player {} {} intent {} amount {} minor",
                            playerId,
                            direction,
                            intentId,
                            amountMinor);
                }
            }
        } catch (RuntimeException e) {
            log.warn("AML scan failed for {} intent {}: {}", direction, intentId, e.getMessage());
        }
    }
}
