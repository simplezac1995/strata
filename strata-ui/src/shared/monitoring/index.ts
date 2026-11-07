// SPDX-License-Identifier: Apache-2.0
/**
 * Frontend monitoring and error reporting utilities.
 *
 * <p>Provides:
 * - Error reporting service (Sentry + custom endpoint)
 * - Global error handlers (window.onerror, unhandledrejection)
 * - React Error Boundary component
 *
 * <p>Usage:
 * ```tsx
 * import { errorReporter, ErrorBoundary } from '@/shared/monitoring';
 *
 * // Initialize at app startup
 * errorReporter.init({ appVersion: '1.0.0' });
 *
 * // Wrap components
 * <ErrorBoundary>
 *   <MyComponent />
 * </ErrorBoundary>
 *
 * // Manual error reporting
 * errorReporter.reportError(new Error('Something went wrong'));
 * ```
 *
 * <p>Performance / Web Vitals monitoring lives in {@link '@/shared/performance'}
 * (single implementation: `initWebVitals`, `useWebVitals`, custom metrics).
 */

export { errorReporter } from "./errorReporter";
export type { ErrorReport, ErrorReporterConfig } from "./errorReporter";
export { ErrorBoundary } from "@/components/ui/ErrorBoundary";
