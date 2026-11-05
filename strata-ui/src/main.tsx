// SPDX-License-Identifier: Apache-2.0
import React from "react";
import ReactDOM from "react-dom/client";
import App from "@/App";
import { useI18nStore } from "@/shared/i18n";
import { errorReporter } from "@/shared/monitoring";
import { initWebVitals } from "@/shared/performance";
import { initPWA, showUpdateDialog } from "@/shared/pwa";
import { getDefaultWebSocketClient } from "@/shared/websocket";
import { ACCESS_TOKEN_KEY } from "@/shared/api/client";
import "./index.css";

/* Initialize error reporting and performance monitoring before React renders. */
errorReporter.init({
  appVersion: import.meta.env.VITE_APP_VERSION || "0.2.0",
  environment: import.meta.env.MODE,
});

initWebVitals({
  reportEndpoint: import.meta.env.PROD ? "/api/metrics/frontend" : undefined,
  sampleRate: import.meta.env.PROD ? 0.1 : 1.0,
  logToConsole: import.meta.env.DEV,
  reportToSentry: true,
});

/* Initialize PWA (Service Worker + offline support). */
initPWA({
  enabledInDev: false,
  onUpdateAvailable: (registration) => {
    showUpdateDialog(registration);
  },
});

/* Initialize WebSocket connection if user is already authenticated.
   The connection will be established after login via the auth store. */
const initWebSocket = () => {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
  if (token) {
    getDefaultWebSocketClient().connect();
  }
};

// Defer WebSocket init to avoid blocking initial render
if (document.readyState === "complete") {
  setTimeout(initWebSocket, 1000);
} else {
  window.addEventListener("load", () => setTimeout(initWebSocket, 1000));
}

/* Keep <html lang> in sync with the active language (a11y/SEO). */
document.documentElement.lang = useI18nStore.getState().lang;
useI18nStore.subscribe((s) => {
  document.documentElement.lang = s.lang;
});

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
