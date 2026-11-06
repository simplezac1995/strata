// SPDX-License-Identifier: Apache-2.0
import { apiPost } from "@/shared/api/client";

/** One row returned by Chat2SQL: column name -> cell value. */
export type AiSqlRow = Record<string, string | number | boolean | null>;

/** Chart suggestion returned by the Chat2SQL endpoint. */
export interface AiSqlChartSuggestion {
  chartType: "line" | "bar" | "pie" | "none";
  categoryColumn?: string | null;
  valueColumns?: string[] | null;
}

/** Response of the Chat2SQL endpoint. */
export interface AiSqlChatResult {
  sql: string;
  columns: string[];
  rows: AiSqlRow[];
  chartSuggestion: AiSqlChartSuggestion | null;
}

export const aiSqlApi = {
  chat: (question: string) => apiPost<AiSqlChatResult>("/api/v1/ai/sql/chat2sql", { question }),
};
