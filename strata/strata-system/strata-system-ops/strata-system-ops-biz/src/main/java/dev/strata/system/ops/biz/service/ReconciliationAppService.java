// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.data.spec.Specs;
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
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconciliationAppService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationAppService.class);

    private static final String BIZ_RECHARGE = "recharge";
    private static final String BIZ_WITHDRAW = "withdraw";
    private static final String BIZ_FEE = "fee";

    private final ReconciliationDailyRepository reconciliationDailyRepository;
    private final ReconciliationDailyMapper reconciliationDailyMapper;
    private final FundFlowRepository fundFlowRepository;
    private final FundFlowMapper fundFlowMapper;

    public ReconciliationAppService(
            ReconciliationDailyRepository reconciliationDailyRepository,
            ReconciliationDailyMapper reconciliationDailyMapper,
            FundFlowRepository fundFlowRepository,
            FundFlowMapper fundFlowMapper) {
        this.reconciliationDailyRepository = reconciliationDailyRepository;
        this.reconciliationDailyMapper = reconciliationDailyMapper;
        this.fundFlowRepository = fundFlowRepository;
        this.fundFlowMapper = fundFlowMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<ReconciliationDailyDto> pageDaily(int page, int size) {
        Page<ReconciliationDaily> result = reconciliationDailyRepository.findAll(
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "bizDate")));
        return PageResult.from(result.map(reconciliationDailyMapper::toDto));
    }

    /**
     * Manually trigger daily reconciliation.
     *
     * <p>TODO: wire the real reconciliation logic (settle casino wallet ledger vs. recharge/withdraw channels,
     * compute diff). This shell upserts a {@code pending} zero row for the requested date so the endpoint and
     * the table are exercisable end to end.
     */
    @Transactional
    @OperationLog(module = "system:reconciliation", action = "create")
    public ReconciliationDailyDto run(RunReconciliationRequest request) {
        LocalDate bizDate = request.date();
        ReconciliationDaily row = reconciliationDailyRepository
                .findByBizDate(bizDate)
                .orElseGet(() -> {
                    ReconciliationDaily fresh = new ReconciliationDaily();
                    fresh.setBizDate(bizDate);
                    return fresh;
                });
        row.setStatus("pending");
        row.setDiffDetail(null);
        reconciliationDailyRepository.save(row);
        return reconciliationDailyMapper.toDto(row);
    }

    @Transactional(readOnly = true)
    public PageResult<FundFlowDto> pageFundFlow(Long userId, String bizType, int page, int size) {
        var spec = Specs.of(FundFlow.class)
                .eq("userId", userId)
                .eq("bizType", bizType)
                .toSpecification();
        Page<FundFlow> result =
                fundFlowRepository.findAll(spec, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResult.from(result.map(fundFlowMapper::toDto));
    }

    @Transactional(readOnly = true)
    public ReconciliationSummaryDto summary() {
        Instant since = startOfToday();
        long rechargeAmount = fundFlowRepository.sumChangeSince(BIZ_RECHARGE, since);
        long rechargeCount = fundFlowRepository.countSince(BIZ_RECHARGE, since);
        long withdrawAmount = fundFlowRepository.sumChangeSince(BIZ_WITHDRAW, since);
        long withdrawCount = fundFlowRepository.countSince(BIZ_WITHDRAW, since);
        long fee = fundFlowRepository.sumChangeSince(BIZ_FEE, since);
        return new ReconciliationSummaryDto(rechargeAmount, rechargeCount, withdrawAmount, withdrawCount, fee);
    }

    private Instant startOfToday() {
        return LocalDate.now(ZoneId.systemDefault())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
    }
}
