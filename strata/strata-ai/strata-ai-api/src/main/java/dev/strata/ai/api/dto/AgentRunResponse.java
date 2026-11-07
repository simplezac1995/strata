// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.dto;

import java.util.List;
import java.util.Map;

/**
 * Result of one agent DAG execution: the run id, the final node output, the accumulated context map, and a
 * per-step trace for the frontend canvas.
 */
public record AgentRunResponse(String runId, String finalOutput, Map<String, Object> context, List<Step> steps) {

    /** One executed vertex in the run trace. */
    public record Step(String nodeKey, String status, String output) {}
}
