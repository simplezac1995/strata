// SPDX-License-Identifier: Apache-2.0
import type { ReactNode } from "react";
import {
  Box,
  Card as MuiCard,
  CardContent,
  Stack,
  Tab,
  Tabs as MuiTabs,
  Typography,
} from "@mui/material";
import type { SxProps, Theme } from "@mui/material/styles";
import { alpha, useTheme } from "@mui/material/styles";

export function Card({
  children,
  className = "",
  sx,
  padding = 2.5,
}: {
  children: ReactNode;
  className?: string;
  sx?: SxProps<Theme>;
  padding?: number | false;
}) {
  return (
    <MuiCard className={className} sx={sx}>
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

export function KpiCard({
  title,
  value,
  delta,
  icon,
  color = "primary",
}: {
  title: string;
  value: string | number;
  delta?: string;
  icon: React.ReactNode;
  color?: "primary" | "success" | "warning" | "error" | "secondary";
}) {
  const theme = useTheme();
  const tone =
    color === "error"
      ? theme.palette.error.main
      : color === "warning"
        ? theme.palette.warning.main
        : color === "success"
          ? theme.palette.success.main
          : color === "secondary"
            ? theme.palette.secondary.main
            : theme.palette.primary.main;
  return (
    <MuiCard sx={{ height: "100%", overflow: "hidden" }}>
      <CardContent sx={{ p: 2.5, "&:last-child": { pb: 2.5 } }}>
        <Stack
          direction="row"
          sx={{ justifyContent: "space-between", alignItems: "flex-start", gap: 2 }}
        >
          <Box sx={{ minWidth: 0, flex: 1 }}>
            <Typography
              variant="overline"
              color="text.secondary"
              sx={{ display: "block", lineHeight: 1.4 }}
            >
              {title}
            </Typography>
            <Typography variant="h4" sx={{ mt: 1, fontWeight: 700, letterSpacing: "-0.02em" }}>
              {value}
            </Typography>
            {delta ? (
              <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: "block" }}>
                {delta}
              </Typography>
            ) : null}
          </Box>
          <Box
            sx={{
              width: 44,
              height: 44,
              borderRadius: 2.5,
              bgcolor: alpha(tone, 0.1),
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              color: tone,
              flexShrink: 0,
              "& .MuiSvgIcon-root": { fontSize: 22 },
            }}
          >
            {icon}
          </Box>
        </Stack>
      </CardContent>
    </MuiCard>
  );
}

export function SectionLabel({ children }: { children: ReactNode }) {
  return (
    <Typography
      variant="caption"
      color="text.secondary"
      sx={{ px: 1.5, pt: 2, pb: 1, display: "block", textTransform: "uppercase", letterSpacing: 1 }}
    >
      {children}
    </Typography>
  );
}

export function Tabs({
  tabs,
  active,
  onChange,
}: {
  tabs: { id: string; label: string }[];
  active: string;
  onChange: (id: string) => void;
}) {
  return (
    <MuiTabs
      value={active}
      onChange={(_, id) => onChange(id)}
      sx={{ mb: 2, borderBottom: 1, borderColor: "divider" }}
    >
      {tabs.map((tab) => (
        <Tab key={tab.id} value={tab.id} label={tab.label} />
      ))}
    </MuiTabs>
  );
}
