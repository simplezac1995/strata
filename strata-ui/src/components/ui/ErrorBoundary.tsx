// SPDX-License-Identifier: Apache-2.0
import { Component, type ErrorInfo, type ReactNode } from "react";
import { Box, Typography, Button, Paper } from "@mui/material";
import { errorReporter } from "@/shared/monitoring/errorReporter";
import { useI18n } from "@/shared/i18n";

interface ErrorBoundaryProps {
  children: ReactNode;
  /** Optional custom fallback UI */
  fallback?: ReactNode;
  /** Whether to show the reset button */
  showReset?: boolean;
  /** Callback when reset is clicked */
  onReset?: () => void;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

/**
 * Internal function component that uses the `useI18n` hook so the fallback
 * UI can be translated. Rendered by the class ErrorBoundary below.
 */
function ErrorFallback({
  error,
  showReset,
  onReset,
}: {
  error: Error | null;
  showReset: boolean;
  onReset: () => void;
}) {
  const { t } = useI18n();

  return (
    <Box
      sx={{
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        minHeight: "400px",
        p: 3,
      }}
    >
      <Paper
        elevation={2}
        sx={{
          p: 4,
          maxWidth: 500,
          textAlign: "center",
        }}
      >
        <Typography variant="h5" gutterBottom color="error">
          {t("error.boundary.title")}
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 2 }}>
          {t("error.boundary.description")}
        </Typography>
        {error && import.meta.env.DEV && (
          <Box
            sx={{
              mt: 2,
              p: 2,
              bgcolor: "grey.100",
              borderRadius: 1,
              textAlign: "left",
            }}
          >
            <Typography variant="caption" color="error">
              {error.message}
            </Typography>
          </Box>
        )}
        {showReset && (
          <Button variant="contained" color="primary" onClick={onReset} sx={{ mt: 3 }}>
            {t("common.retry")}
          </Button>
        )}
      </Paper>
    </Box>
  );
}

/**
 * Error Boundary component that catches JavaScript errors in its child component tree.
 *
 * <p>Logs errors to the error reporter (Sentry/custom endpoint) and displays
 * a user-friendly fallback UI instead of crashing the entire application.
 *
 * <p>Usage:
 * ```tsx
 * <ErrorBoundary>
 *   <MyComponent />
 * </ErrorBoundary>
 *
 * // With custom fallback
 * <ErrorBoundary fallback={<CustomErrorPage />}>
 *   <MyComponent />
 * </ErrorBoundary>
 * ```
 */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    // Report to error monitoring service
    errorReporter.reportError(error, {
      source: "react-error-boundary",
      componentStack: errorInfo.componentStack,
    });
  }

  handleReset = (): void => {
    this.setState({ hasError: false, error: null });
    this.props.onReset?.();
  };

  render(): ReactNode {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <ErrorFallback
          error={this.state.error}
          showReset={this.props.showReset !== false}
          onReset={this.handleReset}
        />
      );
    }

    return this.props.children;
  }
}
