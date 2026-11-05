// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.system.api.dto.GameFlowPointDto;
import dev.strata.system.api.dto.RealtimeDashboardDto;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardAppService 单元测试")
class DashboardAppServiceTest {

    @InjectMocks
    private DashboardAppService service;

    @Test
    @DisplayName("实时大盘返回全零骨架（跨模块聚合未接入）")
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
    @DisplayName("游戏流水趋势返回空序列")
    void gameFlow_ReturnsEmptySeries() {
        List<GameFlowPointDto> points = service.gameFlow(7);

        assertThat(points).isEmpty();
    }

    @Test
    @DisplayName("非法天数参数回落默认窗口，仍返回空序列")
    void gameFlow_NonPositiveDays_StillEmpty() {
        assertThat(service.gameFlow(0)).isEmpty();
        assertThat(service.gameFlow(-3)).isEmpty();
    }
}
