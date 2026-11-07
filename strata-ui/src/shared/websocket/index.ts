// SPDX-License-Identifier: Apache-2.0
// WebSocket module exports

export { WebSocketClient, getWebSocketClient, getDefaultWebSocketClient } from "./WebSocketClient";
export { useWebSocket, useWebSocketStatus, useNotifications } from "./useWebSocket";
export type {
  WebSocketMessage,
  WebSocketMessageType,
  WebSocketState,
  WebSocketOptions,
  NotificationPayload,
  WebSocketEventListener,
} from "./types";
