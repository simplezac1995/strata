// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { useI18n } from "@/shared/i18n";
import { Box, Paper, Slider, Typography, IconButton } from "@mui/material";
import { Button, TextField } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { Send, CloudUpload } from "@mui/icons-material";

const SIZES = ["1:1", "3:4", "4:3", "16:9", "9:16", "2:3", "3:2", "7:3"];

export function AiImageGen() {
  const { t } = useI18n();
  const [size, setSize] = useState("1:1");
  const [count, setCount] = useState(1);
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
        {/* Left: params */}
        <Paper sx={{ p: 2, overflow: "auto" }}>
          <Typography variant="subtitle2" sx={{ mb: 1 }}>
            模型
          </Typography>
          <TextField size="small" fullWidth placeholder={t("ai.image.modelPlaceholder")} />
          <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
            图片尺寸
          </Typography>
          <Box sx={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 1 }}>
            {SIZES.map((s) => (
              <Box
                key={s}
                onClick={() => setSize(s)}
                sx={{
                  border: 1,
                  borderColor: size === s ? "primary.main" : "divider",
                  borderRadius: 1,
                  p: 1,
                  textAlign: "center",
                  cursor: "pointer",
                }}
              >
                <Typography variant="caption">{s}</Typography>
              </Box>
            ))}
          </Box>
          <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
            图片数量: {count}
          </Typography>
          <Slider value={count} min={1} max={4} onChange={(_, v) => setCount(v as number)} />
          <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
            参考图片 (可选)
          </Typography>
          <Button fullWidth variant="outlined" size="small">
            <CloudUpload sx={{ mr: 0.5 }} />
            选择图片
          </Button>
        </Paper>

        {/* Center: canvas + prompt */}
        <Paper sx={{ p: 2, display: "flex", flexDirection: "column" }}>
          <Box
            sx={{
              flex: 1,
              bgcolor: "#f5f5f5",
              borderRadius: 2,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              mb: 2,
            }}
          >
            <Typography variant="body2" sx={{ color: "text.secondary" }}>
              图片将在这里显示
            </Typography>
          </Box>
          <Paper
            elevation={2}
            sx={{ p: 1, display: "flex", alignItems: "center", borderRadius: 3 }}
          >
            <TextField
              fullWidth
              placeholder="描述你想要生成的图片..."
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

        {/* Right: history */}
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

export default AiImageGen;
