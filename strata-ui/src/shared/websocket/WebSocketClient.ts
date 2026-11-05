// SPDX-License-Identifier: Apache-2.0
import { debug, error as logError } from "@/shared/logger";
import type {
  WebSocketMessage,
  WebSocketMessageType,
  WebSocketOptions,
  WebSocketState,
} from "./types";

/**
 * WebSocket client service for real-time communication.
 *
 * Features:
 * - Auto-reconnect with exponential backoff
 * - Heartbeat (ping/pong)
 * - Message type-based event listeners
 * - Connection state tracking
 * - Debug logging
 *
 * Usage:
 * ```tsx
 * const ws = new WebSocketClient({ url: '/ws', debug: true });
 *
 * ws.on('notification', (msg) => {
 *   console.log('Notification:', msg.payload);
 * });
 *
 * ws.connect();
 *
 * // Later
 * ws.disconnect();
 * ```
 */
export class WebSocketClient {
  private ws: WebSocket | null = null;
  private state: WebSocketState = "disconnected";
  private reconnectAttempts = 0;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null;
  private listeners = new Map<
    WebSocketMessageType | WebSocketState | string,
    Set<(data: unknown) => void>
  >();
  private options: Required<WebSocketOptions>;
  private messageQueue: WebSocketMessage[] = [];
  /** Timestamp (ms) of the most recently received inbound frame. Used for the liveness watchdog. */
  private lastInboundAt = 0;
  /**
   * Hard cap on the offline message queue. Backpressure safety: if the socket stays
   * down for a long time we must not grow the queue without bound (memory leak + a
   * thundering herd on reconnect). When full we drop the OLDEST entry rather than the
   * newest, so fresh events (notifications) are retained and stale ones age out.
   */
  private static readonly MAX_QUEUED_MESSAGES = 50;

  constructor(options: WebSocketOptions = {}) {
    this.options = {
      url: options.url ?? this.getDefaultUrl(),
      autoReconnect: options.autoReconnect ?? true,
      maxReconnectAttempts: options.maxReconnectAttempts ?? 10,
      reconnectDelay: options.reconnectDelay ?? 1000,
      maxReconnectDelay: options.maxReconnectDelay ?? 30000,
      heartbeatInterval: options.heartbeatInterval ?? 30000,
      debug: options.debug ?? false,
    };
  }

  /**
   * Get default WebSocket URL based on current location
   */
  private getDefaultUrl(): string {
    if (typeof window === "undefined") return "/ws";
    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    return `${protocol}//${window.location.host}/ws`;
  }

  /**
   * Connect to WebSocket server
   */
  connect(): void {
    if (this.state === "connected" || this.state === "connecting") {
      this.log("Already connected or connecting");
      return;
    }

    this.setState("connecting");
    this.log(`Connecting to ${this.options.url}`);

    try {
      this.ws = new WebSocket(this.options.url);

      this.ws.onopen = this.handleOpen.bind(this);
      this.ws.onmessage = this.handleMessage.bind(this);
      this.ws.onerror = this.handleError.bind(this);
      this.ws.onclose = this.handleClose.bind(this);
    } catch (error) {
      this.log(`Connection error: ${error}`);
      this.setState("error");
      this.scheduleReconnect();
    }
  }

  /**
   * Manual reconnect (TopBar "retry" button). Resets the backoff counter so a user
   * can always force a fresh attempt even after auto-reconnect hit its max, then
   * starts connecting. No-op if already connected/connecting.
   */
  forceReconnect(): void {
    if (this.state === "connected" || this.state === "connecting") {
      return;
    }
    this.reconnectAttempts = 0;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.connect();
  }

  /**
   * Disconnect from WebSocket server
   */
  disconnect(): void {
    this.log("Disconnecting");
    this.options.autoReconnect = false; // Prevent auto-reconnect on manual disconnect

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    this.stopHeartbeat();

    if (this.ws) {
      this.setState("disconnecting");
      this.ws.close(1000, "Client disconnecting");
      this.ws = null;
    }

    this.setState("disconnected");
  }

  /**
   * Send a message to the server
   */
  send(message: WebSocketMessage): void {
    if (this.state === "connected" && this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
      this.log(`Sent message: ${message.type}`);
    } else {
      this.log("Not connected, queuing message");
      this.messageQueue.push(message);
      // Enforce the cap: drop the oldest entry once we exceed it so the most recent
      // events survive and the queue cannot grow unboundedly while offline.
      if (this.messageQueue.length > WebSocketClient.MAX_QUEUED_MESSAGES) {
        const dropped = this.messageQueue.shift();
        if (dropped) {
          this.log(
            `Queue full (${WebSocketClient.MAX_QUEUED_MESSAGES}), dropped oldest message: ${dropped.type}`,
          );
        }
      }
    }
  }

  /**
   * Send a simple message to a destination
   */
  sendTo(destination: string, payload: unknown): void {
    const message: WebSocketMessage = {
      type: "data",
      topic: destination,
      sender: "client",
      recipient: null,
      payload,
      metadata: {},
      timestamp: new Date().toISOString(),
      messageId: this.generateId(),
    };
    this.send(message);
  }

  /**
   * Register an event listener
   */
  on(
    event: WebSocketMessageType | WebSocketState | string,
    listener: (data: unknown) => void,
  ): () => void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event)!.add(listener);

    // Return unsubscribe function
    return () => {
      this.listeners.get(event)?.delete(listener);
    };
  }

  /**
   * Remove an event listener
   */
  off(
    event: WebSocketMessageType | WebSocketState | string,
    listener: (data: unknown) => void,
  ): void {
    this.listeners.get(event)?.delete(listener);
  }

  /**
   * Get current connection state
   */
  getState(): WebSocketState {
    return this.state;
  }

  /**
   * Check if connected
   */
  isConnected(): boolean {
    return this.state === "connected";
  }

  // ===== Private handlers =====

  private handleOpen(): void {
    this.log("Connected");
    this.setState("connected");
    this.reconnectAttempts = 0;
    this.lastInboundAt = Date.now();
    this.startHeartbeat();
    this.flushMessageQueue();
    this.emit("connected", this.state);
  }

  private handleMessage(event: MessageEvent): void {
    try {
      const message: WebSocketMessage = JSON.parse(event.data);
      // Any inbound frame (including pong / server events) proves the link is alive.
      this.lastInboundAt = Date.now();
      this.log(`Received message: ${message.type}`);
      this.emit(message.type, message);
      this.emit("message", message);
    } catch (error) {
      this.log(`Failed to parse message: ${error}`);
    }
  }

  private handleError(event: Event): void {
    this.log(`WebSocket error: ${event}`);
    this.setState("error");
    this.emit("error", event);
  }

  private handleClose(event: CloseEvent): void {
    this.log(`Connection closed: ${event.code} ${event.reason}`);
    this.stopHeartbeat();
    this.setState("disconnected");
    this.emit("disconnected", this.state);

    if (this.options.autoReconnect) {
      this.scheduleReconnect();
    }
  }

  // ===== Reconnection =====

  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.options.maxReconnectAttempts) {
      this.log("Max reconnection attempts reached");
      this.setState("error");
      return;
    }

    const delay = Math.min(
      this.options.reconnectDelay * Math.pow(2, this.reconnectAttempts),
      this.options.maxReconnectDelay,
    );

    this.reconnectAttempts++;
    this.log(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`);
    this.setState("reconnecting");

    this.reconnectTimer = setTimeout(() => {
      this.connect();
    }, delay);
  }

  // ===== Heartbeat =====

  private startHeartbeat(): void {
    if (this.heartbeatTimer) return;

    this.heartbeatTimer = setInterval(() => {
      if (this.state === "connected" && this.ws?.readyState === WebSocket.OPEN) {
        // Liveness watchdog: if we have seen no inbound frame (pong or any server
        // push) for two heartbeat cycles, the link is silently dead (common with
        // half-open TCP behind a proxy). Tear it down so onclose kicks off the
        // reconnect path instead of hanging in a fake "connected" state.
        const silentFor = Date.now() - this.lastInboundAt;
        if (silentFor > this.options.heartbeatInterval * 2) {
          this.log(`No inbound frame for ${silentFor}ms — closing to force reconnect`);
          this.ws.close(4000, "liveness-timeout");
          return;
        }
        const ping: WebSocketMessage = {
          type: "ping",
          topic: null,
          sender: "client",
          recipient: null,
          payload: null,
          metadata: {},
          timestamp: new Date().toISOString(),
          messageId: this.generateId(),
        };
        this.ws.send(JSON.stringify(ping));
      }
    }, this.options.heartbeatInterval);
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  // ===== Message queue =====

  private flushMessageQueue(): void {
    while (this.messageQueue.length > 0) {
      const message = this.messageQueue.shift();
      if (message) {
        this.send(message);
      }
    }
  }

  // ===== Utilities =====

  private setState(state: WebSocketState): void {
    this.state = state;
    this.emit(state, state);
  }

  private emit(event: string, data: unknown): void {
    this.listeners.get(event)?.forEach((listener) => {
      try {
        listener(data);
      } catch (error) {
        logError(`WebSocket listener error for ${event}:`, error);
      }
    });
  }

  private generateId(): string {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  private log(message: string): void {
    if (this.options.debug) {
      debug(`[WebSocket] ${message}`);
    }
  }
}

// Singleton instance
let globalClient: WebSocketClient | null = null;

/**
 * Get or create the global WebSocket client instance
 */
export function getWebSocketClient(options?: WebSocketOptions): WebSocketClient {
  if (!globalClient) {
    globalClient = new WebSocketClient(options);
  }
  return globalClient;
}

/**
 * Canonical factory for the shared WebSocket singleton.
 *
 * <p>Both app boot ({@code main.tsx}) and the auth store (post-login {@code connect})
 * must configure the singleton identically. Whichever path runs first creates it;
 * subsequent {@link getWebSocketClient} calls ignore their options, so this helper
 * centralizes URL / autoReconnect / debug to avoid the singleton being born with
 * different options depending on who happened to call first.
 */
export function getDefaultWebSocketClient(): WebSocketClient {
  return getWebSocketClient({
    url: import.meta.env.VITE_WS_URL || "/ws",
    autoReconnect: true,
    debug: import.meta.env.DEV,
  });
}
