// SPDX-License-Identifier: Apache-2.0
import { CssBaseline, ThemeProvider } from "@mui/material";
import type { ReactNode } from "react";
import { useMemo } from "react";
import { useAppearance } from "@/context/AppearanceContext";
import { createStrataTheme } from "@/theme/muiTheme";

export function MuiProvider({ children }: { children: ReactNode }) {
  const { dark } = useAppearance();
  const theme = useMemo(() => createStrataTheme(dark ? "dark" : "light"), [dark]);

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {children}
    </ThemeProvider>
  );
}
