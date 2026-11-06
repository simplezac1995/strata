// SPDX-License-Identifier: Apache-2.0
// Types and defaults for the inspection (offline document check) page.

export interface InspectionTask {
  id: string;
  name: string;
  logDir: string;
  fileMatch: string;
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
  "你是一名资深 SRE，请分析指定时间范围内的日志，提取 ERROR / WARN 级别异常，输出概览摘要与处置建议。";

export const EMPTY_FORM = {
  name: "",
  enabled: true,
  logDir: "",
  fileMatch: "*.log",
  timeRange: "today",
  systemPrompt: DEFAULT_SYSTEM_PROMPT,
};

export type RunPhase = "idle" | "analyzing" | "done";
