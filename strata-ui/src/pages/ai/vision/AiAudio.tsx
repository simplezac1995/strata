// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, Paper, Slider, Typography, IconButton } from "@mui/material";
import { TextField } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { Send, Headset } from "@mui/icons-material";

export function AiAudio() {
  const [speed, setSpeed] = useState(1);
  const [volume, setVolume] = useState(0);
  const [prompt, setPrompt] = useState("");

  return (
    <PageStack>
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: "280px 1fr 260px",
          gap: 2,
          height: "calc(100vh - 140px)",
        }}
      >
        <Paper sx={{ p: 2 }}>
          <Typography variant="subtitle1" sx={{ mb: 0.5 }}>
            语音配置
          </Typography>
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            语音引擎
          </Typography>
          <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
            模型
          </Typography>
          <TextField size="small" fullWidth placeholder="未配置默认模型" />
          <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
            倍速控制: {speed}x
          </Typography>
          <Slider
            value={speed}
            min={0.5}
            max={2}
            step={0.1}
            onChange={(_, v) => setSpeed(v as number)}
          />
          <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
            音量增益: {volume} dB
          </Typography>
          <Slider value={volume} min={-10} max={10} onChange={(_, v) => setVolume(v as number)} />
        </Paper>

        <Paper sx={{ p: 2, display: "flex", flexDirection: "column" }}>
          <Box
            sx={{
              flex: 1,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              flexDirection: "column",
            }}
          >
            <Headset sx={{ fontSize: 48, color: "text.disabled", mb: 1 }} />
            <Typography variant="body2" sx={{ color: "text.secondary" }}>
              波形可视化占位
            </Typography>
          </Box>
          <Paper
            elevation={2}
            sx={{ p: 1, display: "flex", alignItems: "center", borderRadius: 3 }}
          >
            <TextField
              fullWidth
              placeholder="描述你想要生成的音频..."
              variant="standard"
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              slotProps={{ input: { disableUnderline: true } }}
            />
            <IconButton color="primary">
              <Send />
            </IconButton>
          </Paper>
        </Paper>

        <Paper sx={{ p: 2, overflow: "auto" }}>
          <Typography variant="subtitle2" sx={{ mb: 1 }}>
            生成历史
          </Typography>
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            暂无历史
          </Typography>
        </Paper>
      </Box>
    </PageStack>
  );
}

export default AiAudio;
