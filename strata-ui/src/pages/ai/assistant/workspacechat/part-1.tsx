// SPDX-License-Identifier: Apache-2.0
// Split out of WorkspaceChat.tsx by scripts/auto-split.mjs.

import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import { Box, Card, CardContent, Chip, Divider, Stack, Typography } from "@mui/material";
import { Button } from "@/components/ui/index";

export function ConfirmCard({
  raw,
  onConfirm,
}: {
  raw: string;
  onConfirm: (tool: string, args: string) => void;
}) {
  const m = raw.match(/^\[CONFIRM_REQUIRED:([a-z_]+):(.*)\]$/s);
  if (!m)
    return (
      <Typography
        component="pre"
        variant="body2"
        sx={{ whiteSpace: "pre-wrap", fontFamily: "inherit", m: 0 }}
      >
        {raw}
      </Typography>
    );
  const [, tool, args] = m;
  let fields: [string, string][] = [];
  try {
    const obj = JSON.parse(args);
    fields = Object.entries(obj).map(([k, v]) => [
      k,
      typeof v === "object" ? JSON.stringify(v) : String(v),
    ]);
  } catch {
    fields = [["args", args]];
  }
  return (
    <Card
      variant="outlined"
      sx={{ minWidth: 340, maxWidth: 520, borderRadius: 3, borderColor: "primary.light" }}
    >
      <Box sx={{ display: "flex", alignItems: "center", gap: 1, px: 2.5, pt: 2, pb: 1 }}>
        <WarningAmberIcon color="warning" fontSize="small" />
        <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
          请确认操作
        </Typography>
        <Chip
          label={tool}
          size="small"
          color="primary"
          variant="outlined"
          sx={{ ml: "auto", fontFamily: "monospace", fontSize: 11 }}
        />
      </Box>
      <CardContent sx={{ pt: 0 }}>
        <Stack spacing={1.2}>
          {fields.map(([k, v]) => (
            <Box key={k} sx={{ display: "flex", alignItems: "flex-start", gap: 1.5 }}>
              <Typography
                variant="caption"
                sx={{ minWidth: 110, pt: 0.5, color: "text.secondary", fontWeight: 500 }}
              >
                <span style={{ color: "error.main" }}>* </span>
                {k}
              </Typography>
              <Typography
                variant="body2"
                sx={{
                  flex: 1,
                  bgcolor: "action.hover",
                  px: 1.5,
                  py: 0.75,
                  borderRadius: 1.5,
                  fontFamily: "monospace",
                  fontSize: 13,
                }}
              >
                {v || "—"}
              </Typography>
            </Box>
          ))}
        </Stack>
        <Divider sx={{ my: 2 }} />
        <Box sx={{ display: "flex", justifyContent: "flex-end" }}>
          <Button
            variant="contained"
            size="small"
            onClick={() => onConfirm(tool, args)}
            sx={{ borderRadius: 2 }}
          >
            确认提交
          </Button>
        </Box>
      </CardContent>
    </Card>
  );
}

export function AssistantContent({
  content,
  onConfirm,
}: {
  content: string;
  onConfirm: (tool: string, args: string) => void;
}) {
  if (content.startsWith("[CONFIRM_REQUIRED:"))
    return <ConfirmCard raw={content} onConfirm={onConfirm} />;
  return (
    <Typography
      component="pre"
      variant="body2"
      sx={{ whiteSpace: "pre-wrap", fontFamily: "inherit", m: 0 }}
    >
      {content}
    </Typography>
  );
}

export function formatTime(iso: string): string {
  try {
    const d = new Date(iso);
    return d.toLocaleString("zh-CN", {
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    });
  } catch {
    return "";
  }
}
