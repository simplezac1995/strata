// SPDX-License-Identifier: Apache-2.0
import type { ReactNode } from "react";
import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { queryClient } from "@/shared/query/queryClient";

/**
 * Application-level React Query provider.
 *
 * Wraps the app with QueryClientProvider and mounts DevTools in development.
 * Place this inside AppErrorBoundary so that query errors are still caught
 * by the boundary, and inside BrowserRouter so query hooks can use navigation.
 */
export function QueryProvider({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      {children}
      {import.meta.env.DEV && (
        <ReactQueryDevtools initialIsOpen={false} buttonPosition="bottom-right" position="right" />
      )}
    </QueryClientProvider>
  );
}
