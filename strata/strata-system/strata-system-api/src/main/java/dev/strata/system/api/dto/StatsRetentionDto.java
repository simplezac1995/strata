// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

/**
 * Cohort retention (#6). Rates are in {@code [0, 1]}.
 *
 * <p>TODO(system-stats): true cohort retention needs per-user activity on day D+N. The admin login log
 * ({@code sys_login_log}) only records staff logins, not player sessions, and computing "logged in exactly
 * N days after registration" requires date arithmetic that is dialect-specific. Returned as {@code 0}
 * until a player activity table supports it.
 */
public record StatsRetentionDto(int days, double day1, double day7, double day30) {}
