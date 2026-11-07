// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, Paper, Step, StepLabel, Stepper, Typography, Chip } from "@mui/material";
import { Button, Select, TextField } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { CloudUpload, Download, Search } from "@mui/icons-material";

const STEPS = ["上传图片", "配置检测", "分析结果"];

export function AiSecurity() {
  const [activeStep, setActiveStep] = useState(0);

  return (
    <PageStack>
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 2 }}>
        <Box>
          <Typography variant="h6">工作场所安全分析</Typography>
          <Typography variant="body2" sx={{ color: "text.secondary" }}>
            上传现场照片，配置检测规则，快速定位安全隐患
          </Typography>
        </Box>
        <Button variant="outlined" size="small">
          <Download sx={{ mr: 0.5 }} />
          下载测试图片
        </Button>
      </Box>

      <Stepper activeStep={activeStep} sx={{ mb: 3 }}>
        {STEPS.map((s) => (
          <Step key={s}>
            <StepLabel>{s}</StepLabel>
          </Step>
        ))}
      </Stepper>

      <Box sx={{ display: "grid", gridTemplateColumns: "1fr 340px", gap: 2 }}>
        {/* Left: upload/result area */}
        <Paper sx={{ p: 3, minHeight: 400 }}>
          {activeStep === 0 && (
            <Box
              sx={{
                border: 2,
                borderStyle: "dashed",
                borderColor: "divider",
                borderRadius: 2,
                p: 6,
                textAlign: "center",
              }}
            >
              <CloudUpload sx={{ fontSize: 48, color: "primary.main", mb: 1 }} />
              <Typography variant="body1">上传工作场所照片</Typography>
              <Typography variant="caption" sx={{ color: "text.secondary" }}>
                点击浏览或拖拽文件到此处上传
              </Typography>
              <Box sx={{ mt: 2, display: "flex", gap: 1, justifyContent: "center" }}>
                <Chip label="PNG" size="small" variant="outlined" />
                <Chip label="JPG" size="small" variant="outlined" />
                <Chip label="WEBP" size="small" variant="outlined" />
                <Chip label="最大 5MB" size="small" variant="outlined" />
              </Box>
            </Box>
          )}
          {activeStep === 1 && (
            <Typography variant="body2" sx={{ color: "text.secondary" }}>
              图片已上传，配置右侧参数后点击"分析风险"
            </Typography>
          )}
          {activeStep === 2 && (
            <Box>
              <Box sx={{ display: "flex", gap: 2, mb: 2 }}>
                <Chip label="总风险: 1" color="warning" />
                <Chip label="高风险: 0" color="error" />
                <Chip label="中风险: 0" color="warning" />
                <Chip label="低风险: 1" color="success" />
              </Box>
              <Paper sx={{ p: 2, bgcolor: "#fff8e1" }}>
                <Typography variant="body2">非目标图片</Typography>
                <Typography variant="caption" sx={{ color: "text.secondary" }}>
                  图像显示一名身穿深蓝色T恤的男性在蓝色背景前的半身肖像照，并非施工现场。
                </Typography>
              </Paper>
            </Box>
          )}
        </Paper>

        {/* Right: config */}
        <Paper sx={{ p: 2 }}>
          <Typography variant="subtitle1" sx={{ mb: 2 }}>
            配置检测
          </Typography>
          <Typography variant="body2" sx={{ mb: 1 }}>
            AI 模型
          </Typography>
          <TextField size="small" fullWidth placeholder="qwen3.7-flash-c" sx={{ mb: 2 }} />
          <Typography variant="body2" sx={{ mb: 1 }}>
            安全级别
          </Typography>
          <Select
            value="低 (宽松检测)"
            onChange={() => {}}
            sx={{ mb: 1 }}
            options={[
              { value: "低 (宽松检测)", label: "低 (宽松检测)" },
              { value: "中 (标准检测)", label: "中 (标准检测)" },
              { value: "高 (严格检测)", label: "高 (严格检测)" },
            ]}
          />
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            级别越高，检测越严格
          </Typography>
          <Button
            fullWidth
            variant="contained"
            size="small"
            sx={{ mt: 2 }}
            onClick={() => setActiveStep(2)}
          >
            <Search sx={{ mr: 0.5 }} />
            分析风险
          </Button>
        </Paper>
      </Box>
    </PageStack>
  );
}

export default AiSecurity;
