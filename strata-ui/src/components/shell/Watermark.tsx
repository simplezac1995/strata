// SPDX-License-Identifier: Apache-2.0
import { Box, Typography, useTheme } from "@mui/material";
import { useI18n } from "@/shared/i18n";

/** Diagonal username watermark overlay for sensitive views. */
export function Watermark({ text }: { text: string }) {
  const { t } = useI18n();
  const theme = useTheme();
  const label = text.trim() || t("shell.admin");

  return (
    <Box
      aria-hidden
      sx={{
        pointerEvents: "none",
        position: "absolute",
        inset: 0,
        zIndex: 1,
        overflow: "hidden",
        userSelect: "none",
      }}
    >
      <Box
        sx={{
          position: "absolute",
          inset: "-50%",
          width: "200%",
          height: "200%",
          display: "flex",
          flexWrap: "wrap",
          alignContent: "center",
          justifyContent: "center",
          gap: "80px 64px",
          opacity: theme.palette.mode === "light" ? 0.06 : 0.08,
          transform: "rotate(-24deg) scale(1.1)",
        }}
      >
        {Array.from({ length: 24 }, (_, i) => (
          <Typography
            key={i}
            variant="body2"
            sx={{ fontWeight: 500, whiteSpace: "nowrap", color: "text.primary" }}
          >
            {label}
          </Typography>
        ))}
      </Box>
    </Box>
  );
}
