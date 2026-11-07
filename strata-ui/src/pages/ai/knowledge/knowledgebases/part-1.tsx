// SPDX-License-Identifier: Apache-2.0
// Split out of KnowledgeBases.tsx by scripts/auto-split.mjs.

import { Box, Card, Stack, Typography, Chip } from "@mui/material";

export interface KnowledgeForm {
  name: string;
  description: string;
  mode: "q2a" | "q2q";
  welcome: string;
  vectorStore: string;
  embeddingModel: string;
  chunkSize: number;
  chunkOverlap: number;
  enabled: boolean;
}

export const STEPS = ["基础配置", "高级配置", "安全配置", "语义设置"];

export const EMBEDDING_MODELS = [
  "text-embedding-3-large",
  "text-embedding-3-small",
  "bge-large-zh",
];

export function ModeCard({
  icon,
  title,
  sub,
  desc,
  tags,
  onClick,
}: {
  icon: React.ReactNode;
  title: string;
  sub: string;
  desc: string;
  tags: string[];
  onClick: () => void;
}) {
  return (
    <Card
      variant="outlined"
      onClick={onClick}
      sx={{
        cursor: "pointer",
        p: 2,
        textAlign: "center",
        "&:hover": { borderColor: "primary.main", bgcolor: "action.hover" },
      }}
    >
      <Box
        sx={{
          width: 56,
          height: 56,
          borderRadius: 2,
          bgcolor: "action.hover",
          color: "text.secondary",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          mx: "auto",
        }}
      >
        {icon}
      </Box>
      <Typography variant="subtitle1" sx={{ fontWeight: 600, mt: 1.5 }}>
        {title}
      </Typography>
      <Typography variant="caption" sx={{ color: "primary.main" }}>
        {sub}
      </Typography>
      <Typography variant="body2" sx={{ color: "text.secondary", mt: 1 }}>
        {desc}
      </Typography>
      <Stack
        direction="row"
        spacing={0.5}
        sx={{ justifyContent: "center", mt: 1.5, flexWrap: "wrap" }}
      >
        {tags.map((t) => (
          <Chip key={t} label={t} size="small" variant="outlined" />
        ))}
      </Stack>
    </Card>
  );
}

export function SectionLabel({ text }: { text: string }) {
  return (
    <Typography variant="subtitle2" sx={{ fontWeight: 600, color: "text.primary" }}>
      {text}
    </Typography>
  );
}
