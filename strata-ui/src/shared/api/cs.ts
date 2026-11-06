// SPDX-License-Identifier: Apache-2.0
import { apiGet, apiPost, apiPut, toParams } from "@/shared/api/client";
import type { PageResult } from "@/shared/types";

/* ─── Customer-service workbench (contract-aligned, do not guess fields) ── */

export type CsConversationStatus = "open" | "assigned" | "closed";

export interface CsConversationDto {
  id: string;
  playerId: number;
  playerName: string;
  status: CsConversationStatus;
  lastMessageAt: string | null;
  lastMessagePreview: string | null;
  unreadCount: number;
  createdAt: string;
}

export interface CsMessageDto {
  id: string;
  conversationId: string;
  senderType: "player" | "staff" | "system";
  senderName: string;
  content: string;
  attachments: string[] | null;
  createdAt: string;
}

export interface CsSendMessagePayload {
  content: string;
  attachments?: string[];
}

export const csApi = {
  conversations: (status: CsConversationStatus | "", page: number, size: number) =>
    apiGet<PageResult<CsConversationDto>>(
      `/api/v1/system/cs/conversations${toParams({ status, page, size })}`,
    ),
  messages: (id: string) =>
    apiGet<CsMessageDto[]>(`/api/v1/system/cs/conversations/${id}/messages`),
  sendMessage: (id: string, payload: CsSendMessagePayload) =>
    apiPost<CsMessageDto>(`/api/v1/system/cs/conversations/${id}/messages`, payload),
  createConversation: (playerId: number) =>
    apiPost<CsConversationDto>(`/api/v1/system/cs/conversations${toParams({ playerId })}`),
  closeConversation: (id: string) => apiPut<void>(`/api/v1/system/cs/conversations/${id}/close`),
};
