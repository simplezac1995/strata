// SPDX-License-Identifier: Apache-2.0
import type { ReactNode } from "react";
import { Alert, Box } from "@mui/material";
import { alpha } from "@mui/material/styles";

/** MUI Alert for MVP / Pro boundary hints — structured message column, no raw `<p>` margins. */
export function InfoBanner({ children }: { children: ReactNode }) {
  return (
    <Alert
      severity="warning"
      variant="outlined"
      sx={(theme) => ({
        alignItems: "center",
        borderRadius: 2,
        py: 1.25,
        px: 1.5,
        borderColor: alpha(
          theme.palette.warning.main,
          theme.palette.mode === "light" ? 0.35 : 0.45,
        ),
        bgcolor: alpha(theme.palette.warning.main, theme.palette.mode === "light" ? 0.06 : 0.1),
        "& .MuiAlert-icon": {
          color: "warning.main",
          alignItems: "center",
          mr: 1.25,
          opacity: 1,
        },
        "& .MuiAlert-message": {
          p: 0,
          minWidth: 0,
          flex: 1,
        },
      })}
    >
      <Box
        sx={{
          display: "flex",
          flexDirection: "column",
          gap: 0.75,
          minWidth: 0,
          "& p": { m: 0 },
        }}
      >
        {children}
      </Box>
    </Alert>
  );
}
