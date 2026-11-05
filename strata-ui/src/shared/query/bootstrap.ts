// SPDX-License-Identifier: Apache-2.0
import { queryClient } from "./queryClient";
import { deptApi, dictTypeApi, menuApi, paramApi, postApi, roleApi } from "@/shared/api/system";

/**
 * Low-frequency reference data is considered fresh for 5 minutes after login.
 * Dropdowns / trees that consume it (dictionaries, params, menus, roles,
 * posts, depts) then render from cache instead of firing a request on first
 * mount.
 */
const REFERENCE_STALE_TIME = 5 * 60 * 1000;

/**
 * Prefetch low-frequency reference data right after login so the first
 * navigation into a list/form page does not block on cold dropdowns.
 *
 * Uses `prefetchQuery` (fire-and-forget, deduped by the QueryClient) and is
 * safe to call multiple times — already-fresh entries are skipped. Pages still
 * own their own `useQuery` calls; this only warms the cache.
 */
export function bootstrapReferenceData(): void {
  const prefetch = <T>(queryKey: unknown[], queryFn: () => Promise<T>) => {
    void queryClient.prefetchQuery({
      queryKey,
      queryFn,
      staleTime: REFERENCE_STALE_TIME,
    });
  };

  prefetch(["api", "system", "dict-types", "list"], () => dictTypeApi.list());
  prefetch(["api", "system", "params", "list"], () => paramApi.list());
  prefetch(["api", "system", "menus", "list"], () => menuApi.list());
  prefetch(["api", "system", "roles", "list"], () => roleApi.list());
  prefetch(["api", "system", "posts", "list"], () => postApi.list());
  prefetch(["api", "system", "depts", "list"], () => deptApi.list());
}
