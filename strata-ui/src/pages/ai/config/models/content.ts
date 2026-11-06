// SPDX-License-Identifier: Apache-2.0
// Tab definitions and form shapes for the model configuration page.

export const MODEL_TYPE_COLOR: Record<
  string,
  "primary" | "info" | "secondary" | "warning" | "default"
> = {
  chat: "primary",
  embedding: "info",
  image: "secondary",
  audio: "warning",
  vision: "warning",
};

/** Left-panel capability tabs (pigcloud 模型配置). Extra types render to an empty table. */
export const TYPE_TABS = [
  { value: "all", label: "全部" },
  { value: "chat", label: "聊天" },
  { value: "embedding", label: "向量" },
  { value: "rerank", label: "排序" },
  { value: "image", label: "图片" },
  { value: "video", label: "视频" },
  { value: "audio", label: "音频" },
  { value: "search", label: "搜索" },
  { value: "agent", label: "Agent" },
  { value: "parse", label: "解析" },
];

export const PROVIDER_TYPES = [
  { value: "openai", label: "OpenAI" },
  { value: "anthropic", label: "Anthropic" },
  { value: "azure", label: "Azure OpenAI" },
  { value: "ollama", label: "Ollama" },
  { value: "custom", label: "Custom" },
];

export interface ProviderForm {
  code: string;
  name: string;
  type: string;
  baseUrl: string;
  apiKey: string;
  enabled: boolean;
}

export interface ModelForm {
  providerId: string;
  code: string;
  name: string;
  type: string;
  enabled: boolean;
}

export const PAGE_SIZE = 20;
