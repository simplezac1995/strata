// SPDX-License-Identifier: Apache-2.0
import { apiDelete, apiGet, apiPost, toParams } from "@/shared/api/client";

/* ─── Support tickets + replies ───────────────────────────────────────── */

export type TicketStatus = "OPEN" | "PROCESSING" | "RESOLVED" | "CLOSED";

export interface TicketDto {
  id: string;
  subject: string;
  status: TicketStatus;
  /** Requester display name / username. */
  customer: string;
  /** Initial body of the ticket. */
  content: string;
  createdAt: string;
}

export interface TicketReplyDto {
  id: string;
  ticketId: string;
  author: string;
  content: string;
  /** Internal note (not visible to the customer). */
  internal: boolean;
  createdAt: string;
}

export interface TicketPayload {
  subject: string;
  content: string;
}

export interface TicketReplyPayload {
  content: string;
  internal: boolean;
}

export const ticketApi = {
  list: (status?: TicketStatus, keyword?: string) =>
    apiGet<TicketDto[]>(`/api/v1/tickets${toParams({ status, keyword })}`),
  get: (id: string) => apiGet<TicketDto>(`/api/v1/tickets/${id}`),
  create: (payload: TicketPayload) => apiPost<void>("/api/v1/tickets", payload),
  remove: (id: string) => apiDelete<void>(`/api/v1/tickets/${id}`),
  replies: (id: string) => apiGet<TicketReplyDto[]>(`/api/v1/tickets/${id}/replies`),
  reply: (id: string, payload: TicketReplyPayload) =>
    apiPost<void>(`/api/v1/tickets/${id}/replies`, payload),
};
