// SPDX-License-Identifier: Apache-2.0
import { ApiError } from "@/shared/api/client";

/**
 * Error message utilities for API failures.
 *
 * These helpers are kept separate from the React Query hooks because some
 * pages (AI chat, image generation, BPMN designer, etc.) use direct API
 * calls with manual error handling instead of useMutationWithToast.
 */

/** Extracts a user-facing message from an API failure. */
export function errorMessage(err: unknown, fallback: string): string {
  return err instanceof ApiError ? err.message : fallback;
}

/** Message key for toast errors — ApiError detail is mapped via tError at render. */
export function failureMessage(err: unknown, fallback = "toast.saveFailed"): string {
  return errorMessage(err, fallback);
}
