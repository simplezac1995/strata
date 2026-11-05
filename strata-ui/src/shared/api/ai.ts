// SPDX-License-Identifier: Apache-2.0
import { apiDelete, apiGet, apiPost, apiPut, toParams } from "@/shared/api/client";
import type {
  AiAgentDto,
  AiChatMessageDto,
  AiChatSessionDto,
  AiDatasetDto,
  AiDocumentDto,
  AiKnowledgeDto,
  AiMcpServerDto,
  AiModelDto,
  AiPromptDto,
  AiProviderDto,
  AiSkillDto,
  AiTermDto,
  AiVectorStoreDto,
  PageResult,
} from "@/shared/types";

export interface AiProviderPayload {
  code: string;
  name: string;
  type: string;
  baseUrl: string | null;
  apiKey: string | null;
  enabled: boolean;
}

export const providerApi = {
  list: () => apiGet<AiProviderDto[]>("/api/v1/ai/providers"),
  page: (page: number, size: number) =>
    apiGet<PageResult<AiProviderDto>>(`/api/v1/ai/providers/page?page=${page}&size=${size}`),
  create: (payload: AiProviderPayload) => apiPost<void>("/api/v1/ai/providers", payload),
  update: (id: string, payload: AiProviderPayload) =>
    apiPut<void>(`/api/v1/ai/providers/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/ai/providers/${id}`),
};

export interface AiModelPayload {
  providerId: string;
  code: string;
  name: string;
  type: string;
  enabled: boolean;
}

export interface AiModelUpdatePayload {
  name: string;
  type: string;
  enabled: boolean;
}

export const modelApi = {
  list: () => apiGet<AiModelDto[]>("/api/v1/ai/models"),
  page: (page: number, size: number) =>
    apiGet<PageResult<AiModelDto>>(`/api/v1/ai/models/page?page=${page}&size=${size}`),
  create: (payload: AiModelPayload) => apiPost<void>("/api/v1/ai/models", payload),
  update: (id: string, payload: AiModelUpdatePayload) =>
    apiPut<void>(`/api/v1/ai/models/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/ai/models/${id}`),
};

export interface AiPromptPayload {
  code: string;
  name: string;
  template: string;
  enabled: boolean;
}

export const promptApi = {
  list: () => apiGet<AiPromptDto[]>("/api/v1/ai/prompts"),
  page: (page: number, size: number) =>
    apiGet<PageResult<AiPromptDto>>(`/api/v1/ai/prompts/page?page=${page}&size=${size}`),
  create: (payload: AiPromptPayload) => apiPost<void>("/api/v1/ai/prompts", payload),
  update: (id: string, payload: AiPromptPayload) =>
    apiPut<void>(`/api/v1/ai/prompts/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/ai/prompts/${id}`),
};

export interface AiTermPayload {
  term: string;
  translation: string;
  notes: string | null;
  enabled: boolean;
}

export interface AiTermUpdatePayload {
  translation: string;
  notes: string | null;
  enabled: boolean;
}

export const termApi = {
  list: () => apiGet<AiTermDto[]>("/api/v1/ai/terms"),
  page: (page: number, size: number) =>
    apiGet<PageResult<AiTermDto>>(`/api/v1/ai/terms/page?page=${page}&size=${size}`),
  create: (payload: AiTermPayload) => apiPost<void>("/api/v1/ai/terms", payload),
  update: (id: string, payload: AiTermUpdatePayload) =>
    apiPut<void>(`/api/v1/ai/terms/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/ai/terms/${id}`),
};

export interface AiVectorStorePayload {
  name: string;
  type: string;
  host: string | null;
  dimensions: number;
  enabled: boolean;
}

export const vectorStoreApi = {
  list: () => apiGet<AiVectorStoreDto[]>("/api/v1/ai/vector-stores"),
  page: (page: number, size: number) =>
    apiGet<PageResult<AiVectorStoreDto>>(`/api/v1/ai/vector-stores/page?page=${page}&size=${size}`),
  create: (payload: AiVectorStorePayload) => apiPost<void>("/api/v1/ai/vector-stores", payload),
  update: (id: string, payload: AiVectorStorePayload) =>
    apiPut<void>(`/api/v1/ai/vector-stores/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/ai/vector-stores/${id}`),
};

export interface AiKnowledgePayload {
  name: string;
  description: string | null;
  embeddingModel: string | null;
  vectorStore: string | null;
  chunkSize: number;
  chunkOverlap: number;
  enabled: boolean;
}

export interface AiDocumentPayload {
  knowledgeId: string;
  name: string;
  sizeKb: number;
  status: string;
  chunks: number;
  enabled: boolean;
}

export interface AiDocumentUpdatePayload {
  name: string;
  sizeKb: number;
  status: string;
  chunks: number;
  enabled: boolean;
}

export const knowledgeApi = {
  list: () => apiGet<AiKnowledgeDto[]>("/api/v1/ai/knowledge-bases"),
  page: (page: number, size: number) =>
    apiGet<PageResult<AiKnowledgeDto>>(
      `/api/v1/ai/knowledge-bases/page${toParams({ page, size })}`,
    ),
  create: (payload: AiKnowledgePayload) => apiPost<void>("/api/v1/ai/knowledge-bases", payload),
  update: (id: string, payload: AiKnowledgePayload) =>
    apiPut<void>(`/api/v1/ai/knowledge-bases/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/ai/knowledge-bases/${id}`),
};

export const documentApi = {
  listAll: () => apiGet<AiDocumentDto[]>("/api/v1/ai/documents"),
  listByKnowledge: (knowledgeId: string) =>
    apiGet<AiDocumentDto[]>(`/api/v1/ai/documents${toParams({ knowledgeId })}`),
  page: (page: number, size: number, knowledgeId?: string) =>
    apiGet<PageResult<AiDocumentDto>>(
      `/api/v1/ai/documents/page${toParams({ page, size, knowledgeId })}`,
    ),
  create: (payload: AiDocumentPayload) => apiPost<void>("/api/v1/ai/documents", payload),
  update: (id: string, payload: AiDocumentUpdatePayload) =>
    apiPut<void>(`/api/v1/ai/documents/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/ai/documents/${id}`),
};

export interface AiSkillPayload {
  name: string;
  type: string;
  path: string | null;
  description: string | null;
  enabled: boolean;
}

export interface AiSkillUpdatePayload {
  name: string;
  path: string | null;
  description: string | null;
  enabled: boolean;
}

export interface AiMcpServerPayload {
  name: string;
  url: string;
  authType: string | null;
  authToken: string | null;
  enabled: boolean;
}

export interface AiAgentPayload {
  name: string;
  model: string | null;
  prompt: string | null;
  enabled: boolean;
}

export const skillApi = {
  list: () => apiGet<AiSkillDto[]>("/api/v1/ai/skills"),
  page: (type: string, page: number, size: number) =>
    apiGet<PageResult<AiSkillDto>>(`/api/v1/ai/skills/page?type=${type}&page=${page}&size=${size}`),
  create: (payload: AiSkillPayload) => apiPost<void>("/api/v1/ai/skills", payload),
  update: (id: string, payload: AiSkillUpdatePayload) =>
    apiPut<void>(`/api/v1/ai/skills/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/ai/skills/${id}`),
};

export const mcpServerApi = {
  list: () => apiGet<AiMcpServerDto[]>("/api/v1/ai/mcp-servers"),
  page: (page: number, size: number) =>
    apiGet<PageResult<AiMcpServerDto>>(`/api/v1/ai/mcp-servers/page?page=${page}&size=${size}`),
  create: (payload: AiMcpServerPayload) => apiPost<void>("/api/v1/ai/mcp-servers", payload),
  update: (id: string, payload: AiMcpServerPayload) =>
    apiPut<void>(`/api/v1/ai/mcp-servers/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/ai/mcp-servers/${id}`),
  test: (id: string) => apiPost<Record<string, unknown>>(`/api/v1/ai/mcp-servers/${id}/test`),
  tools: (id: string) =>
    apiGet<Array<{ name: string; description: string }>>(`/api/v1/ai/mcp-servers/${id}/tools`),
};

export const agentApi = {
  list: () => apiGet<AiAgentDto[]>("/api/v1/ai/agents"),
  page: (page: number, size: number) =>
    apiGet<PageResult<AiAgentDto>>(`/api/v1/ai/agents/page?page=${page}&size=${size}`),
  create: (payload: AiAgentPayload) => apiPost<void>("/api/v1/ai/agents", payload),
  update: (id: string, payload: AiAgentPayload) => apiPut<void>(`/api/v1/ai/agents/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/ai/agents/${id}`),
};

export interface AiDatasetPayload {
  name: string;
  description: string | null;
  source: string;
  records: number;
  enabled: boolean;
}

export const datasetApi = {
  list: () => apiGet<AiDatasetDto[]>("/api/v1/ai/datasets"),
  page: (page: number, size: number) =>
    apiGet<PageResult<AiDatasetDto>>(`/api/v1/ai/datasets/page?page=${page}&size=${size}`),
  create: (payload: AiDatasetPayload) => apiPost<void>("/api/v1/ai/datasets", payload),
  update: (id: string, payload: AiDatasetPayload) =>
    apiPut<void>(`/api/v1/ai/datasets/${id}`, payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/ai/datasets/${id}`),
};

export const aiChatApi = {
  listSessions: () => apiGet<AiChatSessionDto[]>("/api/v1/ai/chat/sessions"),
  createSession: (title: string, modelId?: string | null) =>
    apiPost<AiChatSessionDto>("/api/v1/ai/chat/sessions", { title, modelId: modelId ?? null }),
  renameSession: (id: string, title: string) =>
    apiPut<AiChatSessionDto>(`/api/v1/ai/chat/sessions/${id}`, { title }),
  deleteSession: (id: string) => apiDelete<void>(`/api/v1/ai/chat/sessions/${id}`),
  listMessages: (sessionId: string) =>
    apiGet<AiChatMessageDto[]>(`/api/v1/ai/chat/sessions/${sessionId}/messages`),
  sendMessage: (sessionId: string, content: string, attachments: AiChatAttachment[] = []) =>
    apiPost<AiChatMessageDto>(`/api/v1/ai/chat/sessions/${sessionId}/messages`, {
      content,
      attachments,
    }),
  sendMessageStream: async (
    sessionId: string,
    content: string,
    onDelta: (chunk: string) => void,
  ): Promise<void> => {
    const token = localStorage.getItem("strata_access_token") ?? "";
    const resp = await fetch(`/api/v1/ai/chat/sessions/${sessionId}/messages/stream`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ content, attachments: [] }),
    });
    if (!resp.ok || !resp.body) {
      throw new Error(`Stream failed: ${resp.status}`);
    }
    const reader = resp.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    let finished = false;
    while (!finished) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split("\n");
      buffer = lines.pop() ?? "";
      for (const line of lines) {
        if (line.startsWith("data:")) {
          const data = line.slice(5);
          if (data.trim() === "[DONE]") {
            finished = true;
            break;
          }
          if (data) onDelta(data);
        }
      }
    }
    try {
      reader.releaseLock();
    } catch {}
  },
};

/** Reference to an uploaded file attached to a chat message (multimodal input). */
export type AiChatAttachment = {
  bucket: string;
  key: string;
  contentType: string | null;
  size: number;
  fileName: string;
};

export type AiToolName =
  | "proofread"
  | "safety"
  | "code-review"
  | "outline"
  | "rewrite"
  | "image"
  | "tts"
  | "video"
  | "poster"
  | "ocr";

export type AiToolRunDto = { tool: string; result: string; usedLlm: boolean };
export type AiUsageDto = {
  sessionCount: number;
  messageCount: number;
  providerCount: number;
  modelCount: number;
  documentCount: number;
};

export const aiToolApi = {
  usage: () => apiGet<AiUsageDto>("/api/v1/ai/tools/usage"),
  run: (tool: AiToolName, content: string) =>
    apiPost<AiToolRunDto>("/api/v1/ai/tools/run", { tool, content }),
};

export interface AiOfficeResponse {
  content: string;
  usage: { promptTokens: number; completionTokens: number; totalTokens: number };
}

export const officeApi = {
  run: (action: string, text: string) =>
    apiPost<AiOfficeResponse>(`/api/v1/ai/office/${action}`, { text }),
};
