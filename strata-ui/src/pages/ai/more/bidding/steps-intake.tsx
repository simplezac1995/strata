// SPDX-License-Identifier: Apache-2.0
// Steps 0-1: pick the tender file, then run the AI interpretation.

import { useState } from "react";
import { Box, Chip as MuiChip, CircularProgress, Paper, Typography } from "@mui/material";
import AutoAwesomeIcon from "@mui/icons-material/AutoAwesome";
import DescriptionIcon from "@mui/icons-material/Description";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import { Button, Chip } from "@/components/ui/index";
import { TENDER_FILE, TENDER_TEXT, INTERPRET_TEXT } from "./content";
import { Caption } from "./shared";

export function TenderFileStep({ onNext }: { onNext: () => void }) {
  const [fileTab, setFileTab] = useState<"main" | "extra">("main");
  return (
    <Box>
      <Paper
        variant="outlined"
        sx={{ p: 2, display: "flex", alignItems: "center", gap: 1.5, mb: 2 }}
      >
        <DescriptionIcon sx={{ color: "primary.main" }} />
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography variant="body2" sx={{ fontWeight: 600 }}>
            {TENDER_FILE}
          </Typography>
          <Caption>解析成功 · 1,595 字符</Caption>
        </Box>
        <Chip label="主招标文件" color="success" variant="outlined" sx={{ height: 22 }} />
        <MuiChip
          label="招标文件"
          size="small"
          onClick={() => setFileTab("main")}
          sx={{
            bgcolor: fileTab === "main" ? "primary.main" : "transparent",
            color: fileTab === "main" ? "#fff" : "text.secondary",
            cursor: "pointer",
          }}
        />
        <MuiChip
          label="相关资料"
          size="small"
          onClick={() => setFileTab("extra")}
          sx={{ cursor: "pointer" }}
        />
        <Button variant="contained" size="small" startIcon={<AutoAwesomeIcon />} onClick={onNext}>
          开始 AI 解读
        </Button>
      </Paper>

      <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 1 }}>
        解析正文
      </Typography>
      <Paper
        variant="outlined"
        sx={{ p: 3, bgcolor: "grey.50", maxHeight: 460, overflowY: "auto" }}
      >
        <Typography
          component="pre"
          sx={{
            whiteSpace: "pre-wrap",
            fontFamily: "inherit",
            m: 0,
            fontSize: 14,
            lineHeight: 1.8,
          }}
        >
          {TENDER_TEXT}
        </Typography>
      </Paper>
    </Box>
  );
}

/* ============================== 步骤 1：AI 解读 ============================== */
export function InterpretStep({
  state,
  onRun,
}: {
  state: "idle" | "running" | "done";
  onRun: () => void;
}) {
  return (
    <Box>
      <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mb: 2 }}>
        <Chip
          label={state === "done" ? "已完成解读" : "待解读"}
          color={state === "done" ? "success" : "primary"}
          variant="outlined"
          sx={{ height: 22 }}
        />
        <Caption sx={{ flex: 1 }}>{TENDER_FILE}</Caption>
        <Button
          variant="contained"
          size="small"
          startIcon={<AutoAwesomeIcon />}
          onClick={onRun}
          disabled={state === "running"}
        >
          {state === "running" ? "解读中…" : state === "done" ? "重新解读" : "开始 AI 解读"}
        </Button>
      </Box>

      {state === "running" ? (
        <Box sx={{ py: 10, textAlign: "center" }}>
          <CircularProgress sx={{ mb: 2 }} />
          <Typography variant="body2" sx={{ color: "text.secondary" }}>
            AI 正在解读招标文件…
          </Typography>
        </Box>
      ) : state === "done" ? (
        <Box sx={{ borderLeft: 3, borderColor: "primary.main", pl: 2.5 }}>
          {INTERPRET_TEXT.map((sec) => (
            <Box key={sec.title} sx={{ mb: 2.5 }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 0.5 }}>
                {sec.title}
              </Typography>
              {sec.lines.map((l, i) => (
                <Typography key={i} variant="body2" sx={{ lineHeight: 1.9 }}>
                  {l.startsWith("-") ? l : `- ${l}`}
                </Typography>
              ))}
            </Box>
          ))}
        </Box>
      ) : (
        <Box sx={{ py: 8, textAlign: "center" }}>
          <DescriptionIcon sx={{ fontSize: 48, color: "text.disabled", mb: 1 }} />
          <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
            等待解读
          </Typography>
          <Caption>AI 解读完成后，这里会显示关键事实与响应建议。</Caption>
        </Box>
      )}

      <Box
        sx={{
          display: "flex",
          alignItems: "center",
          mt: 3,
          pt: 2,
          borderTop: 1,
          borderColor: "divider",
        }}
      >
        <Typography variant="body2" sx={{ color: "text.secondary" }}>
          查看解析原文（只读）
        </Typography>
        <Box sx={{ flex: 1 }} />
        <ExpandMoreIcon sx={{ color: "text.secondary" }} />
      </Box>
    </Box>
  );
}

/* ============================== 步骤 2：需求与评分 ============================== */
