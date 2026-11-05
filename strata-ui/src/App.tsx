// SPDX-License-Identifier: Apache-2.0
import { useEffect } from "react";
import { BrowserRouter, Routes } from "react-router-dom";
import { AppProvider } from "@/context/AppContext";
import { AppErrorBoundary } from "@/components/shell/AppErrorBoundary";
import { QueryProvider } from "@/shared/query/QueryProvider";
import { useAuthStore } from "@/shared/auth/store";
import { ToastViewport } from "@/shared/pageKit/Toast";
import { appRoutes } from "@/routes/appRoutes";
import { useWebVitals } from "@/shared/performance";

export default function App() {
  useEffect(() => {
    useAuthStore.getState().hydrate();
    // After restore, pull a fresh server-side authority snapshot once so a
    // permission change made in another tab/session is reflected without a reload.
    // Fire-and-forget: refreshAuthorities keeps the stale snapshot on failure.
    void useAuthStore.getState().refreshAuthorities();
  }, []);

  // Web Vitals 性能监控（生产环境启用，开发环境仅日志输出）
  useWebVitals({
    enabled: import.meta.env.PROD,
    sampleRate: 0.1,
  });

  return (
    <AppProvider>
      <BrowserRouter>
        <AppErrorBoundary>
          <QueryProvider>
            <Routes>{appRoutes()}</Routes>
            <ToastViewport />
          </QueryProvider>
        </AppErrorBoundary>
      </BrowserRouter>
    </AppProvider>
  );
}
