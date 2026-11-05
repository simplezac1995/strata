// SPDX-License-Identifier: Apache-2.0
import type { Theme } from "@mui/material/styles";
import { alpha } from "@mui/material/styles";

type ShellStyle = Record<string, unknown>;

/** Auth hero panel — theme primary/secondary gradient (MUI palette). */
export function authHeroPanelSx(theme: Theme): ShellStyle {
  const primary = theme.palette.primary;
  const secondary = theme.palette.secondary;
  return {
    background: `linear-gradient(
      135deg,
      ${primary.dark ?? primary.main} 0%,
      ${primary.main} 48%,
      ${secondary.main} 100%
    )`,
  };
}

export const authFormPaperBaseSx: ShellStyle = {
  p: { xs: 3, sm: 4 },
  borderRadius: 3,
  border: 1,
  borderColor: "divider",
  bgcolor: "background.paper",
};

export function authFormPaperSx(theme: Theme): ShellStyle {
  return {
    ...authFormPaperBaseSx,
    boxShadow:
      theme.palette.mode === "light"
        ? "0 1px 2px rgba(15, 23, 42, 0.04)"
        : "0 1px 2px rgba(0, 0, 0, 0.24)",
  };
}

export const authIconTileSx: ShellStyle = {
  width: 40,
  height: 40,
  borderRadius: 2,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  bgcolor: "primary.main",
  color: "primary.contrastText",
  mb: 2,
};

/** Circular success icon block for auth confirmation states. */
export function authSuccessIconSx(theme: Theme, size = 64): ShellStyle {
  return {
    width: size,
    height: size,
    borderRadius: "50%",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    mx: "auto",
    mb: 2.5,
    bgcolor: alpha(theme.palette.success.main, theme.palette.mode === "light" ? 0.12 : 0.2),
  };
}

export function pageLinkSx(theme: Theme): ShellStyle {
  return {
    color: theme.palette.primary.main,
    fontSize: theme.typography.caption.fontSize,
    fontWeight: 600,
    textDecoration: "none",
    "&:hover": { textDecoration: "underline" },
  };
}

export const kpiCardSx: ShellStyle = {
  p: 2.5,
  height: "100%",
  transition: "box-shadow 0.2s",
  "&:hover": { boxShadow: 4 },
};
