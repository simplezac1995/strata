// SPDX-License-Identifier: Apache-2.0
import { apiPost } from "@/shared/api/client";

/** Office workspace actions (文案生成 / 改写 / 翻译 / 扩写 / 报告). */
export type AiOfficeAction = "draft" | "rewrite" | "translate" | "expand" | "report";

/** Token usage returned by the LLM gateway for one invocation. */
export interface AiUsage {
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
}

export interface AiOfficeResult {
  action: AiOfficeAction;
  result: string;
  usage: AiUsage;
}

export interface AiOfficeRequest {
  input: string;
  /** Optional target language for the translate action. */
  targetLang?: string | null;
  /** Optional tone / style hint for draft / rewrite actions. */
  tone?: string | null;
}

/**
 * The backend office endpoint expects action `generate` (the UI labels it "draft") and a `text` body field,
 * whereas the UI uses `input`. Translate at this boundary so the component stays unchanged.
 */
export const aiOfficeApi = {
  run: (action: AiOfficeAction, request: AiOfficeRequest) => {
    const backendAction = action === "draft" ? "generate" : action;
    return apiPost<AiOfficeResult>(`/api/v1/ai/office/${backendAction}`, {
      text: request.input,
      tone: request.tone ?? null,
    });
  },
};
