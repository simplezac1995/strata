// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Kick off one execution of an agent's DAG with the user-supplied entry input. */
public record AgentRunRequest(@NotBlank @Size(max = 4000) String input) {}
