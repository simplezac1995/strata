// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.util.List;

/**
 * Aggregated cross-entity search results (#15). Orders come from the casino wallet domain, which the staff
 * module must not depend on directly (ADR-0026); it is always empty pending a read contract. TODO(system-search):
 * wire casino payment-intent lookup once a staff-facing API contract exists.
 */
public record SearchResultsDto(
        List<SearchResultDto> users,
        List<SearchResultDto> roles,
        List<SearchResultDto> orders,
        List<SearchResultDto> pages) {

    public SearchResultsDto {
        if (users == null) {
            users = List.of();
        }
        if (roles == null) {
            roles = List.of();
        }
        if (orders == null) {
            orders = List.of();
        }
        if (pages == null) {
            pages = List.of();
        }
    }
}
