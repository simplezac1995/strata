// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.dto;

import java.util.List;

/**
 * Result of a natural-language → read-only SQL round-trip: the generated {@code sql}, its projected columns
 * (name + JDBC type), up to 100 rows, and a lightweight {@code chartSuggestion} ({@code line}/{@code bar}/
 * {@code pie}/{@code null}) derived from the column types.
 */
public record Chat2SqlResponse(
        String sql, List<String> columns, List<java.util.Map<String, Object>> rows, String chartSuggestion) {}
