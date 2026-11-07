// SPDX-License-Identifier: Apache-2.0
import { Component, type ErrorInfo, type ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { Box, Button, Chip, Stack, Typography } from "@mui/material";
import HomeOutlinedIcon from "@mui/icons-material/HomeOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import BugReportOutlinedIcon from "@mui/icons-material/BugReportOutlined";
import { useI18n } from "@/shared/i18n";
import { errorReporter } from "@/shared/monitoring";

type Props = { children: ReactNode };
type State = { hasError: boolean; error: Error | null };

/**
 * Application-level error boundary. Catches render errors anywhere in the
 * child tree and shows a recoverable crash screen instead of a white screen.
 *
 * Place this at the root of the app (inside providers but outside Routes) so
 * that errors in any page, layout or shared component are caught.
 */
export class AppErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    // Forward every render crash to the error reporter. In production this
    // fires a fire-and-forget sendBeacon to /api/v1/system/frontier-error
    // (configured in errorReporter.init); in dev it also logs to the console.
    // Global window.onerror / unhandledrejection are wired separately in
    // main.tsx via errorReporter.init.
    errorReporter.reportError(error, {
      source: "react-error-boundary",
      componentStack: info.componentStack ?? undefined,
    });
    if (import.meta.env.DEV) {
      // eslint-disable-next-line no-console
      console.error("[AppErrorBoundary] Unhandled render error:", error, info.componentStack);
    }
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
  };

  render() {
    if (this.state.hasError) {
      return <CrashScreen error={this.state.error} onReset={this.handleReset} />;
    }
    return this.props.children;
  }
}

function CrashScreen({ error, onReset }: { error: Error | null; onReset: () => void }) {
  const navigate = useNavigate();
  const { t } = useI18n();
  const isDev = import.meta.env.DEV;

  // Leaving a crashed page: reset the boundary, drop any cached data that may
  // have poisoned the render, and navigate to a known-good route so the user
  // is not stuck re-triggering the same error.
  // NOTE: do NOT call useQueryClient() here — the boundary may render outside
  // the QueryClientProvider (e.g. if the crash happened before providers mount),
  // which itself would throw a second error and white-screen the app.
  const handleReset = () => {
    onReset();
    window.location.href = "/dashboard";
  };

  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        bgcolor: "background.default",
        px: 2,
        position: "relative",
        overflow: "hidden",
      }}
    >
      {/* Decorative glow */}
      <Box
        sx={{
          position: "absolute",
          top: "-10%",
          left: "-5%",
          width: 500,
          height: 500,
          borderRadius: "50%",
          bgcolor: "#8B5CF6",
          opacity: 0.04,
          filter: "blur(80px)",
          pointerEvents: "none",
        }}
      />

      <Box sx={{ position: "relative", textAlign: "center", maxWidth: 460, width: "100%" }}>
        {/* Large faded code + icon overlay */}
        <Box sx={{ position: "relative", mb: 3 }}>
          <Typography
            sx={{
              fontSize: { xs: 120, sm: 160 },
              fontWeight: 600,
              lineHeight: 1,
              letterSpacing: "-0.05em",
              color: "#8B5CF6",
              opacity: 0.08,
              userSelect: "none",
            }}
          >
            CRASH
          </Typography>
          <Box
            sx={{
              position: "absolute",
              inset: 0,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            <Box
              sx={{
                width: 80,
                height: 80,
                borderRadius: 3,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                bgcolor: "#8B5CF618",
                border: "1.5px solid #8B5CF630",
                boxShadow: 2,
                color: "#8B5CF6",
              }}
            >
              <BugReportOutlinedIcon sx={{ fontSize: 36 }} />
            </Box>
          </Box>
        </Box>

        <Typography variant="h4" sx={{ fontWeight: 700, mb: 1.5 }}>
          {t("error.page.server")}
        </Typography>
        <Typography color="text.secondary" sx={{ mb: 4, maxWidth: 340, mx: "auto" }}>
          {t("error.page.server.desc")}
        </Typography>

        {/* Dev-only error detail */}
        {isDev && error && (
          <Box
            sx={{
              textAlign: "left",
              bgcolor: "grey.900",
              color: "grey.100",
              borderRadius: 1,
              p: 2,
              mb: 3,
              maxHeight: 200,
              overflow: "auto",
              fontFamily: "monospace",
              fontSize: 12,
              lineHeight: 1.5,
            }}
          >
            <Typography sx={{ color: "error.light", fontWeight: 600, mb: 0.5, fontSize: 12 }}>
              {error.name}: {error.message}
            </Typography>
            <Typography sx={{ whiteSpace: "pre-wrap", fontSize: 11, opacity: 0.8 }}>
              {error.stack}
            </Typography>
          </Box>
        )}

        <Stack
          direction={{ xs: "column", sm: "row" }}
          spacing={1.5}
          sx={{ justifyContent: "center" }}
        >
          <Button variant="outlined" startIcon={<RefreshOutlinedIcon />} onClick={handleReset}>
            {t("auth.common.tryAgain")}
          </Button>
          <Button
            variant="contained"
            startIcon={<HomeOutlinedIcon />}
            onClick={() => navigate("/dashboard")}
          >
            {t("nav.dashboard")}
          </Button>
        </Stack>

        <Chip
          size="small"
          label={t("error.page.badge").replace("{code}", "CRASH")}
          sx={{
            mt: 5,
            fontFamily: "monospace",
            fontSize: 11,
            "&::before": {
              content: '""',
              width: 6,
              height: 6,
              borderRadius: "50%",
              bgcolor: "#8B5CF6",
              mr: 0.5,
            },
          }}
        />
      </Box>
    </Box>
  );
}
