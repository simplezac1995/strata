// SPDX-License-Identifier: Apache-2.0
import { apiPost, apiPut } from "@/shared/api/client";

/** Node kinds available on the agent canvas. */
export type AiAgentNodeType = "llm" | "tool" | "branch" | "subagent";

/** A single node in the agent task-decomposition chain. */
export interface AiAgentCanvasNode {
  id: string;
  type: AiAgentNodeType;
  label: string;
  config: Record<string, unknown>;
}

/** One step in an agent run timeline. */
export interface AiAgentRunStep {
  nodeId: string;
  label: string;
  status: "pending" | "running" | "success" | "failed" | "skipped";
  startedAt?: string | null;
  finishedAt?: string | null;
  output?: string | null;
  error?: string | null;
}

/** Response when running an agent canvas. */
export interface AiAgentRunResult {
  runId: string;
  finalOutput: string;
  context: Record<string, unknown>;
  steps: AiAgentRunStep[];
}

export const aiAgentCanvasApi = {
  run: (agentId: string, input: string) =>
    apiPost<AiAgentRunResult>(`/api/v1/ai/agents/${agentId}/run`, { input }),
  saveNodes: (agentId: string, nodes: AiAgentCanvasNode[]) =>
    apiPut<void>(`/api/v1/ai/agents/${agentId}/nodes`, { nodes }),
};
