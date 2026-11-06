// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import {
  Box,
  CircularProgress,
  Divider,
  IconButton,
  Paper,
  Stack,
  Typography,
  useTheme,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import AutoAwesomeIcon from "@mui/icons-material/AutoAwesome";
import CenterFocusStrongOutlinedIcon from "@mui/icons-material/CenterFocusStrongOutlined";
import DownloadIcon from "@mui/icons-material/Download";
import FormatBoldIcon from "@mui/icons-material/FormatBold";
import FormatItalicIcon from "@mui/icons-material/FormatItalic";
import FormatListBulletedIcon from "@mui/icons-material/FormatListBulleted";
import RedoIcon from "@mui/icons-material/Redo";
import RestartAltOutlinedIcon from "@mui/icons-material/RestartAltOutlined";
import UndoIcon from "@mui/icons-material/Undo";
import { Button, Select, TextArea } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { aiToolApi } from "@/shared/api/ai";
import { toast } from "@/shared/pageKit/Toast";
import { failureMessage } from "@/shared/hooks/useCrud";

const POSTER_TEMPLATES: { id: string; label: string; promptPrefix: string }[] = [
  {
    id: "corporate",
    label: "商务企业风",
    promptPrefix: "Professional corporate poster design, clean business style, ",
  },
  {
    id: "promo",
    label: "促销活动风",
    promptPrefix: "Eye-catching promotional poster, bold colors, sale theme, ",
  },
  { id: "event", label: "庆典活动风", promptPrefix: "Festive event poster, celebration mood, " },
  {
    id: "announcement",
    label: "公告通知风",
    promptPrefix: "Clean announcement poster, modern layout, ",
  },
  {
    id: "business",
    label: "商务简约风",
    promptPrefix: "Sleek business poster, professional typography, ",
  },
  {
    id: "minimal",
    label: "极简留白风",
    promptPrefix: "Minimalist poster, lots of whitespace, elegant, ",
  },
];

/**
 * 合成海报 PNG：AI 只负责底图（无文字），中文文案由前端用真实字体叠加绘制。
 */
function composePosterPng(imageUrl: string, text: string): Promise<Blob> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.onload = () => {
      try {
        const W = 1024;
        const H = 1365; // 3:4
        const canvas = document.createElement("canvas");
        canvas.width = W;
        canvas.height = H;
        const ctx = canvas.getContext("2d");
        if (!ctx) {
          reject(new Error("canvas 2d unavailable"));
          return;
        }
        const scale = Math.max(W / img.width, H / img.height);
        ctx.drawImage(
          img,
          (W - img.width * scale) / 2,
          (H - img.height * scale) / 2,
          img.width * scale,
          img.height * scale,
        );
        const grad = ctx.createLinearGradient(0, H * 0.55, 0, H);
        grad.addColorStop(0, "rgba(0,0,0,0)");
        grad.addColorStop(1, "rgba(0,0,0,0.72)");
        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, W, H);
        const fontFamily = "'PingFang SC','Microsoft YaHei','Noto Sans SC',sans-serif";
        const margin = 72;
        const maxWidth = W - margin * 2;
        const lines: string[] = [];
        for (const raw of text.split("\n")) {
          if (!raw.trim()) {
            lines.push("");
            continue;
          }
          ctx.font = `700 56px ${fontFamily}`;
          let current = "";
          for (const ch of raw) {
            const trial = current + ch;
            if (ctx.measureText(trial).width > maxWidth && current) {
              lines.push(current);
              current = ch;
            } else {
              current = trial;
            }
          }
          lines.push(current);
        }
        const visible = lines.slice(0, 6);
        const fontSize = visible.length > 4 ? 40 : 56;
        const lineHeight = fontSize * 1.35;
        ctx.font = `700 ${fontSize}px ${fontFamily}`;
        ctx.textBaseline = "top";
        ctx.textAlign = "left";
        ctx.shadowColor = "rgba(0,0,0,0.85)";
        ctx.shadowBlur = 18;
        ctx.fillStyle = "#ffffff";
        let y = H - margin - visible.length * lineHeight;
        for (const line of visible) {
          ctx.fillText(line, margin, y);
          y += lineHeight;
        }
        ctx.shadowBlur = 0;
        canvas.toBlob(
          (blob) => (blob ? resolve(blob) : reject(new Error("toBlob failed"))),
          "image/png",
        );
      } catch (e) {
        reject(e);
      }
    };
    img.onerror = () => reject(new Error("image load failed"));
    img.src = imageUrl;
  });
}

export function Poster() {
  const theme = useTheme();
  const [templateId, setTemplateId] = useState("corporate");
  const [templatePickerOpen, setTemplatePickerOpen] = useState(false);
  const [content, setContent] = useState("");
  const [busy, setBusy] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [imageUrl, setImageUrl] = useState<string | null>(null);

  const template = POSTER_TEMPLATES.find((x) => x.id === templateId)!;
  const charCount = content.length;

  async function generate() {
    const prompt = `${template.promptPrefix} NO text, NO words, NO letters, NO characters, clean empty space for title`;
    setBusy(true);
    setImageUrl(null);
    try {
      const res = await aiToolApi.run("image", `${prompt}\n[ratio=3:4]`);
      const urls = res.result.split("\n").filter((u: string) => u.startsWith("http"));
      if (urls.length > 0) setImageUrl(urls[0]);
      else toast.error("No image returned");
    } catch (err) {
      toast.error(failureMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function download() {
    if (!imageUrl || !content.trim() || downloading) return;
    setDownloading(true);
    try {
      const blob = await composePosterPng(imageUrl, content);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `strata-poster-${Date.now()}.png`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      toast.error(failureMessage(err));
    } finally {
      setDownloading(false);
    }
  }

  return (
    <PageStack>
      <Box
        sx={{ display: "flex", gap: 2, alignItems: "stretch", minHeight: "calc(100vh - 160px)" }}
      >
        {/* Left: editor */}
        <Paper
          variant="outlined"
          sx={{
            flex: 1,
            p: 2.5,
            borderRadius: 3,
            display: "flex",
            flexDirection: "column",
            minWidth: 0,
          }}
        >
          {/* Template header */}
          <Box
            sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mb: 2 }}
          >
            <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
              <Box sx={{ width: 44, height: 56, borderRadius: 1, bgcolor: "action.hover" }} />
              <Box>
                <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                  {template.label}{" "}
                  <AutoAwesomeIcon
                    sx={{ fontSize: 16, color: "warning.main", verticalAlign: "middle" }}
                  />
                </Typography>
                <Typography variant="caption" sx={{ color: "text.secondary" }}>
                  海报模板 / 可编辑内容 / 实时预览
                </Typography>
              </Box>
            </Stack>
            <Typography
              variant="body2"
              sx={{ color: "primary.main", cursor: "pointer" }}
              onClick={() => setTemplatePickerOpen((v) => !v)}
            >
              更换模板 ›
            </Typography>
          </Box>

          {templatePickerOpen && (
            <Box sx={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 1, mb: 2 }}>
              {POSTER_TEMPLATES.map((tp) => (
                <Button
                  key={tp.id}
                  variant={tp.id === templateId ? "contained" : "outlined"}
                  onClick={() => setTemplateId(tp.id)}
                  sx={{ py: 1, borderRadius: 2, textTransform: "none" }}
                >
                  {tp.label}
                </Button>
              ))}
            </Box>
          )}

          {/* Model + QR */}
          <Box sx={{ display: "flex", gap: 2, mb: 2 }}>
            <Box sx={{ flex: 1 }}>
              <Typography variant="body2" sx={{ mb: 0.5, fontWeight: 600 }}>
                * AI 模型
              </Typography>
              <Select
                value="qwen3.7-flash-c"
                onChange={() => {}}
                options={[
                  { value: "qwen3.7-flash-c", label: "qwen3.7-flash-c" },
                  { value: "qwen3.7-plus", label: "Aliyun:qwen3.7-plus" },
                ]}
              />
            </Box>
            <Box sx={{ flex: 1 }}>
              <Typography variant="body2" sx={{ mb: 0.5, fontWeight: 600 }}>
                二维码（可选）
              </Typography>
              <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
                <Box
                  sx={{
                    width: 72,
                    height: 72,
                    border: 1,
                    borderStyle: "dashed",
                    borderColor: "divider",
                    borderRadius: 2,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  <AddIcon sx={{ color: "text.disabled" }} />
                </Box>
                <Box>
                  <Typography variant="caption" sx={{ color: "text.secondary", display: "block" }}>
                    支持 JPG / PNG 格式
                  </Typography>
                  <Typography variant="caption" sx={{ color: "text.secondary", display: "block" }}>
                    建议尺寸：300 × 300 px
                  </Typography>
                  <Typography variant="caption" sx={{ color: "text.secondary", display: "block" }}>
                    文件不超过 2MB
                  </Typography>
                </Box>
              </Stack>
            </Box>
          </Box>

          {/* Toolbar (decorative) */}
          <Paper
            variant="outlined"
            sx={{ display: "flex", gap: 0.5, p: 0.8, mb: 1.5, flexWrap: "wrap" }}
          >
            {[UndoIcon, RedoIcon, FormatBoldIcon, FormatItalicIcon, FormatListBulletedIcon].map(
              (Icon, i) => (
                <IconButton key={i} size="small">
                  <Icon sx={{ fontSize: 17 }} />
                </IconButton>
              ),
            )}
            <Divider orientation="vertical" flexItem sx={{ mx: 0.5 }} />
            <IconButton size="small">
              <FormatBoldIcon sx={{ fontSize: 17 }} />
            </IconButton>
          </Paper>

          {/* Content editor */}
          <Box sx={{ flex: 1, minHeight: 200 }}>
            <TextArea
              placeholder="请输入海报内容，/ 调用 AI 编辑器"
              rows={8}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              disabled={busy}
            />
          </Box>
          <Typography variant="caption" sx={{ color: "text.secondary", mt: 0.5 }}>
            {charCount} 字符
          </Typography>

          {/* Footer */}
          <Stack
            direction="row"
            spacing={1}
            sx={{ mt: 2, alignItems: "center", justifyContent: "space-between" }}
          >
            <Button
              variant="outlined"
              color="default"
              startIcon={<RestartAltOutlinedIcon />}
              onClick={() => setContent("")}
            >
              重置内容
            </Button>
            <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
              <Typography variant="caption" sx={{ color: "text.secondary" }}>
                AI 生成约 10-20 秒，请耐心等待
              </Typography>
              <Button variant="contained" disabled={busy} onClick={() => void generate()}>
                {busy ? (
                  <CircularProgress size={18} sx={{ mr: 0.5 }} />
                ) : (
                  <AutoAwesomeIcon sx={{ mr: 0.5 }} />
                )}
                生成海报
              </Button>
            </Stack>
          </Stack>
        </Paper>

        {/* Right: preview */}
        <Paper
          variant="outlined"
          sx={{ width: 400, p: 2, borderRadius: 3, display: "flex", flexDirection: "column" }}
        >
          <Stack
            direction="row"
            sx={{ alignItems: "center", justifyContent: "space-between", mb: 1.5 }}
          >
            <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
              海报预览
            </Typography>
            <Stack direction="row" spacing={0.5} sx={{ alignItems: "center" }}>
              <IconButton size="small">−</IconButton>
              <Typography variant="caption">100%</Typography>
              <IconButton size="small">+</IconButton>
              <Button
                size="small"
                variant="outlined"
                color="default"
                startIcon={<CenterFocusStrongOutlinedIcon sx={{ fontSize: 16 }} />}
              >
                适应窗口
              </Button>
            </Stack>
          </Stack>

          <Box
            sx={{
              flex: 1,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              p: 1,
              minHeight: 320,
            }}
          >
            {imageUrl ? (
              <Box
                sx={{
                  position: "relative",
                  width: "100%",
                  maxWidth: 300,
                  aspectRatio: "3 / 4",
                  borderRadius: 2,
                  overflow: "hidden",
                  boxShadow: theme.shadows[4],
                }}
              >
                <img
                  src={imageUrl}
                  alt="poster"
                  crossOrigin="anonymous"
                  style={{
                    position: "absolute",
                    inset: 0,
                    width: "100%",
                    height: "100%",
                    objectFit: "cover",
                  }}
                />
                <Box
                  sx={{
                    position: "absolute",
                    inset: 0,
                    background: "linear-gradient(180deg, rgba(0,0,0,0) 55%, rgba(0,0,0,0.72) 100%)",
                  }}
                />
                <Box sx={{ position: "absolute", left: 0, right: 0, bottom: 20, px: 3 }}>
                  <Typography
                    variant="h6"
                    sx={{
                      color: "#fff",
                      fontWeight: 700,
                      fontSize: 18,
                      lineHeight: 1.45,
                      textShadow: "0 2px 12px rgba(0,0,0,0.85)",
                      whiteSpace: "pre-line",
                    }}
                  >
                    {content}
                  </Typography>
                </Box>
              </Box>
            ) : (
              <Typography variant="body2" sx={{ color: "text.secondary" }}>
                填写内容并生成后在此预览
              </Typography>
            )}
          </Box>

          <Stack
            direction="row"
            sx={{
              alignItems: "center",
              justifyContent: "space-between",
              pt: 1.5,
              borderTop: 1,
              borderColor: "divider",
            }}
          >
            <Typography variant="caption" sx={{ color: "text.secondary" }}>
              {imageUrl ? "生成完成后可导出海报" : "等待生成..."}
            </Typography>
            <Button
              size="small"
              variant="outlined"
              color="default"
              startIcon={<DownloadIcon sx={{ fontSize: 16 }} />}
              disabled={!imageUrl || downloading}
              onClick={() => void download()}
            >
              {downloading ? <CircularProgress size={16} sx={{ mr: 0.5 }} /> : null}
              导出海报
            </Button>
          </Stack>
        </Paper>
      </Box>
    </PageStack>
  );
}

export default Poster;
