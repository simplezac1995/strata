// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import dev.strata.system.api.dto.GameFlowPointDto;
import dev.strata.system.api.dto.RealtimeDashboardDto;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operations data dashboard.
 *
 * <p>The realtime and game-flow aggregates are sourced from the casino wallet/player/game tables. This module
 * (strata-system-ops-biz) must not depend on strata-casino-*-api, so the cross-module read model is not wired
 * in this batch. Both endpoints return the zero/empty shape so the frontend contract is stable; TODO: build a
 * shared read-model (or application-service lookup) that aggregates online players, today's recharge/withdraw
 * and the per-day game bet/payout flow.
 */
@Service
public class DashboardAppService {

    private static final Logger log = LoggerFactory.getLogger(DashboardAppService.class);

    @Transactional(readOnly = true)
    public RealtimeDashboardDto realtime() {
        // TODO(cross-module): aggregate casino wallet/player for onlineCount, today recharge/withdraw counts
        //  and activeUsersToday. Returning zeros until the read model exists.
        return new RealtimeDashboardDto(0, 0, 0, 0, 0, 0);
    }

    @Transactional(readOnly = true)
    public List<GameFlowPointDto> gameFlow(int days) {
        // TODO(cross-module): aggregate casino BetRound for per-day bet/payout amount and distinct players.
        //  Returning an empty series until the read model exists.
        int window = days <= 0 ? 7 : days;
        log.debug("game-flow requested for {} days (cross-module aggregation not wired)", window);
        return List.of();
    }
}
