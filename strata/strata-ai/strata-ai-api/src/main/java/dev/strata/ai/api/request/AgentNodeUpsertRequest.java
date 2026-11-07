// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Replace the node graph of an agent. The list fully defines the DAG: each node carries its own outgoing edge
 * list ({@code nextNodeKeys}); the engine follows them in order.
 */
public record AgentNodeUpsertRequest(@NotEmpty @Valid List<NodeDef> nodes) {

    /** One vertex of the agent graph. */
    public record NodeDef(
            @NotBlank @Size(max = 64) String nodeKey,
            @NotBlank @Size(max = 16) String nodeType,
            @Size(max = 128) String name,
            Map<String, Object> config,
            BigDecimal positionX,
            BigDecimal positionY,
            @NotNull List<@NotBlank String> nextNodeKeys) {}
}
