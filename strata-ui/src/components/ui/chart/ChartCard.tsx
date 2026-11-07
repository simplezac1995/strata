// SPDX-License-Identifier: Apache-2.0
import { Paper, Typography, Box, type SxProps, type Theme } from "@mui/material";
import type { ReactNode } from "react";

/**
 * Chart card wrapper with MUI Paper styling, title, and subtitle.
 *
 * Provides a consistent container for all chart components with proper
 * spacing, elevation, and typography matching the MUI theme.
 *
 * @example
 * ```tsx
 * <ChartCard title="Monthly Revenue" subtitle="Last 6 months">
 *   <LineChart categories={...} series={...} height={280} />
 * </ChartCard>
 * ```
 */
export interface ChartCardProps {
  /** Chart title */
  title?: string;
  /** Chart subtitle (displayed below title) */
  subtitle?: string;
  /** Action element displayed in the top-right corner */
  action?: ReactNode;
  /** Chart content */
  children: ReactNode;
  /** Additional MUI sx styling */
  sx?: SxProps<Theme>;
}

export function ChartCard({ title, subtitle, action, children, sx }: ChartCardProps) {
  return (
    <Paper
      elevation={1}
      sx={{
        p: 2,
        height: "100%",
        display: "flex",
        flexDirection: "column",
        borderRadius: 2,
        ...sx,
      }}
    >
      {(title || action) && (
        <Box
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "flex-start",
            mb: 1.5,
          }}
        >
          <Box>
            {title && (
              <Typography variant="subtitle1" color="text.primary" sx={{ fontWeight: 600 }}>
                {title}
              </Typography>
            )}
            {subtitle && (
              <Typography variant="body2" color="text.secondary">
                {subtitle}
              </Typography>
            )}
          </Box>
          {action}
        </Box>
      )}
      <Box sx={{ flex: 1, minHeight: 0 }}>{children}</Box>
    </Paper>
  );
}
