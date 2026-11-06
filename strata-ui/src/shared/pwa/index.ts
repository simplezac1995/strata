// SPDX-License-Identifier: Apache-2.0
import { tr, useI18nStore } from "@/shared/i18n";
import { debug, info, warn, error as logError } from "@/shared/logger";

/**
 * PWA (Progressive Web App) service.
 *
 * <p>Handles Service Worker registration, update detection, and offline status.
 *
 * <p>Usage:
 * <pre>{@code
 * import { initPWA, pwaService } from '@/shared/pwa';
 *
 * // Initialize at app startup
 * initPWA({
 *   onUpdateAvailable: (registration) => {
 *     // Show update notification
 *     if (confirm('New version available. Reload?')) {
 *       registration.waiting.postMessage({ type: 'SKIP_WAITING' });
 *       window.location.reload();
 *     }
 *   },
 * });
 *
 * // Check offline status
 * pwaService.isOnline(); // true/false
 * }</pre>
 */

export interface PWAConfig {
  /**
   * Service Worker URL. Default: '/sw.js'
   */
  swUrl?: string;

  /**
   * Whether to enable PWA in development. Default: false
   */
  enabledInDev?: boolean;

  /**
   * Callback when a new Service Worker is installed and waiting.
   */
  onUpdateAvailable?: (registration: ServiceWorkerRegistration) => void;

  /**
   * Callback when the Service Worker is first installed.
   */
  onInstalled?: (registration: ServiceWorkerRegistration) => void;

  /**
   * Callback when network status changes.
   */
  onNetworkChange?: (isOnline: boolean) => void;
}

export interface PWAService {
  /**
   * Whether the app is currently online.
   */
  isOnline: () => boolean;

  /**
   * Whether the app is running in standalone (PWA) mode.
   */
  isStandalone: () => boolean;

  /**
   * Get the current Service Worker registration.
   */
  getRegistration: () => ServiceWorkerRegistration | null;

  /**
   * Check for Service Worker updates.
   */
  checkForUpdates: () => Promise<void>;

  /**
   * Subscribe to push notifications (if supported).
   */
  subscribeToPush: (publicKey: string) => Promise<PushSubscription | null>;

  /**
   * Unsubscribe from push notifications.
   */
  unsubscribeFromPush: () => Promise<boolean>;
}

let registration: ServiceWorkerRegistration | null = null;
let config: Required<PWAConfig> = {
  swUrl: "/sw.js",
  enabledInDev: false,
  onUpdateAvailable: () => {},
  onInstalled: () => {},
  onNetworkChange: () => {},
};

/**
 * Show a lightweight DOM-based modal asking the user to reload for a new version.
 *
 * <p>Renders outside the React tree (called from `main.tsx` before React mounts
 * or at any later point) so it cannot use MUI/ConfirmDialog directly. Uses the
 * same short-key i18n dictionary as the rest of the app.
 *
 * @param registration Service worker registration with a waiting worker.
 */
export function showUpdateDialog(registration: ServiceWorkerRegistration): void {
  const lang = useI18nStore.getState().lang;
  const title = tr(lang, "pwa.updateAvailable.title");
  const message = tr(lang, "pwa.updateAvailable.message");
  const reloadLabel = tr(lang, "common.retry");
  const laterLabel = tr(lang, "common.cancel");

  const backdrop = document.createElement("div");
  backdrop.setAttribute(
    "style",
    "position:fixed;inset:0;z-index:9999;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;",
  );

  const card = document.createElement("div");
  card.setAttribute(
    "style",
    "background:#fff;border-radius:12px;padding:24px;max-width:400px;width:90%;box-shadow:0 8px 32px rgba(0,0,0,0.2);font-family:system-ui,sans-serif;",
  );

  const titleEl = document.createElement("h2");
  titleEl.setAttribute("style", "margin:0 0 8px;font-size:18px;font-weight:600;color:#1a1a1a;");
  titleEl.textContent = title;

  const msgEl = document.createElement("p");
  msgEl.setAttribute("style", "margin:0 0 20px;font-size:14px;color:#666;line-height:1.5;");
  msgEl.textContent = message;

  const btnRow = document.createElement("div");
  btnRow.setAttribute("style", "display:flex;gap:8px;justify-content:flex-end;");

  const laterBtn = document.createElement("button");
  laterBtn.textContent = laterLabel;
  laterBtn.setAttribute(
    "style",
    "padding:8px 16px;border:1px solid #ddd;border-radius:8px;background:#fff;cursor:pointer;font-size:14px;",
  );
  laterBtn.onclick = () => backdrop.remove();

  const reloadBtn = document.createElement("button");
  reloadBtn.textContent = reloadLabel;
  reloadBtn.setAttribute(
    "style",
    "padding:8px 16px;border:none;border-radius:8px;background:#1976d2;color:#fff;cursor:pointer;font-size:14px;",
  );
  reloadBtn.onclick = () => {
    registration.waiting?.postMessage({ type: "SKIP_WAITING" });
    window.location.reload();
  };

  btnRow.appendChild(laterBtn);
  btnRow.appendChild(reloadBtn);
  card.appendChild(titleEl);
  card.appendChild(msgEl);
  card.appendChild(btnRow);
  backdrop.appendChild(card);
  document.body.appendChild(backdrop);
}

/**
 * Initialize PWA support.
 *
 * @param userConfig PWA configuration
 */
export function initPWA(userConfig?: PWAConfig): void {
  config = { ...config, ...userConfig };

  // Check if Service Worker is supported
  if (!("serviceWorker" in navigator)) {
    warn("[PWA] Service Worker is not supported in this browser");
    return;
  }

  // Disable in development unless explicitly enabled
  if (import.meta.env.DEV && !config.enabledInDev) {
    debug("[PWA] Service Worker disabled in development mode");
    return;
  }

  // Register Service Worker
  window.addEventListener("load", () => {
    navigator.serviceWorker
      .register(config.swUrl)
      .then((reg) => {
        registration = reg;
        info("[PWA] Service Worker registered successfully:", reg.scope);

        config.onInstalled(reg);

        // Check for updates
        reg.addEventListener("updatefound", () => {
          const newWorker = reg.installing;
          if (!newWorker) return;

          newWorker.addEventListener("statechange", () => {
            if (newWorker.state === "installed" && navigator.serviceWorker.controller) {
              info("[PWA] New version available");
              config.onUpdateAvailable(reg);
            }
          });
        });
      })
      .catch((error) => {
        logError("[PWA] Service Worker registration failed:", error);
      });
  });

  // Handle controller change (after update)
  let refreshing = false;
  navigator.serviceWorker.addEventListener("controllerchange", () => {
    if (!refreshing) {
      refreshing = true;
      window.location.reload();
    }
  });

  // Network status monitoring
  window.addEventListener("online", () => {
    info("[PWA] Network status: online");
    config.onNetworkChange(true);
  });

  window.addEventListener("offline", () => {
    info("[PWA] Network status: offline");
    config.onNetworkChange(false);
  });
}

/**
 * PWA service instance with utility methods.
 */
export const pwaService: PWAService = {
  isOnline: () => navigator.onLine,

  isStandalone: () => {
    return (
      window.matchMedia("(display-mode: standalone)").matches ||
      (window.navigator as unknown as { standalone?: boolean }).standalone === true
    );
  },

  getRegistration: () => registration,

  checkForUpdates: async () => {
    if (registration) {
      await registration.update();
    }
  },

  subscribeToPush: async (publicKey: string) => {
    if (!registration || !("PushManager" in window)) {
      return null;
    }
    try {
      const key = urlBase64ToUint8Array(publicKey);
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: key as BufferSource,
      });
      info("[PWA] Push subscription successful");
      return subscription;
    } catch (error) {
      logError("[PWA] Push subscription failed:", error);
      return null;
    }
  },

  unsubscribeFromPush: async () => {
    if (!registration) return false;
    try {
      const subscription = await registration.pushManager.getSubscription();
      if (subscription) {
        await subscription.unsubscribe();
        info("[PWA] Push unsubscribed");
        return true;
      }
      return false;
    } catch (error) {
      logError("[PWA] Push unsubscription failed:", error);
      return false;
    }
  },
};

/**
 * Convert a base64 string to Uint8Array for VAPID key.
 */
function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}
