// SPDX-License-Identifier: Apache-2.0
import { QueryClient } from "@tanstack/react-query";

/**
 * Application-wide QueryClient configuration.
 *
 * Design notes:
 * - `staleTime: 30s`: data is considered fresh for 30 seconds, avoiding
 *   refetch on every mount/window focus for rapidly-navigated pages.
 * - `retry: 1`: network errors (5xx, timeout) retry once; 4xx errors are
 *   not retried (they are deterministic client errors). The axios interceptor
 *   already handles 401 refresh, so QueryClient does not need to retry 401.
 * - `refetchOnWindowFocus: false`: avoid surprise refetches when the user
 *   switches tabs; pages that need fresh data can opt in per-query.
 * - `gcTime: 5min`: unused cached data is garbage-collected after 5 minutes.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 30,
      retry: (failureCount, error) => {
        // Do not retry 4xx errors (client errors are deterministic)
        if (error instanceof Error && "status" in error) {
          const status = (error as { status?: number }).status;
          if (status && status >= 400 && status < 500) return false;
        }
        return failureCount < 1;
      },
      refetchOnWindowFocus: false,
      gcTime: 1000 * 60 * 5,
    },
    mutations: {
      retry: false,
    },
  },
});
