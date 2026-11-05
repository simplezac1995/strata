// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import {
  Box,
  Chip,
  CircularProgress,
  Paper,
  Stack,
  Step,
  StepLabel,
  Stepper,
  Typography,
} from "@mui/material";
import CheckCircleOutlineOutlinedIcon from "@mui/icons-material/CheckCircleOutlined";
import DownloadOutlinedIcon from "@mui/icons-material/DownloadOutlined";
import GppMaybeOutlinedIcon from "@mui/icons-material/GppMaybeOutlined";
import HelpOutlineOutlinedIcon from "@mui/icons-material/HelpOutlineOutlined";
import ReportProblemOutlinedIcon from "@mui/icons-material/ReportProblemOutlined";
import UploadFileOutlinedIcon from "@mui/icons-material/UploadFileOutlined";
import { Button, Select, TextArea } from "@/components/ui/index";
import { failureMessage } from "@/shared/hooks/useCrud";
import { aiToolApi } from "@/shared/api/ai";
import { toast } from "@/shared/pageKit/Toast";

const STEPS = ["上传内容", "配置检测", "分析结果"];

export function SafetyCheck() {
  const [step, setStep] = useState(0);
  const [content, setContent] = useState("");
  const [model, setModel] = useState("qwen3.7-flash-c");
  const [level, setLevel] = useState("low");
  const [result, setResult] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const passed = result?.toLowerCase().includes("pass") ?? false;

  async function runCheck() {
    const text = content.trim();
    if (!text) {
      toast.error("请先粘贴或上传待检测内容");
      return;
    }
    setBusy(true);
    setResult(null);
    try {
      const res = await aiToolApi.run("safety", `${text}\n[level=${level}]`);
      setResult(res.result);
      setStep(2);
    } catch (err) {
      toast.error(failureMessage(err));
    } finally {
      setBusy(false);
    }
  }

  const riskCount = (kw: string) => (result ? (result.toLowerCase().includes(kw) ? 1 : 0) : 0);
  const kpis = [
    { label: "总风险", value: result ? 1 : 0, color: "text.primary" },
    { label: "高风险", value: riskCount("high"), color: "error.main" },
    { label: "中风险", value: riskCount("medium") + riskCount("moderate"), color: "warning.main" },
    {
      label: "低风险",
      value: result && !riskCount("high") && !riskCount("medium") ? 1 : 0,
      color: "success.main",
    },
  ];

  return (
    <Box sx={{ p: 2 }}>
      {/* Header */}
      <Box
        sx={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", mb: 2 }}
      >
        <Box>
          <Typography variant="h6" sx={{ fontWeight: 700 }}>
            工作场所安全分析
          </Typography>
          <Typography variant="body2" sx={{ color: "text.secondary" }}>
            上传现场照片或粘贴文本，配置检测规则，快速定位安全隐患
          </Typography>
        </Box>
        <Button
          variant="outlined"
          color="default"
          size="small"
          startIcon={<DownloadOutlinedIcon />}
        >
          下载测试图片
        </Button>
      </Box>

      {/* Stepper */}
      <Paper variant="outlined" sx={{ p: 2, mb: 2, borderRadius: 3 }}>
        <Stepper activeStep={step}>
          {STEPS.map((label, i) => (
            <Step key={label} onClick={() => i < 2 && setStep(i)} sx={{ cursor: "pointer" }}>
              <StepLabel>{label}</StepLabel>
            </Step>
          ))}
        </Stepper>
      </Paper>

      <Box sx={{ display: "flex", gap: 2, alignItems: "flex-start" }}>
        {/* Left main */}
        <Paper variant="outlined" sx={{ flex: 1, p: 2, borderRadius: 3, minWidth: 0 }}>
          {/* Toolbar */}
          <Stack
            direction="row"
            sx={{ alignItems: "center", justifyContent: "space-between", mb: 2 }}
          >
            <Stack direction="row" spacing={1}>
              <Chip label="区域标记" size="small" color="primary" variant="outlined" />
              <Chip label="热力图" size="small" variant="outlined" />
            </Stack>
            {step === 2 ? (
              <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                <Typography variant="caption" sx={{ color: "text.secondary" }}>
                  AI 模型: {model} 安全级别:{" "}
                  {level === "high"
                    ? "高(严格检测)"
                    : level === "mid"
                      ? "中(标准检测)"
                      : "低(宽松检测)"}
                </Typography>
                <Button
                  size="small"
                  variant="outlined"
                  color="default"
                  onClick={() => {
                    setStep(0);
                    setResult(null);
                  }}
                >
                  重新上传
                </Button>
                <Button
                  size="small"
                  variant="contained"
                  onClick={() => void runCheck()}
                  disabled={busy}
                >
                  重新分析
                </Button>
              </Stack>
            ) : (
              <Typography variant="caption" sx={{ color: "text.secondary" }}>
                分析风险
              </Typography>
            )}
          </Stack>

          {/* Body */}
          {step < 2 ? (
            <Box
              sx={{
                border: 2,
                borderStyle: "dashed",
                borderColor: "divider",
                borderRadius: 3,
                p: 3,
                minHeight: 380,
                display: "flex",
                flexDirection: "column",
              }}
            >
              {step === 0 ? (
                <>
                  <TextArea
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    placeholder="点击浏览或拖拽内容到此处上传，或直接粘贴待检测文本…"
                    rows={10}
                    sx={{ flex: 1 }}
                  />
                  <Stack direction="row" spacing={1} sx={{ mt: 1.5 }}>
                    {["PNG", "JPG", "WEBP", "最大 5MB"].map((b) => (
                      <Chip key={b} label={b} size="small" variant="outlined" />
                    ))}
                  </Stack>
                  <Stack direction="row" sx={{ justifyContent: "flex-end", mt: 2 }}>
                    <Button
                      variant="contained"
                      disabled={!content.trim()}
                      onClick={() => setStep(1)}
                    >
                      下一步
                    </Button>
                  </Stack>
                </>
              ) : (
                <Box
                  sx={{
                    flex: 1,
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    justifyContent: "center",
                    gap: 1.5,
                    color: "text.disabled",
                  }}
                >
                  <UploadFileOutlinedIcon sx={{ fontSize: 48 }} />
                  <Typography variant="body2" sx={{ color: "text.secondary" }}>
                    已准备就绪，请在右侧配置检测参数后开始分析
                  </Typography>
                  <Stack direction="row" spacing={1} sx={{ mt: 2 }}>
                    <Button variant="outlined" color="default" onClick={() => setStep(0)}>
                      上一步
                    </Button>
                    <Button variant="contained" disabled={busy} onClick={() => void runCheck()}>
                      {busy ? "分析中..." : "开始分析"}
                    </Button>
                  </Stack>
                </Box>
              )}
            </Box>
          ) : (
            <Box sx={{ minHeight: 380 }}>
              {busy ? (
                <Stack
                  spacing={2}
                  sx={{ alignItems: "center", justifyContent: "center", height: 380 }}
                >
                  <CircularProgress />
                  <Typography variant="body2" sx={{ color: "text.secondary" }}>
                    正在分析图像...
                  </Typography>
                </Stack>
              ) : result ? (
                <Box sx={{ display: "flex", gap: 2 }}>
                  <Box
                    sx={{
                      flex: 1,
                      bgcolor: passed ? "success.light" : "warning.light",
                      borderRadius: 3,
                      p: 2,
                    }}
                  >
                    <Stack direction="row" sx={{ alignItems: "center", gap: 1, mb: 1 }}>
                      {passed ? (
                        <CheckCircleOutlineOutlinedIcon sx={{ color: "success.main" }} />
                      ) : (
                        <GppMaybeOutlinedIcon sx={{ color: "warning.main" }} />
                      )}
                      <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                        {passed ? "检测通过" : "发现风险项"}
                      </Typography>
                    </Stack>
                    <Typography variant="body2" sx={{ whiteSpace: "pre-wrap", lineHeight: 1.6 }}>
                      {result}
                    </Typography>
                  </Box>
                  <Paper variant="outlined" sx={{ p: 1.5, width: 140 }}>
                    <Typography variant="caption" sx={{ fontWeight: 700, display: "block", mb: 1 }}>
                      风险等级图例
                    </Typography>
                    {[
                      { c: "error.main", l: "高风险" },
                      { c: "warning.main", l: "中风险" },
                      { c: "success.main", l: "低风险" },
                    ].map((x) => (
                      <Stack
                        key={x.l}
                        direction="row"
                        spacing={1}
                        sx={{ alignItems: "center", mb: 0.5 }}
                      >
                        <Box sx={{ width: 10, height: 10, borderRadius: "50%", bgcolor: x.c }} />
                        <Typography variant="caption">{x.l}</Typography>
                      </Stack>
                    ))}
                  </Paper>
                </Box>
              ) : (
                <Typography
                  variant="body2"
                  sx={{ color: "text.secondary", mt: 10, textAlign: "center" }}
                >
                  暂无结果
                </Typography>
              )}
            </Box>
          )}
        </Paper>

        {/* Right column */}
        <Box sx={{ width: 320, display: "flex", flexDirection: "column", gap: 2 }}>
          {step === 2 ? (
            <Paper variant="outlined" sx={{ p: 2, borderRadius: 3 }}>
              <Box sx={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 1 }}>
                {kpis.map((k) => (
                  <Box key={k.label} sx={{ textAlign: "center" }}>
                    <Typography variant="caption" sx={{ color: "text.secondary" }}>
                      {k.label}
                    </Typography>
                    <Typography variant="h5" sx={{ fontWeight: 700, color: k.color }}>
                      {k.value}
                    </Typography>
                  </Box>
                ))}
              </Box>
            </Paper>
          ) : (
            <Paper variant="outlined" sx={{ p: 2, borderRadius: 3 }}>
              <Stack direction="row" spacing={1} sx={{ alignItems: "center", mb: 1.5 }}>
                <Box
                  sx={{
                    width: 24,
                    height: 24,
                    borderRadius: "50%",
                    bgcolor: "primary.main",
                    color: "#fff",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontSize: 13,
                  }}
                >
                  2
                </Box>
                <Box>
                  <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                    配置检测
                  </Typography>
                  <Typography variant="caption" sx={{ color: "text.secondary" }}>
                    选择识别模型和检测灵敏度
                  </Typography>
                </Box>
              </Stack>
              <Typography variant="body2" sx={{ mb: 0.5, fontWeight: 600 }}>
                AI 模型
              </Typography>
              <Select
                value={model}
                onChange={(e) => setModel(e.target.value)}
                options={[
                  { value: "qwen3.7-flash-c", label: "qwen3.7-flash-c" },
                  { value: "qwen3.7-plus", label: "Aliyun:qwen3.7-plus" },
                ]}
              />
              <Box sx={{ display: "flex", justifyContent: "space-between", mt: 2, mb: 0.5 }}>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  安全级别
                </Typography>
                <Typography variant="caption" sx={{ color: "text.secondary" }}>
                  检测灵敏度
                </Typography>
              </Box>
              <Select
                value={level}
                onChange={(e) => setLevel(e.target.value)}
                options={[
                  { value: "low", label: "低 (宽松检测)" },
                  { value: "mid", label: "中 (标准检测)" },
                  { value: "high", label: "高 (严格检测)" },
                ]}
              />
              <Typography
                variant="caption"
                sx={{ color: "text.disabled", display: "block", mt: 0.5 }}
              >
                级别越高，检测越严格
              </Typography>
            </Paper>
          )}

          <Paper variant="outlined" sx={{ p: 2, borderRadius: 3, flex: 1 }}>
            {step === 2 && result ? (
              <Box
                sx={{
                  display: "flex",
                  gap: 1.5,
                  bgcolor: "warning.light",
                  p: 1.5,
                  borderRadius: 2,
                }}
              >
                <ReportProblemOutlinedIcon sx={{ color: "warning.main" }} />
                <Box>
                  <Typography variant="body2" sx={{ fontWeight: 700 }}>
                    风险提示
                  </Typography>
                  <Typography variant="caption" sx={{ color: "text.secondary" }}>
                    {passed ? "未发现明显违规内容。" : "已标记疑似风险项，请人工复核。"}
                  </Typography>
                </Box>
              </Box>
            ) : (
              <Stack spacing={1} sx={{ alignItems: "center", justifyContent: "center", pt: 6 }}>
                <HelpOutlineOutlinedIcon sx={{ fontSize: 40, color: "text.disabled" }} />
                <Typography variant="body2" sx={{ fontWeight: 700 }}>
                  等待分析结果
                </Typography>
                <Typography variant="caption" sx={{ color: "text.secondary", textAlign: "center" }}>
                  先上传内容，再配置模型与检测级别
                </Typography>
              </Stack>
            )}
          </Paper>
        </Box>
      </Box>
    </Box>
  );
}

export default SafetyCheck;
