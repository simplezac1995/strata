// SPDX-License-Identifier: Apache-2.0
import { alpha, type Theme } from "@mui/material/styles";
import type { SidebarTheme } from "@/context/AppearanceContext";

type ShellStyle = Record<string, unknown>;

export function topbarAppBarSx(gradient: boolean, appDark: boolean): ShellStyle {
  if (!gradient) {
    return {
      bgcolor: "background.paper",
      color: "text.primary",
      borderBottom: 1,
      borderColor: "divider",
      boxShadow: "none",
    };
  }
  return {
    position: "relative",
    isolation: "isolate",
    color: "#fff",
    borderBottom: "1px solid rgba(255,255,255,0.1)",
    boxShadow: appDark
      ? "inset 0 1px 0 rgba(255,255,255,0.06), 0 4px 18px -6px rgba(0,0,0,0.55)"
      : "inset 0 1px 0 rgba(255,255,255,0.1), 0 4px 16px -6px rgba(49,46,129,0.45)",
    background: appDark
      ? "linear-gradient(118deg, #1e1b4b 0%, #312e81 32%, #3730a3 62%, #4338ca 100%)"
      : "linear-gradient(118deg, #312e81 0%, #4338ca 26%, #4f46e5 50%, #6366f1 76%, #7c83f6 100%)",
    "&::before": {
      content: '""',
      position: "absolute",
      inset: 0,
      pointerEvents: "none",
      zIndex: 0,
      background: `
        radial-gradient(ellipse 90% 140% at 0% -20%, rgba(255,255,255,0.16), transparent 58%),
        radial-gradient(ellipse 55% 90% at 100% 120%, rgba(15,23,42,0.18), transparent 52%)
      `,
    },
    "& > *": { position: "relative", zIndex: 1 },
  };
}

export function topbarIconButtonSx(gradient: boolean): ShellStyle {
  if (!gradient) return { flexShrink: 0 };
  return {
    flexShrink: 0,
    color: "rgba(255,255,255,0.88)",
    "&:hover": { color: "#fff", bgcolor: "rgba(255,255,255,0.12)" },
  };
}

export function topbarChipButtonSx(gradient: boolean): ShellStyle {
  if (!gradient) {
    return {
      textTransform: "none",
      borderRadius: 1.5,
      color: "inherit",
    };
  }
  return {
    textTransform: "none",
    borderRadius: 1.5,
    color: "rgba(255,255,255,0.92)",
    border: "1px solid rgba(255,255,255,0.2)",
    bgcolor: "rgba(255,255,255,0.1)",
    backdropFilter: "blur(8px)",
    "&:hover": {
      bgcolor: "rgba(255,255,255,0.16)",
      borderColor: "rgba(255,255,255,0.28)",
    },
  };
}

export function topbarSearchButtonSx(gradient: boolean): ShellStyle {
  const base = {
    minHeight: 36,
    px: 1.5,
    borderRadius: 2,
    justifyContent: "flex-start",
    textTransform: "none",
    fontWeight: 500,
  };
  if (!gradient) {
    return {
      ...base,
      color: "text.secondary",
      border: 1,
      borderColor: "divider",
      bgcolor: "background.paper",
      "&:hover": {
        bgcolor: "action.hover",
        borderColor: "primary.light",
        color: "text.primary",
      },
    };
  }
  return {
    ...base,
    border: "1px solid rgba(255,255,255,0.16)",
    bgcolor: "rgba(255,255,255,0.1)",
    color: "rgba(255,255,255,0.78)",
    backdropFilter: "blur(8px)",
    "&:hover": {
      bgcolor: "rgba(255,255,255,0.16)",
      borderColor: "rgba(255,255,255,0.24)",
      color: "#fff",
    },
    "& svg": { color: "rgba(255,255,255,0.75)" },
    "&:hover svg": { color: "#fff" },
  };
}

export function topbarUserButtonSx(gradient: boolean): ShellStyle {
  if (!gradient) return { textTransform: "none", color: "inherit" };
  return {
    textTransform: "none",
    color: "rgba(255,255,255,0.9)",
    "&:hover": { bgcolor: "rgba(255,255,255,0.12)" },
    "& .topbar-user-muted": { color: "rgba(255,255,255,0.65)" },
  };
}

export function horizontalNavButtonSx(
  theme: Theme,
  opts: { onGradient: boolean; active: boolean; inline: boolean },
): ShellStyle {
  const size = opts.inline
    ? { px: 1.5, py: 0.75, fontSize: "13px" }
    : { px: 1.5, py: 0.75, fontSize: "12px" };
  const base = {
    ...size,
    fontWeight: 600,
    whiteSpace: "nowrap",
    flexShrink: 0,
    borderRadius: 1,
    minWidth: 0,
    textTransform: "none",
  } as const;

  if (opts.onGradient) {
    if (opts.active) {
      return {
        ...base,
        bgcolor: "rgba(255,255,255,0.94)",
        color: "#4338ca",
        boxShadow: "0 1px 4px rgba(15,23,42,0.12)",
        "& svg": { color: "#4f46e5" },
      };
    }
    return {
      ...base,
      color: "rgba(255,255,255,0.82)",
      "&:hover": { bgcolor: "rgba(255,255,255,0.1)", color: "#fff" },
    };
  }

  if (opts.active) {
    return {
      ...base,
      bgcolor: alpha(theme.palette.primary.main, theme.palette.mode === "light" ? 0.08 : 0.12),
      color: theme.palette.primary.main,
    };
  }
  return {
    ...base,
    color: theme.palette.text.secondary,
    "&:hover": {
      bgcolor: theme.palette.action.hover,
      color: theme.palette.text.primary,
    },
  };
}

export function horizontalNavBarSx(theme: Theme): ShellStyle {
  return {
    display: "flex",
    minWidth: 0,
    borderTop: 1,
    borderColor: "divider",
    bgcolor: theme.palette.mode === "light" ? "#fafbfc" : "#0d1117",
    px: { xs: 2, sm: 4 },
    py: 1.5,
  };
}

const MENU_GRADIENTS: Record<string, string> = {
  dark: "linear-gradient(180deg, rgba(255,255,255,0.09) 0%, rgba(255,255,255,0.03) 32%, transparent 58%, rgba(0,0,0,0.14) 100%)",
  light:
    "linear-gradient(180deg, rgba(99,102,241,0.09) 0%, rgba(99,102,241,0.03) 28%, transparent 55%, rgba(15,23,42,0.03) 100%)",
  lightDark:
    "linear-gradient(180deg, rgba(129,140,248,0.12) 0%, rgba(99,102,241,0.04) 30%, transparent 58%, rgba(0,0,0,0.1) 100%)",
  forest:
    "linear-gradient(180deg, rgba(74,222,128,0.1) 0%, transparent 45%, rgba(0,0,0,0.12) 100%)",
};

export function menuGradientSx(
  enabled: boolean,
  sidebarTheme: SidebarTheme,
  appDark: boolean,
  variant: "default" | "rail" | "panel",
  customAccent?: string,
): ShellStyle | undefined {
  if (!enabled) return undefined;

  let gradient = MENU_GRADIENTS.dark;
  if (variant === "panel" || sidebarTheme === "light") {
    gradient = appDark ? MENU_GRADIENTS.lightDark : MENU_GRADIENTS.light;
  } else if (sidebarTheme === "forest") {
    gradient = MENU_GRADIENTS.forest;
  } else if (sidebarTheme === "custom" && customAccent) {
    gradient = `linear-gradient(180deg, color-mix(in srgb, ${customAccent} 22%, transparent) 0%, transparent 50%, rgba(0,0,0,0.12) 100%)`;
  }

  return {
    position: "relative",
    isolation: "isolate",
    "&::before": {
      content: '""',
      position: "absolute",
      inset: 0,
      pointerEvents: "none",
      zIndex: 0,
      background: gradient,
    },
    "& > *": { position: "relative", zIndex: 1 },
  };
}

export function scrollButtonSx(gradient: boolean, theme: Theme): ShellStyle {
  if (gradient) {
    return {
      flexShrink: 0,
      width: 28,
      height: 28,
      bgcolor: "rgba(255,255,255,0.12)",
      border: "1px solid rgba(255,255,255,0.2)",
      color: "rgba(255,255,255,0.9)",
      boxShadow: "none",
      "&:hover": { bgcolor: "rgba(255,255,255,0.2)", color: "#fff" },
    };
  }
  return {
    flexShrink: 0,
    width: 28,
    height: 28,
    color: theme.palette.text.secondary,
    bgcolor: theme.palette.mode === "light" ? "rgba(255,255,255,0.9)" : "#161b22",
    border: 1,
    borderColor: "divider",
    boxShadow: theme.palette.mode === "light" ? "0 1px 2px rgba(15,23,42,0.06)" : "none",
    "&:hover": {
      color: theme.palette.primary.main,
      bgcolor: theme.palette.mode === "light" ? "#eef2ff" : alpha(theme.palette.primary.main, 0.12),
    },
  };
}

export function scrollRowSx(className?: string): ShellStyle {
  return {
    display: "flex",
    alignItems: "center",
    gap: 0.5,
    minWidth: 0,
    ...(className ? {} : {}),
  };
}

export function selectionTileSx(active: boolean, theme: Theme): ShellStyle {
  return {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    gap: 1,
    p: 1.25,
    cursor: "pointer",
    borderRadius: 2,
    border: 1,
    borderColor: active ? (theme.palette.mode === "light" ? "#a5b4fc" : "#818cf8") : "divider",
    bgcolor: active
      ? theme.palette.mode === "light"
        ? "rgba(238,242,255,0.65)"
        : "rgba(79,70,229,0.1)"
      : "transparent",
    transition: "border-color 0.15s, background-color 0.15s",
    "&:hover": {
      borderColor:
        theme.palette.mode === "light" ? "rgba(15,23,42,0.14)" : "rgba(226,232,240,0.16)",
      bgcolor: theme.palette.mode === "light" ? "rgba(248,250,252,0.8)" : "rgba(255,255,255,0.03)",
    },
  };
}

export const configSectionLabelSx: ShellStyle = {
  fontSize: 11,
  fontWeight: 500,
  textTransform: "uppercase",
  letterSpacing: "0.05em",
  color: "text.secondary",
};

export function configRowSx(_theme: Theme): ShellStyle {
  return {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 2,
    py: 1,
    borderBottom: 1,
    borderColor: "divider",
    "&:last-of-type": { borderBottom: "none" },
  };
}

export const configListSx: ShellStyle = {
  display: "flex",
  flexDirection: "column",
  gap: 0.25,
};
