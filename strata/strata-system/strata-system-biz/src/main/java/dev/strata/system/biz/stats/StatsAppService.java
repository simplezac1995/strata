// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.stats;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.system.api.dto.StatsOverviewDto;
import dev.strata.system.api.dto.StatsRetentionDto;
import dev.strata.system.api.dto.StatsTrendDto;
import dev.strata.system.api.dto.StatsTrendPointDto;
import dev.strata.system.ops.api.entity.LoginLog;
import dev.strata.system.ops.biz.repository.LoginLogRepository;
import dev.strata.system.rbac.biz.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only operations aggregation (#6). Sourced entirely from the staff side: {@code sys_user} for
 * registrations and {@code sys_login_log} for active users. Recharge/withdraw and true cohort retention
 * are not computable here (see DTO TODOs); they return zeros rather than guessed numbers.
 */
@Service
public class StatsAppService {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final UserRepository userRepository;
    private final LoginLogRepository loginLogRepository;

    public StatsAppService(UserRepository userRepository, LoginLogRepository loginLogRepository) {
        this.userRepository = userRepository;
        this.loginLogRepository = loginLogRepository;
    }

    @Transactional(readOnly = true)
    public StatsOverviewDto overview(String range) {
        int days = parseRange(range);
        Instant now = Instant.now();
        Instant currentFrom = now.minusSeconds(days * 86400L);
        Instant previousFrom = currentFrom.minusSeconds(days * 86400L);

        long total = userRepository.countByDeletedFalse();
        long currentNew = countUsersBetween(currentFrom, now);
        long previousNew = countUsersBetween(previousFrom, currentFrom);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant startOfToday = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        long activeToday = loginLogRepository.countDistinctUsernameByStatusAndCreatedAtGreaterThanEqual(
                LoginLog.STATUS_SUCCESS, startOfToday);

        // TODO(system-stats): recharge/withdraw totals live in the casino wallet domain, which the staff
        // module must not depend on directly (ADR-0026). Return zeros until a read contract exists.
        return new StatsOverviewDto(total, currentNew, previousNew, activeToday, 0, 0, 0, 0);
    }

    @Transactional(readOnly = true)
    public StatsTrendDto trend(String metric, int days) {
        if (days <= 0 || days > 366) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "days must be between 1 and 366");
        }
        String normalized = metric == null ? "user" : metric.trim().toLowerCase();
        return switch (normalized) {
            case "user" -> userTrend(days);
            // TODO(system-stats): recharge/withdraw daily series needs casino wallet access + dialect-agnostic
            // date bucketing. Return empty series until a read contract exists.
            case "recharge", "withdraw" -> new StatsTrendDto(normalized, days, List.of(), List.of());
            default ->
                throw new BusinessException(
                        ErrorCode.BAD_REQUEST, "Unknown metric: " + metric + " (expected user|recharge|withdraw)");
        };
    }

    @Transactional(readOnly = true)
    public StatsRetentionDto retention(int days) {
        if (days <= 0 || days > 366) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "days must be between 1 and 366");
        }
        // TODO(system-stats): true cohort retention requires per-user activity on day D+N. The staff login
        // log does not record player sessions and day-offset date math is dialect-specific. Return zeros.
        return new StatsRetentionDto(days, 0.0, 0.0, 0.0);
    }

    private StatsTrendDto userTrend(int days) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        // Pull registrations covering both the current and previous windows.
        Instant floor = today.minusDays(2L * days).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<Instant> registered = userRepository.findCreatedAtAfter(floor);

        Map<LocalDate, Long> buckets = new TreeMap<>();
        for (Instant instant : registered) {
            LocalDate day = instant.atZone(ZoneOffset.UTC).toLocalDate();
            buckets.merge(day, 1L, Long::sum);
        }

        List<StatsTrendPointDto> current = new ArrayList<>(days);
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            current.add(new StatsTrendPointDto(DAY_FORMATTER.format(day), buckets.getOrDefault(day, 0L)));
        }
        List<StatsTrendPointDto> previous = new ArrayList<>(days);
        for (int i = 2 * days - 1; i >= days; i--) {
            LocalDate day = today.minusDays(i);
            previous.add(new StatsTrendPointDto(DAY_FORMATTER.format(day), buckets.getOrDefault(day, 0L)));
        }
        return new StatsTrendDto("user", days, current, previous);
    }

    private long countUsersBetween(Instant from, Instant to) {
        Specification<dev.strata.system.rbac.api.entity.User> spec = (root, query, cb) -> cb.and(
                cb.isFalse(root.get("deleted")),
                cb.greaterThanOrEqualTo(root.get("createdAt"), from),
                cb.lessThan(root.get("createdAt"), to));
        return userRepository.count(spec);
    }

    private static int parseRange(String range) {
        String value = range == null ? "7d" : range.trim().toLowerCase();
        return switch (value) {
            case "7d" -> 7;
            case "30d" -> 30;
            case "90d" -> 90;
            default ->
                throw new BusinessException(
                        ErrorCode.BAD_REQUEST, "range must be one of 7d, 30d, 90d (got: " + range + ")");
        };
    }
}
