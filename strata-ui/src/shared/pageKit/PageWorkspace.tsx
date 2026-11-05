// SPDX-License-Identifier: Apache-2.0
import type { ReactNode } from "react";
import { Box, Grid, Stack, Typography } from "@mui/material";
import type { SxProps, Theme } from "@mui/material/styles";
import { PageHeader } from "@/components/ui/index";
import { ContentCard, PageStack } from "@/shared/pageKit/PageLayout";

const heroGradient = (theme: Theme) =>
  theme.palette.mode === "dark"
    ? "linear-gradient(135deg, rgba(99, 102, 241, 0.14) 0%, rgba(15, 23, 42, 0.4) 55%, transparent 100%)"
    : "linear-gradient(135deg, rgba(238, 242, 255, 0.95) 0%, rgba(255, 255, 255, 0.6) 50%, transparent 100%)";

export function PageWorkspace({ children, sx }: { children: ReactNode; sx?: SxProps<Theme> }) {
  return (
    <Box sx={{ width: "100%", maxWidth: 1280, mx: "auto", ...sx }}>
      <PageStack sx={{ gap: 3.5 }}>{children}</PageStack>
    </Box>
  );
}

export function PageHero({
  title,
  subtitle,
  actions,
  footer,
}: {
  title: ReactNode;
  subtitle?: ReactNode;
  actions?: ReactNode;
  footer?: ReactNode;
}) {
  return (
    <ContentCard
      sx={{
        background: heroGradient,
        border: 1,
        borderColor: "divider",
        boxShadow: (theme) =>
          theme.palette.mode === "dark"
            ? "0 1px 0 rgba(255,255,255,0.04) inset"
            : "0 1px 0 rgba(255,255,255,0.8) inset",
      }}
    >
      <Stack spacing={2.5}>
        <PageHeader title={title} subtitle={subtitle} actions={actions} />
        {footer}
      </Stack>
    </ContentCard>
  );
}

export function PageMetric({
  label,
  value,
  hint,
}: {
  label: string;
  value: ReactNode;
  hint?: ReactNode;
}) {
  return (
    <Box
      sx={{
        px: 2.5,
        py: 2,
        borderRadius: 3,
        bgcolor: "background.paper",
        border: 1,
        borderColor: "divider",
        minHeight: 88,
        display: "flex",
        flexDirection: "column",
        justifyContent: "center",
      }}
    >
      <Typography
        variant="caption"
        color="text.secondary"
        sx={{ fontWeight: 600, letterSpacing: 0.4 }}
      >
        {label}
      </Typography>
      <Typography
        variant="h4"
        sx={{ fontWeight: 700, letterSpacing: "-0.03em", mt: 0.5, lineHeight: 1.2 }}
      >
        {value}
      </Typography>
      {hint ? (
        <Typography variant="caption" color="text.secondary" sx={{ mt: 0.75 }}>
          {hint}
        </Typography>
      ) : null}
    </Box>
  );
}

export function PageMetricRow({ children }: { children: ReactNode }) {
  return (
    <Grid container spacing={2}>
      {Array.isArray(children)
        ? children.map((child, index) => (
            <Grid key={index} size={{ xs: 12, sm: 6, md: 4 }}>
              {child}
            </Grid>
          ))
        : children}
    </Grid>
  );
}

export function PageSection({
  title,
  description,
  action,
  children,
  padding = 3,
}: {
  title: ReactNode;
  description?: ReactNode;
  action?: ReactNode;
  children: ReactNode;
  padding?: number | false;
}) {
  return (
    <ContentCard padding={padding}>
      <Stack
        direction={{ xs: "column", sm: "row" }}
        spacing={1.5}
        sx={{ mb: 2.5, justifyContent: "space-between", alignItems: { sm: "flex-start" } }}
      >
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="h6" sx={{ fontWeight: 650, letterSpacing: "-0.02em" }}>
            {title}
          </Typography>
          {description ? (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75, maxWidth: "42rem" }}>
              {description}
            </Typography>
          ) : null}
        </Box>
        {action}
      </Stack>
      {children}
    </ContentCard>
  );
}
