// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure helpers for the Chat2SQL flow: prompt assembly, extracting the first {@code SELECT} from a model
 * reply, enforcing the read-only contract, and inferring a chart hint from column names.
 */
final class Chat2SqlSupport {

    /** Statements a Chat2SQL endpoint must never execute. */
    private static final Pattern FORBIDDEN = Pattern.compile(
            "\\b(insert|update|delete|drop|alter|grant|truncate|create|replace|merge|call|exec|execute|revoke|lock|unlock|attach|detach)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SELECT_FINDER = Pattern.compile("\\bselect\\b", Pattern.CASE_INSENSITIVE);

    private Chat2SqlSupport() {}

    /** Build the system prompt that forces SELECT-only output against the live schema. */
    static String buildSystemPrompt(String schemaHint) {
        String schema = (schemaHint == null || schemaHint.isBlank())
                ? "Use only tables/columns from the live schema provided above."
                : schemaHint.trim();
        return "You are a SQL expert. Generate a SELECT-only query against a PostgreSQL database. "
                + "Use PostgreSQL syntax (e.g. NOW() - INTERVAL '7 days'). "
                + "Return ONLY the SQL, no explanation. "
                + "CRITICAL: Only use table and column names that actually appear in the live schema below. "
                + "Never invent columns like `deleted` if they are not listed. "
                + "Table schemas: "
                + schema;
    }

    static String extractSelect(String llmOutput) {
        String cleaned = stripCodeFence(llmOutput == null ? "" : llmOutput);
        Matcher matcher = SELECT_FINDER.matcher(cleaned);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Model reply did not contain a SELECT statement");
        }
        int start = matcher.start();
        int semi = cleaned.indexOf(';', start);
        String sql = semi < 0 ? cleaned.substring(start) : cleaned.substring(start, semi);
        sql = sql.trim();
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        return sql;
    }

    static boolean isReadOnlySelect(String sql) {
        if (sql == null || sql.isBlank()) {
            return false;
        }
        String trimmed = sql.stripLeading();
        String upper = trimmed.toUpperCase(Locale.ROOT);
        boolean startsWithSelect = upper.startsWith("SELECT");
        boolean startsWithCte = upper.startsWith("WITH") && upper.contains("SELECT");
        if (!startsWithSelect && !startsWithCte) {
            return false;
        }
        return !FORBIDDEN.matcher(upper).find();
    }

    /**
     * Infer a chart suggestion from column names:
     * first column looks temporal and 2+ numeric-ish columns → {@code line};
     * exactly one numeric-ish column → {@code bar};
     * otherwise {@code null}.
     */
    static String suggestChart(List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            return null;
        }
        int numeric = 0;
        for (String col : columns) {
            if (isNumericName(col)) {
                numeric++;
            }
        }
        String first = columns.get(0) == null ? "" : columns.get(0).toLowerCase(Locale.ROOT);
        boolean firstIsTemporal = first.contains("date") || first.contains("time") || first.contains("day");
        if (firstIsTemporal && numeric >= 2) {
            return "line";
        }
        if (numeric == 1) {
            return "bar";
        }
        return null;
    }

    private static boolean isNumericName(String name) {
        if (name == null) return false;
        String n = name.toLowerCase(Locale.ROOT);
        return n.contains("count")
                || n.contains("total")
                || n.contains("sum")
                || n.contains("amount")
                || n.contains("avg")
                || n.contains("balance")
                || n.contains("price")
                || n.contains("new_")
                || n.contains("cnt");
    }

    private static String stripCodeFence(String text) {
        String out = text.trim();
        if (out.startsWith("```")) {
            int newline = out.indexOf('\n');
            if (newline >= 0) {
                out = out.substring(newline + 1);
            }
            int fence = out.lastIndexOf("```");
            if (fence >= 0) {
                out = out.substring(0, fence);
            }
        }
        return out;
    }
}
