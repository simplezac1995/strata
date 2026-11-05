// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service.compliance;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.system.ops.api.entity.compliance.AmlReport;
import dev.strata.system.ops.api.entity.compliance.AmlRule;
import dev.strata.system.ops.api.entity.compliance.ResponsibleGamblingLimit;
import dev.strata.system.ops.biz.repository.compliance.AmlReportRepository;
import dev.strata.system.ops.biz.repository.compliance.AmlRuleRepository;
import dev.strata.system.ops.biz.repository.compliance.ResponsibleGamblingLimitRepository;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletCompliancePortAdapter 单元测试")
class WalletCompliancePortAdapterTest {

    @Mock
    private ResponsibleGamblingLimitRepository limits;

    @Mock
    private AmlRuleRepository amlRules;

    @Mock
    private AmlReportRepository amlReports;

    @InjectMocks
    private WalletCompliancePortAdapter adapter;

    private ResponsibleGamblingLimit depositLimit(long amountMinor) {
        ResponsibleGamblingLimit l = new ResponsibleGamblingLimit();
        l.setUserId(7L);
        l.setLimitType("deposit");
        l.setAmountMinor(BigInteger.valueOf(amountMinor));
        return l;
    }

    @Test
    @DisplayName("未超限额时放行")
    void assertDepositAllowed_WithinLimit_Passes() {
        when(limits.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(depositLimit(10_000_000L)));

        assertThatCode(() -> adapter.assertDepositAllowed(7L, 5_000_000L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("超过限额时拒绝")
    void assertDepositAllowed_OverLimit_Throws() {
        when(limits.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(depositLimit(1_000_000L)));

        assertThatThrownBy(() -> adapter.assertDepositAllowed(7L, 5_000_000L)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("超过 AML 阈值时创建报告")
    void inspectDepositSettled_OverThreshold_CreatesReport() {
        AmlRule rule = new AmlRule();
        rule.setName("large deposit");
        rule.setRuleType("large_deposit");
        rule.setThresholdMinor(BigInteger.valueOf(10_000_000L));
        rule.setEnabled(true);
        when(amlRules.findByRuleTypeAndEnabledTrue("large_deposit")).thenReturn(List.of(rule));

        adapter.inspectDepositSettled(7L, 50_000_000L, 55L);

        verify(amlReports).save(any(AmlReport.class));
    }

    @Test
    @DisplayName("低于 AML 阈值不创建报告")
    void inspectDepositSettled_BelowThreshold_NoReport() {
        AmlRule rule = new AmlRule();
        rule.setName("large deposit");
        rule.setRuleType("large_deposit");
        rule.setThresholdMinor(BigInteger.valueOf(10_000_000L));
        when(amlRules.findByRuleTypeAndEnabledTrue("large_deposit")).thenReturn(List.of(rule));

        adapter.inspectDepositSettled(7L, 1_000L, 55L);

        verify(amlReports, never()).save(any());
    }

    @Test
    @DisplayName("AML 扫描失败时不抛错")
    void inspectDepositSettled_RepositoryThrows_Swallows() {
        when(amlRules.findByRuleTypeAndEnabledTrue(any())).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> adapter.inspectWithdrawalSettled(7L, 1L, 1L)).doesNotThrowAnyException();
    }
}
