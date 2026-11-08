// SPDX-License-Identifier: Apache-2.0
// WebSocket type definitions for Strata Admin

/**
 * WebSocket message types
 */
export type WebSocketMessageType =
  "notification" | "chat" | "data" | "event" | "error" | "ping" | "pong";

/**
 * WebSocket message envelope (matches backend WebSocketMessage record)
 */
export interface WebSocketMessage {
  type: WebSocketMessageType;
  topic: string | null;
  sender: string;
  recipient: string | null;
  payload: unknown;
  metadata: Record<string, unknown>;
  timestamp: string;
  messageId: string;
}

/**
 * WebSocket connection state
 */
export type WebSocketState =
  "connecting" | "connected" | "disconnecting" | "disconnected" | "reconnecting" | "error";

/**
 * WebSocket configuration options
 */
export interface WebSocketOptions {
  /** WebSocket endpoint URL (default: /ws) */
  url?: string;
  /** Whether to auto-reconnect on disconnect (default: true) */
  autoReconnect?: boolean;
  /** Maximum reconnection attempts (default: 10) */
  maxReconnectAttempts?: number;
  /** Initial reconnection delay in ms (default: 1000) */
  reconnectDelay?: number;
  /** Maximum reconnection delay in ms (default: 30000) */
  maxReconnectDelay?: number;
  /** Heartbeat interval in ms (default: 30000) */
  heartbeatInterval?: number;
  /** Whether to enable debug logging (default: false) */
  debug?: boolean;
}

/**
 * Notification payload structure
 */
export interface NotificationPayload {
  id: string;
  title: string;
  message: string;
  type: "info" | "success" | "warning" | "error";
  read: boolean;
  createdAt: string;
  actionUrl?: string;
}

/**
 * WebSocket event listener type
 */
export type WebSocketEventListener = (event: WebSocketMessage | WebSocketState | Event) => void;
