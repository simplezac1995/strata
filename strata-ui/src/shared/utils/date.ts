// SPDX-License-Identifier: Apache-2.0
/**
 * Shared date/time formatting helpers.
 *
 * `formatDateTime` renders an ISO date string as a localized date+time, or falls back to
 * an em dash when the value is null/undefined/empty.
 */
export function formatDateTime(value: string | null | undefined): string {
  return value ? new Date(value).toLocaleString() : "—";
}
