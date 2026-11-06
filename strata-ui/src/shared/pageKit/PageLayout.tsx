// SPDX-License-Identifier: Apache-2.0
import type { ReactNode } from "react";
import { Box, Card as MuiCard, CardContent, Grid, Link, Stack, Typography } from "@mui/material";
import type { SxProps, Theme } from "@mui/material/styles";
import { Link as RouterLink } from "react-router-dom";
import { pageLinkSx } from "@/theme/pageStyles";

/** Standard vertical spacing for admin pages (24px rhythm). */
export function PageStack({ children, sx }: { children: ReactNode; sx?: SxProps<Theme> }) {
  return (
    <Stack spacing={3} sx={{ width: "100%", minWidth: 0, overflowX: "clip", ...sx }}>
      {children}
    </Stack>
  );
}

/** Negates MainArea padding so full-bleed panes (chat, split views) align with shell edges. */
export const mainAreaBleedSx: SxProps<Theme> = {
  mx: { xs: -2.5, sm: -3, lg: -3.5 },
  my: { xs: -2.5, sm: -3, lg: -3.5 },
  flex: 1,
  minHeight: 0,
  display: "flex",
  flexDirection: "column",
  overflow: "hidden",
};

/** Full-height page body inside MainArea (pair with flex column on shell content). */
export function PageFill({ children, sx }: { children: ReactNode; sx?: SxProps<Theme> }) {
  return (
    <Box sx={sx ? ([mainAreaBleedSx, sx] as SxProps<Theme>) : mainAreaBleedSx}>{children}</Box>
  );
}

/** MUI Card with consistent padding. */
export function ContentCard({
  children,
  sx,
  padding = 2.5,
}: {
  children: ReactNode;
  sx?: SxProps<Theme>;
  padding?: number | false;
}) {
  return (
    <MuiCard sx={sx}>
      <CardContent
        sx={{
          p: padding === false ? 0 : padding,
          "&:last-child": { pb: padding === false ? 0 : padding },
        }}
      >
        {children}
      </CardContent>
    </MuiCard>
  );
}

/** Responsive grid for KPI / card rows. */
export function PageGrid({
  children,
  columns = { xs: 12, sm: 6, xl: 3 },
  spacing = 2.5,
}: {
  children: ReactNode;
  columns?: { xs?: number; sm?: number; md?: number; lg?: number; xl?: number };
  spacing?: number;
}) {
  return (
    <Grid container spacing={spacing} sx={{ width: "100%", maxWidth: "100%" }}>
      {Array.isArray(children)
        ? children.map((child, i) => (
            <Grid key={i} size={columns}>
              {child}
            </Grid>
          ))
        : children}
    </Grid>
  );
}

export function PageLink({ to, children }: { to: string; children: ReactNode }) {
  return (
    <Link component={RouterLink} to={to} sx={(theme) => pageLinkSx(theme)}>
      {children}
    </Link>
  );
}

export function CardSectionHeader({
  title,
  action,
  icon,
}: {
  title: ReactNode;
  action?: ReactNode;
  icon?: ReactNode;
}) {
  return (
    <Stack
      direction="row"
      sx={{ alignItems: "center", justifyContent: "space-between", mb: 2, gap: 1.5 }}
    >
      <Stack direction="row" spacing={1.25} sx={{ alignItems: "center", minWidth: 0 }}>
        {icon}
        <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
          {title}
        </Typography>
      </Stack>
      {action}
    </Stack>
  );
}

export function ListRow({
  primary,
  secondary,
  end,
  action,
}: {
  primary: ReactNode;
  secondary?: ReactNode;
  end?: ReactNode;
  action?: ReactNode;
}) {
  return (
    <Stack
      direction="row"
      spacing={1.5}
      sx={{
        alignItems: "center",
        justifyContent: "space-between",
        py: 1.5,
        borderBottom: 1,
        borderColor: "divider",
        "&:last-child": { borderBottom: 0 },
      }}
    >
      <Box sx={{ minWidth: 0, flex: 1 }}>
        <Typography variant="body2" sx={{ fontWeight: 500 }} noWrap>
          {primary}
        </Typography>
        {secondary ? (
          <Typography variant="caption" color="text.secondary" noWrap component="div">
            {secondary}
          </Typography>
        ) : null}
      </Box>
      {end}
      {action}
    </Stack>
  );
}
