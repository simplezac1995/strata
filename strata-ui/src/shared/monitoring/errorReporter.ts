// SPDX-License-Identifier: Apache-2.0
/**
 * Error reporting service for frontend error monitoring.
 *
 * <p>Supports Sentry integration (when configured) and custom error reporting endpoints.
 * Captures JavaScript errors, unhandled promise rejections, and React component errors.
 *
 * <p>Configuration via environment variables:
 * - VITE_SENTRY_DSN: Sentry DSN (optional, enables Sentry when set)
 * - VITE_SENTRY_ENVIRONMENT: Sentry environment (default: production)
 * - VITE_ERROR_REPORT_URL: Custom error reporting endpoint URL (optional)
 */

export interface ErrorReport {
  message: string;
  stack?: string;
  timestamp: string;
  url: string;
  userAgent: string;
  environment: string;
  appVersion: string;
  userId?: string;
  extra?: Record<string, unknown>;
}

export interface ErrorReporterConfig {
  sentryDsn?: string;
  environment?: string;
  appVersion?: string;
  reportUrl?: string;
  userId?: string;
  enabled?: boolean;
}

class ErrorReporter {
  private config: ErrorReporterConfig = {
    environment: import.meta.env.MODE || "production",
    appVersion: import.meta.env.VITE_APP_VERSION || "0.0.0",
    enabled: true,
  };

  private initialized = false;
  private sentryInitialized = false;

  /**
   * Initialize the error reporter with configuration.
   * Should be called once at application startup.
   */
  init(config?: Partial<ErrorReporterConfig>): void {
    if (this.initialized) {
      console.warn("[ErrorReporter] Already initialized");
      return;
    }

    this.config = { ...this.config, ...config };

    // Read from environment variables if not provided
    if (!this.config.sentryDsn) {
      this.config.sentryDsn = import.meta.env.VITE_SENTRY_DSN;
    }
    if (!this.config.reportUrl) {
      this.config.reportUrl = import.meta.env.VITE_ERROR_REPORT_URL;
    }
    // Production front-door crash telemetry: always fire to the backend-owned
    // endpoint unless an explicit override (env or init config) wins. This is
    // the fire-and-forget sendBeacon sink that receives render errors,
    // window.onerror and unhandledrejection payloads (message/stack/url/UA).
    if (!this.config.reportUrl && import.meta.env.PROD) {
      this.config.reportUrl = "/api/v1/system/frontier-error";
    }

    // Initialize Sentry if DSN is configured
    if (this.config.sentryDsn) {
      this.initSentry();
    }

    // Register global error handlers
    this.registerGlobalHandlers();

    this.initialized = true;
  }

  /**
   * Set the current user ID for error context.
   */
  setUserId(userId: string | undefined): void {
    this.config.userId = userId;
    if (this.sentryInitialized) {
      import("@sentry/react").then((Sentry) => {
        Sentry.setUser(userId ? { id: userId } : null);
      });
    }
  }

  /**
   * Report an error manually.
   */
  reportError(error: Error | string, extra?: Record<string, unknown>): void {
    if (!this.config.enabled) return;

    const errorObj = typeof error === "string" ? new Error(error) : error;

    // Report to Sentry
    if (this.sentryInitialized) {
      import("@sentry/react").then((Sentry) => {
        Sentry.captureException(errorObj, { extra });
      });
    }

    // Report to custom endpoint
    if (this.config.reportUrl) {
      this.sendToCustomEndpoint(errorObj, extra);
    }

    // Log to console in development
    if (import.meta.env.DEV) {
      console.error("[ErrorReporter]", errorObj, extra);
    }
  }

  /**
   * Report a message (non-error event).
   */
  reportMessage(message: string, extra?: Record<string, unknown>): void {
    if (!this.config.enabled) return;

    if (this.sentryInitialized) {
      import("@sentry/react").then((Sentry) => {
        Sentry.captureMessage(message, { extra });
      });
    }

    if (this.config.reportUrl) {
      const report = this.buildReport(message, undefined, extra);
      this.sendReport(report);
    }
  }

  private initSentry(): void {
    try {
      // Dynamic import to avoid bundling Sentry when not configured
      import("@sentry/react").then((Sentry) => {
        Sentry.init({
          dsn: this.config.sentryDsn,
          environment: this.config.environment,
          release: this.config.appVersion,
          integrations: [
            Sentry.browserTracingIntegration(),
            Sentry.replayIntegration({
              maskAllText: true,
              blockAllMedia: true,
            }),
          ],
          tracesSampleRate: this.config.environment === "production" ? 0.1 : 1.0,
          replaysSessionSampleRate: 0.1,
          replaysOnErrorSampleRate: 1.0,
          beforeSend(event) {
            // Don't send errors in development
            if (import.meta.env.DEV) return null;
            return event;
          },
        });
        this.sentryInitialized = true;
      });
    } catch (e) {
      console.warn("[ErrorReporter] Failed to initialize Sentry:", e);
    }
  }

  private registerGlobalHandlers(): void {
    // Global error handler
    window.addEventListener("error", (event) => {
      if (event.error) {
        this.reportError(event.error, {
          source: "window.onerror",
          filename: event.filename,
          lineno: event.lineno,
          colno: event.colno,
        });
      }
    });

    // Unhandled promise rejection handler
    window.addEventListener("unhandledrejection", (event) => {
      const reason = event.reason;
      const error = reason instanceof Error ? reason : new Error(String(reason));
      this.reportError(error, {
        source: "unhandledrejection",
        reason: String(reason),
      });
    });
  }

  private buildReport(
    message: string,
    stack?: string,
    extra?: Record<string, unknown>,
  ): ErrorReport {
    return {
      message,
      stack,
      timestamp: new Date().toISOString(),
      url: window.location.href,
      userAgent: navigator.userAgent,
      environment: this.config.environment || "unknown",
      appVersion: this.config.appVersion || "0.0.0",
      userId: this.config.userId,
      extra,
    };
  }

  private sendToCustomEndpoint(error: Error, extra?: Record<string, unknown>): void {
    const report = this.buildReport(error.message, error.stack, extra);
    this.sendReport(report);
  }

  private sendReport(report: ErrorReport): void {
    if (!this.config.reportUrl) return;

    try {
      navigator.sendBeacon(this.config.reportUrl, JSON.stringify(report));
    } catch {
      // Fallback to fetch if sendBeacon is not available
      fetch(this.config.reportUrl!, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(report),
        keepalive: true,
      }).catch(() => {
        // Silently fail - don't let error reporting cause more errors
      });
    }
  }
}

// Singleton instance
export const errorReporter = new ErrorReporter();
