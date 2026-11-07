// SPDX-License-Identifier: Apache-2.0
import { useEffect, useRef } from "react";
import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/shared/pageKit/Toast";
import type { PageResult } from "@/shared/types";

export type SortDir = "asc" | "desc";

export type SortState = {
  field: string;
  dir: SortDir;
};

/**
 * Paginated list loader backed by React Query.
 *
 * Drop-in replacement for `usePagedCrud` — same return shape, same
 * 1-based `page` convention, same `fetch(pageIndex, pageSize, sort)`
 * signature where `pageIndex` is 0-based for the backend.
 *
 * Differences from `usePagedCrud`:
 * - Results are cached by `queryKey` + pagination params; navigating away
 *   and back shows cached data instantly while refetching in background.
 * - `reload()` invalidates the whole query key family (all pages), not just
 *   the current page — useful after a create/update/delete mutation.
 * - Failed requests show a toast (same as usePagedCrud) but React Query
 *   also retries transient network errors once.
 * - **Prefetching**: after the current page loads, the next page is
 *   prefetched automatically so pagination feels instant.
 *
 * @param queryKey  Base query key, e.g. `['api', 'system', 'users', { keyword }]`.
 *                  Pagination params are appended automatically.
 * @param fetch     Fetches one page: `(pageIndex, pageSize, sort?) => Promise<PageResult<T>>`.
 * @param options   Optional `initialPageSize` (default 10) and `prefetch` (default true).
 */
export function usePagedQuery<T>(
  queryKey: unknown[],
  fetch: (pageIndex: number, pageSize: number, sort?: SortState) => Promise<PageResult<T>>,
  options: { initialPageSize?: number; prefetch?: boolean } = {},
) {
  const { initialPageSize = 10, prefetch = true } = options;
  const queryClient = useQueryClient();

  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [sort, setSort] = useState<SortState | undefined>(undefined);

  // Keep the latest fetch in a ref so the prefetch effect does not re-run
  // on every render when callers pass inline arrow functions.
  const fetchRef = useRef(fetch);
  fetchRef.current = fetch;

  const fullKey = [...queryKey, page, pageSize, sort];

  // The "non-pagination" part of the query key (everything callers pass in).
  // Used to decide whether placeholderData should carry over: we only keep
  // previous data when the *filter* is the same and only the page changed.
  // When filters change (different queryKey), we do NOT show stale data.
  const baseKeySerialRef = useRef<string>(JSON.stringify(queryKey));
  const currentBaseKeySerial = JSON.stringify(queryKey);
  const baseKeyUnchanged = baseKeySerialRef.current === currentBaseKeySerial;
  baseKeySerialRef.current = currentBaseKeySerial;

  const { data, isLoading, isFetching, error, refetch } = useQuery({
    queryKey: fullKey,
    queryFn: () => fetchRef.current(page - 1, pageSize, sort),
    // Only keep previous page data while paginating; clear it when filters change.
    placeholderData: baseKeyUnchanged ? (prev) => prev : undefined,
  });

  // Prefetch next page for instant pagination.
  // Depends only on pagination state — fetch is read via ref so inline
  // arrow functions from callers do not cause unnecessary re-runs.
  useEffect(() => {
    if (!prefetch || !data) return;
    const totalPages = Math.ceil(data.totalElements / pageSize);
    if (page < totalPages) {
      const nextKey = [...queryKey, page + 1, pageSize, sort];
      void queryClient.prefetchQuery({
        queryKey: nextKey,
        queryFn: () => fetchRef.current(page, pageSize, sort),
      });
    }
  }, [data, page, pageSize, sort, prefetch, queryKey, queryClient]);

  // Toast on error (matches usePagedCrud behavior). React Query v5 removed
  // onError, so we use an effect on the error state.
  useEffect(() => {
    if (error) {
      toast.error("toast.loadFailed");
    }
  }, [error]);

  const reload = () => {
    // Invalidate the base key family so all pages (not just current) refetch.
    // This is the desired behavior after a mutation changes the dataset.
    queryClient.invalidateQueries({ queryKey });
  };

  // Shared QueryForm onSearch: jump back to page 1 and refetch the current
  // filter set. Pages still own their onReset (which fields to clear).
  const onSearch = () => {
    if (page > 1) {
      // Jumping to page 1 changes the query key (new page + current filters), so
      // React Query refetches fresh page-1 results — no separate invalidation.
      setPage(1);
    } else {
      // Already on page 1: setPage(1) is a no-op, so explicitly refetch filters.
      reload();
    }
  };

  return {
    rows: data?.content ?? [],
    total: data?.totalElements ?? 0,
    page,
    setPage,
    pageSize,
    setPageSize,
    sort,
    setSort,
    loading: isLoading || isFetching,
    error,
    reload,
    onSearch,
    refetch, // exposed for callers that want to refetch only the current page
  };
}
