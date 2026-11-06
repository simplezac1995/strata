// SPDX-License-Identifier: Apache-2.0
// Split out of Documents.tsx by scripts/auto-split.mjs.

import { Stack, Typography } from "@mui/material";

export const DOC_STATUS_VARIANT: Record<string, "warning" | "info" | "success" | "error"> = {
  pending: "warning",
  processing: "info",
  ready: "success",
  failed: "error",
};

export const DOC_STATUS_LABEL: Record<string, string> = {
  pending: "待处理",
  processing: "处理中",
  ready: "已就绪",
  failed: "失败",
};

export interface DocumentForm {
  knowledgeId: string;
  name: string;
  sizeKb: number;
  status: string;
  chunks: number;
}

export function MetaRow({ label, value }: { label: string; value: string }) {
  return (
    <Stack direction="row" spacing={2} sx={{ alignItems: "center" }}>
      <Typography variant="body2" sx={{ color: "text.secondary", width: 110 }}>
        {label}
      </Typography>
      <Typography variant="body2">{value}</Typography>
    </Stack>
  );
}
