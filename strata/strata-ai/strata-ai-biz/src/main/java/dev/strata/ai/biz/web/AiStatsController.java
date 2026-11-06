// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Aggregated AI usage stats for the dashboard. */
@RestController
@RequestMapping("/api/v1/ai/stats")
public class AiStatsController {

    private static final Logger log = LoggerFactory.getLogger(AiStatsController.class);
    private final JdbcTemplate jdbc;

    public AiStatsController(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
    }

    @GetMapping
    @PreAuthorize(Permissions.AI_BILLING_LIST)
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> out = new HashMap<>();
        try {
            List<Map<String, Object>> trend = jdbc.queryForList(
                    "SELECT DATE(created_at) d, SUM(prompt_tokens) p, SUM(completion_tokens) c, SUM(total_tokens) t "
                            + "FROM ai_llm_usage WHERE created_at >= DATE_SUB(NOW(), INTERVAL 14 DAY) "
                            + "GROUP BY DATE(created_at) ORDER BY d");
            out.put("trend", trend);
        } catch (Exception e) {
            log.warn("trend query failed: {}", e.getMessage());
            out.put("trend", List.of());
        }
        try {
            List<Map<String, Object>> byModel =
                    jdbc.queryForList("SELECT model, COUNT(*) calls, SUM(total_tokens) tokens FROM ai_llm_usage "
                            + "GROUP BY model ORDER BY calls DESC LIMIT 20");
            out.put("byModel", byModel);
        } catch (Exception e) {
            out.put("byModel", List.of());
        }
        try {
            List<Map<String, Object>> byAction =
                    jdbc.queryForList("SELECT action, COUNT(*) c FROM ai_llm_usage GROUP BY action");
            out.put("byAction", byAction);
        } catch (Exception e) {
            out.put("byAction", List.of());
        }
        try {
            Map<String, Object> totals = jdbc.queryForMap(
                    "SELECT COUNT(*) calls, COALESCE(SUM(total_tokens),0) tokens, COUNT(DISTINCT session_id) sessions FROM ai_llm_usage");
            out.put("totals", totals);
        } catch (Exception e) {
            out.put("totals", Map.of("calls", 0, "tokens", 0, "sessions", 0));
        }
        return Result.ok(out);
    }
}
