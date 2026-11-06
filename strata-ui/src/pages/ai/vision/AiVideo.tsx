// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, Paper, Typography, IconButton } from "@mui/material";
import { TextField } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { Send, VideoLibrary } from "@mui/icons-material";

const RATIOS = ["16:9", "9:16", "1:1"];

export function AiVideo() {
  const [ratio, setRatio] = useState("16:9");
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
        <Paper sx={{ p: 2, overflow: "auto" }}>
          <Typography variant="subtitle1" sx={{ mb: 0.5 }}>
            视频配置
          </Typography>
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            视频引擎
          </Typography>
          <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
            模型选择
          </Typography>
          <TextField size="small" fullWidth placeholder="请选择模型" />
          <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
            视频比例
          </Typography>
          <Box sx={{ display: "flex", gap: 1 }}>
            {RATIOS.map((r) => (
              <Box
                key={r}
                onClick={() => setRatio(r)}
                sx={{
                  border: 1,
                  borderColor: ratio === r ? "primary.main" : "divider",
                  borderRadius: 1,
                  p: 1,
                  textAlign: "center",
                  cursor: "pointer",
                  flex: 1,
                }}
              >
                <Typography variant="caption">{r}</Typography>
              </Box>
            ))}
          </Box>
          <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
            生成种子
          </Typography>
          <TextField size="small" fullWidth placeholder="89493" />
          <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
            反向提示词
          </Typography>
          <TextField
            size="small"
            fullWidth
            multiline
            rows={4}
            placeholder="色调艳丽,过曝,静态,细节模糊不清..."
          />
        </Paper>

        <Paper sx={{ p: 2, display: "flex", flexDirection: "column" }}>
          <Typography variant="subtitle1">视频工作台</Typography>
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            使用先进的 AI 编排器创建高保真视频内容
          </Typography>
          <Box
            sx={{
              flex: 1,
              bgcolor: "#f5f5f5",
              borderRadius: 2,
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              justifyContent: "center",
              my: 2,
            }}
          >
            <VideoLibrary sx={{ fontSize: 48, color: "text.disabled", mb: 1 }} />
            <Typography variant="body1">画布就绪</Typography>
            <Typography variant="caption" sx={{ color: "text.secondary" }}>
              在下方输入框中描述您的创意，开始生成视频
            </Typography>
          </Box>
          <Paper
            elevation={2}
            sx={{ p: 1, display: "flex", alignItems: "center", borderRadius: 3 }}
          >
            <TextField
              fullWidth
              placeholder="描述你想要生成的视频..."
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

export default AiVideo;
