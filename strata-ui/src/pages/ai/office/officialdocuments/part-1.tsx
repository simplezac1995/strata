// SPDX-License-Identifier: Apache-2.0
// Split out of OfficialDocuments.tsx by scripts/auto-split.mjs.

import { Box, Typography } from "@mui/material";

export interface DocTemplate {
  id: string;
  name: string;
  prompt: string;
  updatedAt: string;
}

export function DocPreviewSkeleton({ title }: { title: string }) {
  return (
    <Box
      sx={{
        border: 1,
        borderColor: "divider",
        borderRadius: 1,
        p: 1.5,
        bgcolor: "#fff",
        minHeight: 150,
      }}
    >
      <Typography variant="caption" sx={{ color: "text.secondary" }}>
        000001
      </Typography>
      <Box sx={{ textAlign: "center", my: 1 }}>
        <Typography sx={{ color: "#c0392b", fontWeight: 700, letterSpacing: 4, fontSize: 15 }}>
          佩格科技集团
        </Typography>
      </Box>
      <Box
        sx={{
          borderBottom: 2,
          borderColor: "#c0392b",
          display: "flex",
          justifyContent: "space-between",
          mb: 1.5,
        }}
      >
        <Typography variant="caption" sx={{ color: "#c0392b" }}>
          发文机关代字〔20××〕10号
        </Typography>
        <Typography variant="caption" sx={{ color: "text.secondary" }}>
          签发人：×××
        </Typography>
      </Box>
      <Typography variant="subtitle2" sx={{ textAlign: "center", fontWeight: 600, mb: 0.5 }}>
        {title || "主体部分标题样式"}
      </Typography>
      <Typography variant="caption" sx={{ color: "text.secondary", lineHeight: 1.6 }}>
        一、一级标题样式
        <br />
        正文内容正文内容正文内容，这里用于预览样式效果。
      </Typography>
    </Box>
  );
}
