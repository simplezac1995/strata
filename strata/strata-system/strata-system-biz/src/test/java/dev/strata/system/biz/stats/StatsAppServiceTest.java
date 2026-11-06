// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.system.api.dto.StatsOverviewDto;
import dev.strata.system.api.dto.StatsRetentionDto;
import dev.strata.system.api.dto.StatsTrendDto;
import dev.strata.system.ops.api.entity.LoginLog;
import dev.strata.system.ops.biz.repository.LoginLogRepository;
import dev.strata.system.rbac.biz.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatsAppService 单元测试")
@SuppressWarnings({"unchecked", "varargs"})
class StatsAppServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginLogRepository loginLogRepository;

    @InjectMocks
    private StatsAppService service;

    @Test
    @DisplayName("overview 汇总总数、新增对比与今日活跃")
    void overview_AggregatesTotals() {
        when(userRepository.countByDeletedFalse()).thenReturn(42L);
        when(userRepository.count(any(Specification.class))).thenReturn(5L).thenReturn(3L);
        when(loginLogRepository.countDistinctUsernameByStatusAndCreatedAtGreaterThanEqual(
                        eq(LoginLog.STATUS_SUCCESS), any(Instant.class)))
                .thenReturn(7L);

        StatsOverviewDto dto = service.overview("7d");

        assertThat(dto.totalUsers()).isEqualTo(42L);
        assertThat(dto.newUsersCurrent()).isEqualTo(5L);
        assertThat(dto.newUsersPrevious()).isEqualTo(3L);
        assertThat(dto.activeToday()).isEqualTo(7L);
        // Recharge/withdraw are TODO (casino not wired).
        assertThat(dto.rechargeTotalCount()).isZero();
        assertThat(dto.withdrawTotalCount()).isZero();
    }

    @Test
    @DisplayName("overview 非法 range 抛 BAD_REQUEST")
    void overview_BadRange_Throws() {
        assertThatThrownBy(() -> service.overview("1d"))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex -> assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    @DisplayName("user trend 按天分桶并返回 current/previous 两段")
    void userTrend_BucketsByDay() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant todayNoon = today.atTime(12, 0).toInstant(ZoneOffset.UTC);
        Instant yesterdayNoon = today.minusDays(1).atTime(12, 0).toInstant(ZoneOffset.UTC);
        when(userRepository.findCreatedAtAfter(any(Instant.class)))
                .thenReturn(List.of(todayNoon, todayNoon, yesterdayNoon));

        StatsTrendDto trend = service.trend("user", 7);

        assertThat(trend.metric()).isEqualTo("user");
        assertThat(trend.days()).isEqualTo(7);
        assertThat(trend.current()).hasSize(7);
        assertThat(trend.previous()).hasSize(7);
        assertThat(trend.current().get(6).value()).isEqualTo(2L); // today
        assertThat(trend.current().get(5).value()).isEqualTo(1L); // yesterday
    }

    @Test
    @DisplayName("recharge/withdraw trend 返回空序列（TODO）")
    void rechargeTrend_ReturnsEmpty() {
        StatsTrendDto recharge = service.trend("recharge", 30);
        assertThat(recharge.current()).isEmpty();
        assertThat(recharge.previous()).isEmpty();
    }

    @Test
    @DisplayName("未知 metric 抛 BAD_REQUEST")
    void trend_UnknownMetric_Throws() {
        assertThatThrownBy(() -> service.trend("balance", 30))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex -> assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    @DisplayName("retention 返回结构正确但全为 0（TODO）")
    void retention_ZerosWithStructure() {
        StatsRetentionDto dto = service.retention(30);
        assertThat(dto.days()).isEqualTo(30);
        assertThat(dto.day1()).isZero();
        assertThat(dto.day7()).isZero();
        assertThat(dto.day30()).isZero();
    }

    @Test
    @DisplayName("days 越界抛 BAD_REQUEST")
    void retention_OutOfRange_Throws() {
        assertThatThrownBy(() -> service.retention(0))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex -> assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }
}
