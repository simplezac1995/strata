// SPDX-License-Identifier: Apache-2.0
import { apiPost } from "@/shared/api/client";

/** A single cited source chunk attached to a QA answer. */
export interface AiQaCitation {
  documentId: string;
  documentName: string;
  chunkIndex: number;
  snippet: string;
  score?: number | null;
}

/** One turn in a RAG QA conversation. */
export interface AiQaMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  citations?: AiQaCitation[];
}

/** Response of the retrieve-and-generate endpoint. */
export interface AiQaRetrieveResult {
  answer: string;
  citations: AiQaCitation[];
}

export const aiQaApi = {
  retrieve: (question: string, knowledgeBaseId: string | null) =>
    apiPost<AiQaRetrieveResult>("/api/v1/ai/qa/retrieve", {
      question,
      knowledgeBaseId,
    }),
};
