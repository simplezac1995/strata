// SPDX-License-Identifier: Apache-2.0
import { alpha, createTheme, type ThemeOptions } from "@mui/material/styles";

/** Strata brand primary — single source for theme and sidebar accents. */
export const STRATA_PRIMARY = "#4F46E5";

/** Primary color at a given opacity (for tinted backgrounds). */
export function strataPrimaryAlpha(opacity: number): string {
  return alpha(STRATA_PRIMARY, opacity);
}

const baseTypography: ThemeOptions["typography"] = {
  fontFamily: '"Inter", system-ui, -apple-system, sans-serif',
  fontSize: 14,
  h1: { fontSize: "2rem", fontWeight: 700, letterSpacing: "-0.025em", lineHeight: 1.25 },
  h2: { fontSize: "1.5rem", fontWeight: 700, letterSpacing: "-0.02em", lineHeight: 1.3 },
  h3: { fontSize: "1.25rem", fontWeight: 600, letterSpacing: "-0.015em", lineHeight: 1.35 },
  h4: { fontSize: "1.125rem", fontWeight: 600, lineHeight: 1.4 },
  h5: { fontSize: "1rem", fontWeight: 600, lineHeight: 1.45 },
  h6: { fontSize: "0.875rem", fontWeight: 600, lineHeight: 1.5 },
  subtitle1: { fontSize: "0.9375rem", fontWeight: 600, lineHeight: 1.5 },
  subtitle2: { fontSize: "0.8125rem", fontWeight: 600, lineHeight: 1.5 },
  body1: { fontSize: "0.875rem", lineHeight: 1.6 },
  body2: { fontSize: "0.8125rem", lineHeight: 1.55 },
  caption: { fontSize: "0.75rem", lineHeight: 1.5 },
  overline: {
    fontSize: "0.6875rem",
    fontWeight: 600,
    letterSpacing: "0.06em",
    lineHeight: 1.5,
    textTransform: "uppercase",
  },
  button: { textTransform: "none", fontWeight: 600 },
};

const baseShape: ThemeOptions["shape"] = {
  borderRadius: 12,
};

function tableHeadBg(mode: "light" | "dark") {
  return mode === "light" ? alpha("#0F172A", 0.03) : alpha("#F8FAFC", 0.04);
}

function buildComponents(mode: "light" | "dark"): ThemeOptions["components"] {
  return {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          scrollbarColor: mode === "light" ? "#cbd5e1 transparent" : "#334155 transparent",
        },
      },
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { borderRadius: 10, fontWeight: 600, paddingInline: 16 },
        sizeSmall: { paddingInline: 12, fontSize: "0.8125rem" },
      },
    },
    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: {
          border: "1px solid",
          borderColor: "divider",
          boxShadow:
            mode === "light" ? "0 1px 2px rgba(15, 23, 42, 0.04)" : "0 1px 2px rgba(0, 0, 0, 0.24)",
        },
      },
    },
    MuiPaper: {
      defaultProps: { elevation: 0 },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: ({ theme }) => ({
          borderRadius: 10,
          backgroundColor: theme.palette.mode === "light" ? "#FFFFFF" : alpha("#FFFFFF", 0.03),
          "&:hover .MuiOutlinedInput-notchedOutline": {
            borderColor: alpha(STRATA_PRIMARY, 0.35),
          },
          "&.Mui-focused .MuiOutlinedInput-notchedOutline": {
            borderWidth: 1.5,
          },
          "& input:-webkit-autofill": {
            WebkitBoxShadow: `0 0 0 100px ${
              theme.palette.mode === "light" ? "#FFFFFF" : theme.palette.background.paper
            } inset`,
            WebkitTextFillColor: theme.palette.text.primary,
            caretColor: theme.palette.text.primary,
            transition: "background-color 9999s ease-out 0s",
          },
        }),
        input: {
          paddingTop: 10,
          paddingBottom: 10,
        },
      },
    },
    MuiInputLabel: {
      styleOverrides: {
        root: { fontSize: "0.8125rem" },
      },
    },
    MuiTableContainer: {
      styleOverrides: {
        root: { borderRadius: 12, overflow: "hidden" },
      },
    },
    MuiTableHead: {
      styleOverrides: {
        root: {
          "& .MuiTableCell-head": {
            bgcolor: tableHeadBg(mode),
            color: mode === "light" ? "#475569" : "#94A3B8",
            fontSize: "0.75rem",
            fontWeight: 600,
            letterSpacing: "0.04em",
            textTransform: "uppercase",
            borderBottom: "1px solid",
            borderColor: "divider",
            py: 1.25,
            whiteSpace: "nowrap",
          },
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderColor: "divider",
          py: 1.5,
          fontSize: "0.8125rem",
        },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: {
          "&:last-child td": { borderBottom: 0 },
          "&.MuiTableRow-hover:hover": {
            bgcolor: mode === "light" ? alpha(STRATA_PRIMARY, 0.04) : alpha(STRATA_PRIMARY, 0.08),
          },
        },
      },
    },
    MuiDialog: {
      styleOverrides: {
        paper: {
          borderRadius: 16,
          border: "1px solid",
          borderColor: "divider",
        },
      },
    },
    MuiDialogTitle: {
      styleOverrides: {
        root: { fontSize: "1.0625rem", fontWeight: 700, pb: 1 },
      },
    },
    MuiTabs: {
      styleOverrides: {
        indicator: { height: 2, borderRadius: 1 },
      },
    },
    MuiTab: {
      styleOverrides: {
        root: {
          textTransform: "none",
          fontWeight: 600,
          fontSize: "0.8125rem",
          minHeight: 44,
        },
      },
    },
    MuiStepper: {
      styleOverrides: {
        root: { bgcolor: "transparent" },
      },
    },
    MuiStepLabel: {
      styleOverrides: {
        label: {
          fontSize: "0.75rem",
          fontWeight: 600,
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: { fontWeight: 600 },
      },
    },
    MuiAlert: {
      styleOverrides: {
        root: ({ theme }) => ({
          borderRadius: 8,
          alignItems: "center",
          width: "100%",
          boxSizing: "border-box",
          padding: theme.spacing(0.75, 1.5),
        }),
        icon: {
          alignItems: "center",
          padding: 0,
          marginRight: 10,
          opacity: 1,
        },
        message: {
          padding: 0,
          minWidth: 0,
          flex: 1,
          overflow: "hidden",
          lineHeight: 1.43,
        },
      },
    },
    MuiFormHelperText: {
      styleOverrides: {
        root: {
          marginLeft: 0,
          marginRight: 0,
          lineHeight: 1.45,
        },
      },
    },
    MuiSwitch: {
      defaultProps: { size: "small" },
    },
    MuiAppBar: {
      defaultProps: { elevation: 0, color: "default" },
      styleOverrides: {
        root: {
          borderBottom: "1px solid",
          borderColor: "divider",
          backgroundImage: "none",
        },
      },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: {
          backgroundImage: "none",
        },
      },
    },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
        },
      },
    },
    MuiSnackbar: {
      defaultProps: {
        anchorOrigin: { vertical: "bottom", horizontal: "right" },
      },
    },
    MuiPaginationItem: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          fontWeight: 600,
        },
      },
    },
  };
}

export function createStrataTheme(mode: "light" | "dark") {
  return createTheme({
    palette: {
      mode,
      primary: {
        main: STRATA_PRIMARY,
        dark: mode === "light" ? "#4338CA" : "#818CF8",
        light: mode === "light" ? "#6366F1" : "#A5B4FC",
        contrastText: "#ffffff",
      },
      secondary: {
        main: mode === "light" ? "#0EA5E9" : "#38BDF8",
      },
      success: { main: "#16A34A" },
      warning: { main: "#D97706" },
      error: { main: "#DC2626" },
      background: {
        default: mode === "light" ? "#F4F6FB" : "#0D1117",
        paper: mode === "light" ? "#FFFFFF" : "#161B22",
      },
      divider: mode === "light" ? "rgba(15, 23, 42, 0.08)" : "#21262D",
      text: {
        primary: mode === "light" ? "#0F172A" : "#E5E7EB",
        secondary: mode === "light" ? "#64748B" : "#94A3B8",
      },
    },
    typography: baseTypography,
    shape: baseShape,
    spacing: 8,
    components: buildComponents(mode),
  });
}
