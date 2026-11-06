// SPDX-License-Identifier: Apache-2.0
/**
 * Lightweight console logger that suppresses verbose output in production.
 *
 * - `debug()` / `info()` only print when `import.meta.env.DEV` is true.
 * - `warn()` / `error()` always print but never include sensitive payloads.
 *
 * Use this instead of raw `console.log` / `console.info` in shared services
 * (PWA, WebSocket, etc.) to avoid leaking internal state in production builds.
 */

const isDev = import.meta.env.DEV;

/**
 * Debug-level log — only visible in development.
 */
export function debug(...args: unknown[]): void {
  if (isDev) {
    // eslint-disable-next-line no-console -- this is the single logger abstraction
    console.log(...args);
  }
}

/**
 * Info-level log — only visible in development.
 */
export function info(...args: unknown[]): void {
  if (isDev) {
    // eslint-disable-next-line no-console -- single logger abstraction
    console.info(...args);
  }
}

/**
 * Warn-level log — always visible. Use for recoverable issues.
 */
export function warn(...args: unknown[]): void {
  // eslint-disable-next-line no-console -- single logger abstraction
  console.warn(...args);
}

/**
 * Error-level log — always visible. Never include secrets or PII in args.
 */
export function error(...args: unknown[]): void {
  // eslint-disable-next-line no-console -- single logger abstraction
  console.error(...args);
}
