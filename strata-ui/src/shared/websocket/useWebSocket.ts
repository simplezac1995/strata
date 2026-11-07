// SPDX-License-Identifier: Apache-2.0
import { useEffect, useRef, useState, useCallback } from "react";
import { WebSocketClient, getWebSocketClient, getDefaultWebSocketClient } from "./WebSocketClient";
import { queryClient } from "@/shared/query/queryClient";
import type {
  WebSocketMessage,
  WebSocketMessageType,
  WebSocketOptions,
  WebSocketState,
} from "./types";

/**
 * Query-key prefixes refetched once the socket comes back after a drop.
 *
 * A reconnect means we may have missed server pushes while offline, so cached
 * lists keyed under these prefixes are considered stale and refetched. Host
 * pages can override per-hook via `invalidateOnReconnect`.
 */
export const DEFAULT_INVALIDATE_ON_RECONNECT: unknown[][] = [
  ["api", "notification"],
  ["api", "cs"],
];

/**
 * React Hook for WebSocket communication.
 *
 * Features:
 * - Auto-connect on mount
 * - Auto-disconnect on unmount
 * - Message type-based subscriptions
 * - Connection state tracking
 * - Send messages
 *
 * Usage:
 * ```tsx
 * function NotificationPanel() {
 *   const { state, lastMessage, send } = useWebSocket({
 *     url: '/ws',
 *     onMessage: (msg) => console.log('Received:', msg),
 *   });
 *
 *   const [notifications, setNotifications] = useState<Notification[]>([]);
 *
 *   useEffect(() => {
 *     if (lastMessage?.type === 'notification') {
 *       setNotifications(prev => [lastMessage.payload, ...prev]);
 *     }
 *   }, [lastMessage]);
 *
 *   return (
 *     <div>
 *       <p>Connection: {state}</p>
 *       {notifications.map(n => <Notification key={n.id} {...n} />)}
 *     </div>
 *   );
 * }
 * ```
 */
export function useWebSocket(
  options: WebSocketOptions & {
    onMessage?: (message: WebSocketMessage) => void;
    onStateChange?: (state: WebSocketState) => void;
    onError?: (error: Event) => void;
    /**
     * Query-key prefixes to invalidate when the socket reconnects after being
     * disconnected. Defaults to {@link DEFAULT_INVALIDATE_ON_RECONNECT}. Pass an
     * empty array to disable refetch-on-reconnect for this consumer.
     */
    invalidateOnReconnect?: unknown[][];
  } = {},
) {
  const {
    onMessage,
    onStateChange,
    onError,
    invalidateOnReconnect = DEFAULT_INVALIDATE_ON_RECONNECT,
    ...wsOptions
  } = options;
  const clientRef = useRef<WebSocketClient | null>(null);
  const [state, setState] = useState<WebSocketState>("disconnected");
  const [lastMessage, setLastMessage] = useState<WebSocketMessage | null>(null);
  const [connected, setConnected] = useState(false);
  // The very first "connected" event is the initial handshake; only subsequent
  // transitions are a reconnect where we likely missed pushes while offline.
  const hasConnectedOnce = useRef(false);

  // Initialize client
  useEffect(() => {
    clientRef.current = getWebSocketClient(wsOptions);

    const unsubscribeState = clientRef.current.on("connected", () => {
      setState("connected");
      setConnected(true);
      if (hasConnectedOnce.current && invalidateOnReconnect.length > 0) {
        // Refetch lists that may have changed server-side while the socket was down.
        invalidateOnReconnect.forEach((prefix) => {
          void queryClient.invalidateQueries({ queryKey: prefix });
        });
      }
      hasConnectedOnce.current = true;
      onStateChange?.("connected");
    });

    const unsubscribeDisconnected = clientRef.current.on("disconnected", () => {
      setState("disconnected");
      setConnected(false);
      onStateChange?.("disconnected");
    });

    const unsubscribeReconnecting = clientRef.current.on("reconnecting", () => {
      setState("reconnecting");
      onStateChange?.("reconnecting");
    });

    const unsubscribeError = clientRef.current.on("error", (data) => {
      setState("error");
      setConnected(false);
      onError?.(data as Event);
      onStateChange?.("error");
    });

    const unsubscribeMessage = clientRef.current.on("message", (data) => {
      const message = data as WebSocketMessage;
      setLastMessage(message);
      onMessage?.(message);
    });

    // Auto-connect
    clientRef.current.connect();

    return () => {
      unsubscribeState();
      unsubscribeDisconnected();
      unsubscribeReconnecting();
      unsubscribeError();
      unsubscribeMessage();
      // Don't disconnect on unmount if using global client
      // clientRef.current?.disconnect();
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  /**
   * Send a message to the server
   */
  const send = useCallback((message: WebSocketMessage) => {
    clientRef.current?.send(message);
  }, []);

  /**
   * Send data to a destination
   */
  const sendTo = useCallback((destination: string, payload: unknown) => {
    clientRef.current?.sendTo(destination, payload);
  }, []);

  /**
   * Subscribe to a specific message type
   */
  const subscribe = useCallback(
    (type: WebSocketMessageType | string, listener: (message: WebSocketMessage) => void) => {
      if (!clientRef.current) return () => {};
      return clientRef.current.on(type, (data) => listener(data as WebSocketMessage));
    },
    [],
  );

  /**
   * Manually connect
   */
  const connect = useCallback(() => {
    clientRef.current?.connect();
  }, []);

  /**
   * Manually disconnect
   */
  const disconnect = useCallback(() => {
    clientRef.current?.disconnect();
  }, []);

  return {
    state,
    connected,
    lastMessage,
    send,
    sendTo,
    subscribe,
    connect,
    disconnect,
    client: clientRef.current,
  };
}

/**
 * Lightweight connection-state-only hook for shell chrome (e.g. TopBar badge).
 *
 * Unlike {@link useWebSocket} this does NOT subscribe to per-frame messages, so it
 * does not re-render on every notification. It tracks the shared default singleton's
 * connection lifecycle and exposes a manual `reconnect()` for the "offline" retry
 * button.
 */
export function useWebSocketStatus() {
  const clientRef = useRef<WebSocketClient | null>(null);
  const [state, setState] = useState<WebSocketState>("disconnected");

  useEffect(() => {
    const client = getDefaultWebSocketClient();
    clientRef.current = client;
    setState(client.getState());

    const unsubs = (
      ["connected", "connecting", "reconnecting", "error", "disconnected"] as const
    ).map((s) =>
      client.on(s, (data) => {
        setState(data as WebSocketState);
      }),
    );
    return () => unsubs.forEach((unsub) => unsub());
  }, []);

  const reconnect = useCallback(() => {
    const client = clientRef.current ?? getDefaultWebSocketClient();
    // A manual retry must bypass the max-attempts backoff ceiling: forceReconnect
    // resets the counter so the user can always force a fresh attempt.
    client.forceReconnect();
  }, []);

  return { state, reconnect };
}

/**
 * Hook for subscribing to notification messages
 */
export function useNotifications() {
  const [notifications, setNotifications] = useState<
    Array<{
      id: string;
      title: string;
      message: string;
      type: "info" | "success" | "warning" | "error";
      read: boolean;
      createdAt: string;
    }>
  >([]);

  const { subscribe } = useWebSocket();

  useEffect(() => {
    const unsubscribe = subscribe("notification", (msg) => {
      const payload = msg.payload as {
        id: string;
        title: string;
        message: string;
        type: "info" | "success" | "warning" | "error";
      };
      setNotifications((prev) => [
        {
          ...payload,
          read: false,
          createdAt: msg.timestamp,
        },
        ...prev,
      ]);
    });

    return unsubscribe;
  }, [subscribe]);

  const markAsRead = useCallback((id: string) => {
    setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)));
  }, []);

  const markAllAsRead = useCallback(() => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  }, []);

  const clearAll = useCallback(() => {
    setNotifications([]);
  }, []);

  const unreadCount = notifications.filter((n) => !n.read).length;

  return {
    notifications,
    unreadCount,
    markAsRead,
    markAllAsRead,
    clearAll,
  };
}
