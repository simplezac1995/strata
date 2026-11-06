// SPDX-License-Identifier: Apache-2.0
import { Box, Card as MuiCard, CardContent, Skeleton, Stack } from "@mui/material";

/**
 * Card-shaped skeleton that mirrors the KpiCard layout (overline label, large
 * value line, caption, and a trailing rounded icon tile). Drop it in place of a
 * real card while KPI / stat data is loading to avoid layout shift.
 */
export function CardSkeleton({ rows = 1 }: { rows?: number }) {
  return (
    <MuiCard sx={{ height: "100%", overflow: "hidden" }}>
      <CardContent sx={{ p: 2.5, "&:last-child": { pb: 2.5 } }}>
        <Stack direction="row" sx={{ justifyContent: "space-between", gap: 2 }}>
          <Box sx={{ minWidth: 0, flex: 1 }}>
            <Skeleton variant="text" width="55%" height={14} />
            <Skeleton variant="text" width="40%" height={36} sx={{ mt: 0.5 }} />
            <Skeleton variant="text" width="35%" height={13} sx={{ mt: 0.5 }} />
            {Array.from({ length: Math.max(0, rows - 1) }).map((_, i) => (
              <Skeleton key={i} variant="text" width="80%" height={13} sx={{ mt: 0.5 }} />
            ))}
          </Box>
          <Skeleton variant="rounded" width={44} height={44} sx={{ flexShrink: 0 }} />
        </Stack>
      </CardContent>
    </MuiCard>
  );
}

/**
 * Full-page skeleton: page header (title + subtitle) followed by a card block
 * with a few text lines. Use as the loading fallback for a page before its
 * data resolves.
 */
export function PageSkeleton({ lines = 4 }: { lines?: number }) {
  return (
    <Stack spacing={2.5}>
      <Box>
        <Skeleton variant="text" width={220} height={32} />
        <Skeleton variant="text" width={160} height={18} />
      </Box>
      <MuiCard>
        <CardContent sx={{ p: 2.5 }}>
          {Array.from({ length: lines }).map((_, i) => (
            <Skeleton key={i} variant="text" width={`${Math.max(30, 90 - i * 12)}%`} height={22} />
          ))}
        </CardContent>
      </MuiCard>
    </Stack>
  );
}

/**
 * Table-shaped skeleton: a header row plus several body rows with text cells.
 * Use as the loading state for list pages that render their own table markup.
 */
export function TableSkeleton({ rows = 6, cols = 5 }: { rows?: number; cols?: number }) {
  return (
    <Stack spacing={1.5}>
      <Skeleton variant="rectangular" height={48} sx={{ borderRadius: 1 }} />
      {Array.from({ length: rows }).map((_, r) => (
        <Stack key={r} direction="row" spacing={2} sx={{ alignItems: "center", px: 1 }}>
          {Array.from({ length: cols }).map((_, c) => (
            <Skeleton key={c} variant="text" height={20} sx={{ width: c === 0 ? "22%" : "16%" }} />
          ))}
        </Stack>
      ))}
    </Stack>
  );
}
