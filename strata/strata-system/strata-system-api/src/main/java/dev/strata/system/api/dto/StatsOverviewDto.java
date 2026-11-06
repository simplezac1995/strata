// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

/**
 * Operations overview snapshot (#6).
 *
 * <p>Recharge/withdraw totals are sourced from the casino wallet domain, which the staff system module must
 * not depend on directly (ADR-0026 / ArchitectureRulesTest). They return {@code 0} here pending a
 * cross-domain read contract. TODO(system-stats): wire casino wallet read model once a staff-facing API
 * contract exists.
 */
public record StatsOverviewDto(
        long totalUsers,
        long newUsersCurrent,
        long newUsersPrevious,
        long activeToday,
        long rechargeTotalAmountMinor,
        long rechargeTotalCount,
        long withdrawTotalAmountMinor,
        long withdrawTotalCount) {}
