// SPDX-License-Identifier: Apache-2.0
import { useEffect } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/shared/pageKit/Toast";

/**
 * Full list loader backed by React Query.
 *
 * Drop-in replacement for `useCrudList` — same return shape.
 * Use this for non-paginated lists (dropdown options, tree data, reference
 * data) that should be cached across components.
 *
 * @param queryKey  Query key, e.g. `['api', 'system', 'dict-types', 'list']`.
 * @param loader    Fetches the full list: `() => Promise<T[]>`.
 * @param options   Optional `staleTime` (ms) for long-lived reference data.
 *                  Use 5 * 60 * 1000 for dropdowns/dictionaries that rarely change.
 */
export function useListQuery<T>(
  queryKey: unknown[],
  loader: () => Promise<T[]>,
  options: { staleTime?: number } = {},
) {
  const queryClient = useQueryClient();

  const { data, isLoading, isFetching, error, refetch } = useQuery({
    queryKey,
    queryFn: loader,
    staleTime: options.staleTime,
  });

  useEffect(() => {
    if (error) {
      toast.error("toast.loadFailed");
    }
  }, [error]);

  const reload = () => {
    queryClient.invalidateQueries({ queryKey });
  };

  return {
    data: data ?? [],
    loading: isLoading || isFetching,
    reload,
    refetch,
  };
}
