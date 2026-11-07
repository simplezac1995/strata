// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.system.api.dto.GameFlowPointDto;
import dev.strata.system.api.dto.RealtimeDashboardDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardAppServiceTest {

    @InjectMocks
    private DashboardAppService service;

    @Test
    void realtime_ReturnsZeroShape() {
        RealtimeDashboardDto dto = service.realtime();

        assertThat(dto.onlineCount()).isZero();
        assertThat(dto.todayRechargeAmountMinor()).isZero();
        assertThat(dto.todayRechargeCount()).isZero();
        assertThat(dto.todayWithdrawAmountMinor()).isZero();
        assertThat(dto.todayWithdrawCount()).isZero();
        assertThat(dto.activeUsersToday()).isZero();
    }

    @Test
    void gameFlow_ReturnsEmptySeries() {
        List<GameFlowPointDto> points = service.gameFlow(7);

        assertThat(points).isEmpty();
    }

    @Test
    void gameFlow_NonPositiveDays_StillEmpty() {
        assertThat(service.gameFlow(0)).isEmpty();
        assertThat(service.gameFlow(-3)).isEmpty();
    }
}
