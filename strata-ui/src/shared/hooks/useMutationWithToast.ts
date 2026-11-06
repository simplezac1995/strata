// SPDX-License-Identifier: Apache-2.0
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/shared/pageKit/Toast";
import { errorMessage } from "@/shared/hooks/useCrud";

/**
 * Mutation hook with automatic success/failure toasts.
 *
 * Replacement for the standalone `runMutation` helper. Use this for
 * create/update/delete operations that should show a toast on completion.
 *
 * Supports optimistic updates via `onMutate` (update cache before request).
 * This hook does NOT provide automatic rollback on error — if you mutate the
 * cache in `onMutate`, you are responsible for snapshotting and restoring it
 * yourself (e.g. capture `queryClient.getQueryCache().findAll(...)` in
 * `onMutate` and roll back inside `onError`). Use `invalidateKeys` to
 * auto-invalidate related queries after success.
 *
 * @example
 * ```tsx
 * const { mutateAsync, isPending } = useMutationWithToast(
 *   (payload: UserCreatePayload) => userApi.create(payload),
 *   { success: "toast.saved", failure: "toast.saveFailed" },
 *   { invalidateKeys: [['api', 'system', 'users']] },
 * );
 * ```
 *
 * @param mutationFn  The async operation to perform.
 * @param messages    Toast i18n keys for success and failure.
 * @param options     Optional callbacks and invalidation config.
 */
export function useMutationWithToast<TData = unknown, TVariables = void>(
  mutationFn: (variables: TVariables) => Promise<TData>,
  messages: { success: string; failure: string },
  options: {
    onSuccess?: (data: TData, variables: TVariables) => void;
    onError?: (error: Error, variables: TVariables) => void;
    onMutate?: (variables: TVariables) => unknown | Promise<unknown>;
    onSettled?: (data: TData | undefined, error: Error | null, variables: TVariables) => void;
    /** Query keys to invalidate after successful mutation. */
    invalidateKeys?: unknown[][];
  } = {},
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn,
    onMutate: options.onMutate,
    onSuccess: (data, variables) => {
      toast.success(messages.success);
      if (options.invalidateKeys) {
        for (const key of options.invalidateKeys) {
          void queryClient.invalidateQueries({ queryKey: key });
        }
      }
      options.onSuccess?.(data, variables);
    },
    onError: (error, variables) => {
      toast.error(errorMessage(error, messages.failure));
      options.onError?.(error as Error, variables);
    },
    onSettled: (data, error, variables) => {
      options.onSettled?.(data, error as Error | null, variables);
    },
  });
}
