// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.FundFlowDto;
import dev.strata.system.api.dto.ReconciliationDailyDto;
import dev.strata.system.api.dto.ReconciliationSummaryDto;
import dev.strata.system.api.request.RunReconciliationRequest;
import dev.strata.system.ops.api.entity.FundFlow;
import dev.strata.system.ops.api.entity.ReconciliationDaily;
import dev.strata.system.ops.biz.mapper.FundFlowMapper;
import dev.strata.system.ops.biz.mapper.ReconciliationDailyMapper;
import dev.strata.system.ops.biz.repository.FundFlowRepository;
import dev.strata.system.ops.biz.repository.ReconciliationDailyRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "varargs"})
class ReconciliationAppServiceTest {

    @Mock
    private ReconciliationDailyRepository reconciliationDailyRepository;

    @Mock
    private ReconciliationDailyMapper reconciliationDailyMapper;

    @Mock
    private FundFlowRepository fundFlowRepository;

    @Mock
    private FundFlowMapper fundFlowMapper;

    @InjectMocks
    private ReconciliationAppService service;

    @Test
    void pageDaily_ReturnsPaged() {
        ReconciliationDaily row = new ReconciliationDaily();
        when(reconciliationDailyRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(row)));
        when(reconciliationDailyMapper.toDto(any(ReconciliationDaily.class)))
                .thenReturn(new ReconciliationDailyDto("1", LocalDate.now(), 0, 0, 0, 0, "pending", null, null));

        PageResult<ReconciliationDailyDto> result = service.pageDaily(0, 10);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void run_NewDate_CreatesPendingRow() {
        RunReconciliationRequest req = new RunReconciliationRequest(LocalDate.of(2026, 9, 21));
        when(reconciliationDailyRepository.findByBizDate(req.date())).thenReturn(Optional.empty());
        when(reconciliationDailyRepository.save(any(ReconciliationDaily.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reconciliationDailyMapper.toDto(any(ReconciliationDaily.class)))
                .thenReturn(new ReconciliationDailyDto("1", req.date(), 0, 0, 0, 0, "pending", null, null));

        ReconciliationDailyDto dto = service.run(req);

        assertThat(dto.status()).isEqualTo("pending");
        verify(reconciliationDailyRepository).save(any(ReconciliationDaily.class));
    }

    @Test
    void run_ExistingDate_ResetsToPending() {
        RunReconciliationRequest req = new RunReconciliationRequest(LocalDate.of(2026, 9, 21));
        ReconciliationDaily existing = new ReconciliationDaily();
        existing.setStatus("matched");
        when(reconciliationDailyRepository.findByBizDate(req.date())).thenReturn(Optional.of(existing));
        when(reconciliationDailyMapper.toDto(existing))
                .thenReturn(new ReconciliationDailyDto("1", req.date(), 0, 0, 0, 0, "pending", null, null));

        service.run(req);

        assertThat(existing.getStatus()).isEqualTo("pending");
        verify(reconciliationDailyRepository).save(existing);
    }

    @Test
    void pageFundFlow_Filters() {
        FundFlow row = new FundFlow();
        when(fundFlowRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row)));
        when(fundFlowMapper.toDto(any(FundFlow.class)))
                .thenReturn(new FundFlowDto("1", "9", "wallet", 100, 1000, "recharge", "100", null));

        PageResult<FundFlowDto> result = service.pageFundFlow(9L, "recharge", 0, 10);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void summary_AggregatesToday() {
        when(fundFlowRepository.sumChangeSince(eq("recharge"), any(Instant.class)))
                .thenReturn(5000L);
        when(fundFlowRepository.countSince(eq("recharge"), any(Instant.class))).thenReturn(3L);
        when(fundFlowRepository.sumChangeSince(eq("withdraw"), any(Instant.class)))
                .thenReturn(2000L);
        when(fundFlowRepository.countSince(eq("withdraw"), any(Instant.class))).thenReturn(1L);
        when(fundFlowRepository.sumChangeSince(eq("fee"), any(Instant.class))).thenReturn(100L);

        ReconciliationSummaryDto summary = service.summary();

        assertThat(summary.todayRechargeAmountMinor()).isEqualTo(5000L);
        assertThat(summary.todayRechargeCount()).isEqualTo(3L);
        assertThat(summary.todayWithdrawAmountMinor()).isEqualTo(2000L);
        assertThat(summary.todayWithdrawCount()).isEqualTo(1L);
        assertThat(summary.todayFeeMinor()).isEqualTo(100L);
    }
}
