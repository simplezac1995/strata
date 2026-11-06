// SPDX-License-Identifier: Apache-2.0
import { Chip } from "@mui/material";
import { alpha, useTheme, type Theme } from "@mui/material/styles";
import type { SxProps } from "@mui/material/styles";
import { useI18n } from "@/shared/i18n";

type BadgeVariant = "default" | "success" | "error" | "warning" | "info" | "pro" | "ai" | "outline";

interface BadgeProps {
  variant?: BadgeVariant;
  children: React.ReactNode;
  sx?: SxProps<Theme>;
}

function badgeSx(variant: BadgeVariant, theme: Theme): SxProps<Theme> {
  const base = {
    height: "auto",
    fontSize: 12,
    fontWeight: 600,
    borderRadius: 1,
    "& .MuiChip-label": { px: 1, py: 0.25 },
  } satisfies SxProps<Theme>;

  switch (variant) {
    case "success":
      return {
        ...base,
        bgcolor: alpha(theme.palette.success.main, theme.palette.mode === "dark" ? 0.2 : 0.12),
        color:
          theme.palette.mode === "dark" ? theme.palette.success.light : theme.palette.success.dark,
      };
    case "error":
      return {
        ...base,
        bgcolor: alpha(theme.palette.error.main, theme.palette.mode === "dark" ? 0.2 : 0.12),
        color: theme.palette.mode === "dark" ? theme.palette.error.light : theme.palette.error.dark,
      };
    case "warning":
      return {
        ...base,
        bgcolor: alpha(theme.palette.warning.main, theme.palette.mode === "dark" ? 0.2 : 0.12),
        color:
          theme.palette.mode === "dark" ? theme.palette.warning.light : theme.palette.warning.dark,
      };
    case "info":
      return {
        ...base,
        bgcolor: alpha(theme.palette.secondary.main, theme.palette.mode === "dark" ? 0.2 : 0.12),
        color:
          theme.palette.mode === "dark"
            ? theme.palette.secondary.light
            : theme.palette.secondary.dark,
      };
    case "pro":
      return {
        ...base,
        bgcolor: alpha("#7C3AED", theme.palette.mode === "dark" ? 0.22 : 0.12),
        color: theme.palette.mode === "dark" ? "#C4B5FD" : "#6D28D9",
      };
    case "ai":
      return {
        ...base,
        bgcolor: alpha(theme.palette.primary.main, theme.palette.mode === "dark" ? 0.22 : 0.12),
        color:
          theme.palette.mode === "dark" ? theme.palette.primary.light : theme.palette.primary.dark,
      };
    case "outline":
      return {
        ...base,
        bgcolor: "transparent",
        color: "text.secondary",
        border: 1,
        borderColor: "divider",
      };
    default:
      return {
        ...base,
        bgcolor:
          theme.palette.mode === "dark" ? alpha(theme.palette.common.white, 0.08) : "grey.100",
        color: "text.secondary",
      };
  }
}

export function Badge({ variant = "default", children, sx }: BadgeProps) {
  const theme = useTheme();
  return (
    <Chip
      size="small"
      label={children}
      sx={[badgeSx(variant, theme), ...(Array.isArray(sx) ? sx : sx ? [sx] : [])]}
    />
  );
}

const compactBadgeSx = {
  fontSize: 10,
  "& .MuiChip-label": { px: 0.75, py: 0 },
} satisfies SxProps<Theme>;

export function ProBadge() {
  const { t } = useI18n();
  return (
    <Badge variant="pro" sx={compactBadgeSx}>
      {t("nav.pro")}
    </Badge>
  );
}

export function AiBadge() {
  const { t } = useI18n();
  return (
    <Badge variant="ai" sx={compactBadgeSx}>
      {t("ui.badge.aiPack")}
    </Badge>
  );
}
