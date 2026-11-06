// SPDX-License-Identifier: Apache-2.0
// Small presentational pieces shared by the bidding steps (caption, step rail icon, stat chip, dot).

import { Box, Typography } from "@mui/material";
import type { StepIconProps } from "@mui/material/StepIcon";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import { Chip } from "@/components/ui/index";

export function Caption({ children, sx }: { children: React.ReactNode; sx?: object }) {
  return (
    <Typography variant="caption" sx={{ color: "text.secondary", display: "block", ...sx }}>
      {children}
    </Typography>
  );
}

export function RailIcon({ active, completed, icon }: StepIconProps) {
  if (completed) return <CheckCircleIcon sx={{ color: "success.main", fontSize: 24 }} />;
  return (
    <Box
      sx={{
        width: 24,
        height: 24,
        borderRadius: "50%",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        fontSize: 12,
        fontWeight: 700,
        bgcolor: active ? "primary.main" : "transparent",
        color: active ? "#fff" : "text.secondary",
        border: active ? 0 : "1px solid",
        borderColor: "divider",
      }}
    >
      {icon}
    </Box>
  );
}

export function StatChip({ children }: { children: React.ReactNode }) {
  return (
    <Chip
      label={children}
      variant="outlined"
      sx={{ height: 26, bgcolor: "background.paper", borderRadius: 1.5 }}
    />
  );
}

/* ============================== 主组件 ============================== */

export function Dot({ color }: { color: string }) {
  return <Box sx={{ width: 8, height: 8, borderRadius: "50%", bgcolor: color }} />;
}

/* ============================== 步骤 6：废标检查 ============================== */
