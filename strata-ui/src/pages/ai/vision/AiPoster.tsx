// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, Paper, Typography, IconButton } from "@mui/material";
import { Button, TextField } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { Image, Refresh, ZoomIn, ZoomOut, Fullscreen } from "@mui/icons-material";

export function AiPoster() {
  const [content, setContent] = useState("");

  return (
    <PageStack>
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: "1fr 380px",
          gap: 2,
          height: "calc(100vh - 140px)",
        }}
      >
        {/* Left editor */}
        <Paper sx={{ p: 2, display: "flex", flexDirection: "column" }}>
          <Box sx={{ display: "flex", alignItems: "center", gap: 2, mb: 2 }}>
            <Box sx={{ width: 48, height: 64, bgcolor: "#f5f5f5", borderRadius: 1 }} />
            <Box sx={{ flex: 1 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                红色党政风
              </Typography>
              <Typography variant="caption" sx={{ color: "text.secondary" }}>
                海报模板 / 可编辑内容 / 实时预览
              </Typography>
            </Box>
            <Button variant="text" size="small">
              更换模板
            </Button>
          </Box>

          <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 2, mb: 2 }}>
            <Box>
              <Typography variant="body2" sx={{ mb: 0.5 }}>
                * AI 模型
              </Typography>
              <TextField size="small" fullWidth placeholder="qwen3.7-flash-c" />
            </Box>
            <Box>
              <Typography variant="body2" sx={{ mb: 0.5 }}>
                二维码 (可选)
              </Typography>
              <Box
                sx={{
                  border: 2,
                  borderStyle: "dashed",
                  borderColor: "divider",
                  borderRadius: 1,
                  height: 60,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                }}
              >
                <Typography variant="caption" sx={{ color: "text.secondary" }}>
                  +
                </Typography>
              </Box>
            </Box>
          </Box>

          <TextField
            fullWidth
            multiline
            rows={10}
            placeholder="请输入海报内容，/ 调用 AI 编辑器"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            sx={{ mb: 2, flex: 1 }}
          />

          <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <Button variant="outlined" size="small">
              <Refresh sx={{ mr: 0.5 }} />
              重置内容
            </Button>
            <Typography variant="caption" sx={{ color: "text.secondary" }}>
              AI 生成约 10-20 秒，请耐心等待
            </Typography>
            <Button variant="contained" size="small">
              生成海报
            </Button>
          </Box>
        </Paper>

        {/* Right preview */}
        <Paper sx={{ p: 2, display: "flex", flexDirection: "column" }}>
          <Box
            sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 2 }}
          >
            <Typography variant="subtitle1">海报预览</Typography>
            <Box sx={{ display: "flex", gap: 0.5, alignItems: "center" }}>
              <IconButton size="small">
                <ZoomOut fontSize="small" />
              </IconButton>
              <Typography variant="caption">39%</Typography>
              <IconButton size="small">
                <ZoomIn fontSize="small" />
              </IconButton>
              <IconButton size="small">
                <Fullscreen fontSize="small" />
              </IconButton>
            </Box>
          </Box>
          <Box
            sx={{
              flex: 1,
              bgcolor: "#f5f5f5",
              borderRadius: 2,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            <Image sx={{ fontSize: 48, color: "text.disabled" }} />
          </Box>
          <Button fullWidth variant="outlined" size="small" sx={{ mt: 2 }}>
            导出海报
          </Button>
        </Paper>
      </Box>
    </PageStack>
  );
}

export default AiPoster;
