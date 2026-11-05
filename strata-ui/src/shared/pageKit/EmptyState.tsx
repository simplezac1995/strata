// SPDX-License-Identifier: Apache-2.0
import type { ReactNode } from "react";
import { Box, Paper, Stack, Typography } from "@mui/material";
import { InboxOutlined } from "@mui/icons-material";

/** Unified empty placeholder — use instead of ad-hoc "No data" copy. */
export function EmptyState({
  message,
  title,
  description,
  icon,
  action,
}: {
  /** Single-line or custom body (preferred). */
  message?: ReactNode;
  /** Legacy: bold title + optional description. */
  title?: string;
  description?: string;
  icon?: ReactNode;
  action?: ReactNode;
}) {
  const body =
    message ??
    (title ? (
      <Stack spacing={0.75} sx={{ alignItems: "center" }}>
        <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
          {title}
        </Typography>
        {description ? (
          <Typography variant="body2" color="text.secondary">
            {description}
          </Typography>
        ) : null}
      </Stack>
    ) : null);

  return (
    <Paper
      variant="outlined"
      sx={{
        py: 7,
        px: 3,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        textAlign: "center",
        borderRadius: 3,
        bgcolor: (theme) =>
          theme.palette.mode === "light" ? "rgba(255,255,255,0.72)" : "background.paper",
      }}
    >
      <Box
        sx={{
          width: 48,
          height: 48,
          borderRadius: 2.5,
          bgcolor: (theme) => (theme.palette.mode === "light" ? "primary.main" : "primary.dark"),
          color: "primary.contrastText",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          mb: 2,
          opacity: 0.92,
        }}
      >
        {icon ?? <InboxOutlined sx={{ fontSize: 22 }} />}
      </Box>
      {typeof body === "string" ? (
        <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 420, lineHeight: 1.6 }}>
          {body}
        </Typography>
      ) : (
        body
      )}
      {action ? <Box sx={{ mt: 2.5 }}>{action}</Box> : null}
    </Paper>
  );
}
