// SPDX-License-Identifier: Apache-2.0
// Split out of CodeReview.tsx by scripts/auto-split.mjs.

export interface ReviewTask {
  id: string;
  name: string;
  gitUrl: string;
  branch: string;
  timeRange: string;
  enabled: boolean;
  lastRunAt: string;
  createdAt: string;
  systemPrompt: string;
}

export interface RunLog {
  time: string;
  tool: string;
  command: string;
}

export interface RunRecord {
  id: string;
  taskName: string;
  finishedAt: string;
  durationSec: number;
  logs: RunLog[];
  report: string;
}

export const TIME_RANGES = [
  { value: "1h", label: "最近 1 小时" },
  { value: "today", label: "今天" },
  { value: "yesterday", label: "昨天" },
  { value: "3d", label: "最近 3 天" },
  { value: "7d", label: "最近 7 天" },
];

export const DEFAULT_SYSTEM_PROMPT =
  "你是一名资深代码评审专家，请分析指定时间范围内的代码变更，识别潜在 Bug、安全风险与可维护性问题，并给出改进建议。";

export const EMPTY_FORM = {
  name: "",
  enabled: true,
  gitUrl: "",
  branch: "main",
  timeRange: "today",
  username: "",
  token: "",
  systemPrompt: DEFAULT_SYSTEM_PROMPT,
};

export type RunPhase = "idle" | "analyzing" | "done";
