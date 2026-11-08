// SPDX-License-Identifier: Apache-2.0
import { useNavigate } from "react-router-dom";
import { Box, Button, Chip, Stack, Typography } from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import HomeOutlinedIcon from "@mui/icons-material/HomeOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import GppBadOutlinedIcon from "@mui/icons-material/GppBadOutlined";
import ReportProblemOutlinedIcon from "@mui/icons-material/ReportProblemOutlined";
import type { ReactNode } from "react";
import { useI18n } from "@/shared/i18n";

function ErrorLayout({
  code,
  icon,
  color,
  title,
  subtitle,
  actions,
}: {
  code: string;
  icon: ReactNode;
  color: string;
  title: string;
  subtitle: string;
  actions: ReactNode;
}) {
  const { t } = useI18n();
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
      <Box
        sx={{
          position: "absolute",
          top: "-10%",
          left: "-5%",
          width: 500,
          height: 500,
          borderRadius: "50%",
          bgcolor: color,
          opacity: 0.04,
          filter: "blur(80px)",
          pointerEvents: "none",
        }}
      />
      <Box
        sx={{
          position: "absolute",
          bottom: "-10%",
          right: "-5%",
          width: 400,
          height: 400,
          borderRadius: "50%",
          bgcolor: color,
          opacity: 0.04,
          filter: "blur(80px)",
          pointerEvents: "none",
        }}
      />

      <Box sx={{ position: "relative", textAlign: "center", maxWidth: 420, width: "100%" }}>
        <Box sx={{ position: "relative", mb: 3 }}>
          <Typography
            sx={{
              fontSize: { xs: 120, sm: 160 },
              fontWeight: 600,
              lineHeight: 1,
              letterSpacing: "-0.05em",
              color,
              opacity: 0.08,
              userSelect: "none",
            }}
          >
            {code}
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
                bgcolor: `${color}18`,
                border: `1.5px solid ${color}30`,
                boxShadow: 2,
                color,
              }}
            >
              {icon}
            </Box>
          </Box>
        </Box>

        <Typography variant="h4" sx={{ fontWeight: 700, mb: 1.5 }}>
          {title}
        </Typography>
        <Typography color="text.secondary" sx={{ mb: 4, maxWidth: 320, mx: "auto" }}>
          {subtitle}
        </Typography>

        <Stack
          direction={{ xs: "column", sm: "row" }}
          spacing={1.5}
          sx={{ justifyContent: "center" }}
        >
          {actions}
        </Stack>

        <Chip
          size="small"
          label={t("error.page.badge").replace("{code}", code)}
          sx={{
            mt: 5,
            fontFamily: "monospace",
            fontSize: 11,
            "&::before": {
              content: '""',
              width: 6,
              height: 6,
              borderRadius: "50%",
              bgcolor: color,
              mr: 0.5,
            },
          }}
        />
      </Box>
    </Box>
  );
}

export function Error403() {
  const navigate = useNavigate();
  const { t } = useI18n();
  return (
    <ErrorLayout
      code="403"
      icon={<GppBadOutlinedIcon sx={{ fontSize: 36 }} />}
      color="#EF4444"
      title={t("error.page.access.denied")}
      subtitle={t("error.page.access.denied.desc")}
      actions={
        <>
          <Button variant="outlined" startIcon={<ArrowBackIcon />} onClick={() => navigate(-1)}>
            {t("common.goBack")}
          </Button>
          <Button
            variant="contained"
            startIcon={<HomeOutlinedIcon />}
            onClick={() => navigate("/dashboard")}
          >
            {t("nav.dashboard")}
          </Button>
        </>
      }
    />
  );
}

export function Error500() {
  const navigate = useNavigate();
  const { t } = useI18n();
  return (
    <ErrorLayout
      code="500"
      icon={<ReportProblemOutlinedIcon sx={{ fontSize: 36 }} />}
      color="#F59E0B"
      title={t("error.page.server")}
      subtitle={t("error.page.server.desc")}
      actions={
        <>
          <Button
            variant="outlined"
            startIcon={<RefreshOutlinedIcon />}
            onClick={() => window.location.reload()}
          >
            {t("auth.common.tryAgain")}
          </Button>
          <Button
            variant="contained"
            startIcon={<HomeOutlinedIcon />}
            onClick={() => navigate("/dashboard")}
          >
            {t("nav.dashboard")}
          </Button>
        </>
      }
    />
  );
}
