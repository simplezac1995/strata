// SPDX-License-Identifier: Apache-2.0
// Split out of McpServers.tsx by scripts/auto-split.mjs.

export interface McpForm {
  name: string;
  description: string;
  type: string;
  url: string;
  headers: string;
  enabled: boolean;
}

export interface McpTool {
  name: string;
  description: string;
}
