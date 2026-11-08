// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import {
  Box,
  Button as MuiButton,
  CircularProgress,
  Collapse,
  IconButton,
  Paper,
  Slider,
  Stack,
  Typography,
} from "@mui/material";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutlined";
import DownloadIcon from "@mui/icons-material/Download";
import EastIcon from "@mui/icons-material/East";
import ImageOutlinedIcon from "@mui/icons-material/ImageOutlined";
import RefreshIcon from "@mui/icons-material/Refresh";
import SendIcon from "@mui/icons-material/Send";
import { Select, TextField } from "@/components/ui/index";
import { failureMessage } from "@/shared/hooks/useCrud";
import { aiToolApi, modelApi } from "@/shared/api/ai";
import type { AiModelDto } from "@/shared/types";
import { toast } from "@/shared/pageKit/Toast";

const ASPECT_RATIOS = ["1:1", "3:4", "4:3", "16:9", "9:16", "2:3", "3:2", "7:3"];
const HISTORY_KEY = "ai_image_history";
type HistoryItem = { prompt: string; urls: string[]; time: string };

const EXAMPLES = [
  "赛博朋克城市夜景，霓虹灯，雨景",
  "一只穿宇航服的猫在月球上，卡通风格",
  "中国风山水水墨画，远山如黛，云雾缭绕",
];

function randomSeed() {
  return String(Math.floor(1000000000 + Math.random() * 9000000000));
}

export function ImageGeneration() {
  const [prompt, setPrompt] = useState("");
  const [ratio, setRatio] = useState("1:1");
  const [count, setCount] = useState(1);
  const [seed, setSeed] = useState(randomSeed());
  const [steps, setSteps] = useState(25);
  const [guidance, setGuidance] = useState(7.5);
  const [negative, setNegative] = useState("");
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const [models, setModels] = useState<AiModelDto[]>([]);
  const [modelCode, setModelCode] = useState("");
  const [result, setResult] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [history, setHistory] = useState<HistoryItem[]>([]);

  useEffect(() => {
    try {
      const s = localStorage.getItem(HISTORY_KEY);
      if (s) setHistory(JSON.parse(s));
    } catch {}
  }, []);
  useEffect(() => {
    modelApi
      .list()
      .then((all) => {
        const imgs = all.filter((m) => m.enabled && m.type === "image");
        setModels(imgs);
        if (imgs.length > 0) setModelCode(imgs[0].code);
      })
      .catch(() => {});
  }, []);

  function saveHistory(item: HistoryItem) {
    const next = [item, ...history].slice(0, 20);
    setHistory(next);
    localStorage.setItem(HISTORY_KEY, JSON.stringify(next));
  }

  async function generate() {
    const content = prompt.trim();
    if (!content) return;
    setBusy(true);
    setResult(null);
    try {
      const extra = [`ratio=${ratio}`, `count=${count}`, `seed=${seed}`, `steps=${steps}`];
      if (negative.trim()) extra.push(`negative=${negative.trim()}`);
      const res = await aiToolApi.run("image", `${content}\n[${extra.join(", ")}]`);
      setResult(res.result);
      const urls = res.result.split("\n").filter((u: string) => u.startsWith("http"));
      if (urls.length > 0)
        saveHistory({ prompt: content, urls, time: new Date().toLocaleString() });
    } catch (err) {
      toast.error(failureMessage(err));
    } finally {
      setBusy(false);
    }
  }

  const urls = result ? result.split("\n").filter((u) => u.startsWith("http")) : [];

  return (
    <Box sx={{ display: "flex", gap: 2, height: "calc(100vh - 140px)", p: 2 }}>
      {/* Left: config */}
      <Paper
        variant="outlined"
        sx={{
          width: 300,
          p: 2.5,
          borderRadius: 3,
          display: "flex",
          flexDirection: "column",
          gap: 2.5,
          overflowY: "auto",
        }}
      >
        <Box>
          <Typography variant="body2" sx={{ mb: 1, fontWeight: 600 }}>
            模型
          </Typography>
          <Select
            value={modelCode}
            onChange={(e) => setModelCode(e.target.value)}
            displayEmpty
            options={
              models.length > 0
                ? models.map((m) => ({ value: m.code, label: m.name }))
                : [{ value: "", label: "请选择模型" }]
            }
          />
        </Box>

        <Box>
          <Typography variant="body2" sx={{ mb: 1, fontWeight: 600 }}>
            图片尺寸
          </Typography>
          <Box sx={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 1 }}>
            {ASPECT_RATIOS.map((r) => (
              <MuiButton
                key={r}
                variant={ratio === r ? "contained" : "outlined"}
                onClick={() => setRatio(r)}
                sx={{
                  py: 1,
                  borderRadius: 2,
                  borderStyle: ratio === r ? "solid" : "dashed",
                  fontSize: 12,
                }}
              >
                {r}
              </MuiButton>
            ))}
          </Box>
        </Box>

        <Box>
          <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              图片数量
            </Typography>
            <Typography variant="body2" sx={{ color: "primary.main", fontWeight: 700 }}>
              {count}
            </Typography>
          </Box>
          <Slider
            min={1}
            max={4}
            step={1}
            value={count}
            onChange={(_, v) => setCount(v as number)}
          />
        </Box>

        <Box>
          <Box
            sx={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              cursor: "pointer",
            }}
            onClick={() => setAdvancedOpen((v) => !v)}
          >
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              高级参数
            </Typography>
            <ChevronRightIcon
              sx={{
                transition: "transform .2s",
                transform: advancedOpen ? "rotate(90deg)" : "none",
              }}
            />
          </Box>
          <Collapse in={advancedOpen}>
            <Stack spacing={2} sx={{ mt: 2 }}>
              <Box>
                <Typography variant="caption" sx={{ display: "block", mb: 0.5 }}>
                  种子
                </Typography>
                <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                  <TextField
                    size="small"
                    fullWidth
                    value={seed}
                    onChange={(e) => setSeed(e.target.value)}
                  />
                  <IconButton onClick={() => setSeed(randomSeed())}>
                    <RefreshIcon fontSize="small" />
                  </IconButton>
                </Stack>
              </Box>
              <Box>
                <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                  <Typography variant="caption">推理步数</Typography>
                  <Typography variant="caption" sx={{ color: "primary.main", fontWeight: 700 }}>
                    {steps}
                  </Typography>
                </Box>
                <Slider
                  min={10}
                  max={50}
                  step={1}
                  value={steps}
                  onChange={(_, v) => setSteps(v as number)}
                />
              </Box>
              <Box>
                <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                  <Typography variant="caption">引导强度</Typography>
                  <Typography variant="caption" sx={{ color: "primary.main", fontWeight: 700 }}>
                    {guidance}
                  </Typography>
                </Box>
                <Slider
                  min={1}
                  max={15}
                  step={0.5}
                  value={guidance}
                  onChange={(_, v) => setGuidance(v as number)}
                />
              </Box>
              <TextField
                multiline
                minRows={3}
                fullWidth
                size="small"
                label="反向提示词"
                placeholder="不想在图片中看到的内容"
                value={negative}
                onChange={(e) => setNegative(e.target.value)}
              />
            </Stack>
          </Collapse>
        </Box>

        <Box sx={{ borderTop: 1, borderColor: "divider", pt: 2 }}>
          <Typography variant="body2" sx={{ mb: 1, fontWeight: 600 }}>
            参考图片（可选）
          </Typography>
          <Box
            sx={{
              border: 1,
              borderStyle: "dashed",
              borderColor: "divider",
              borderRadius: 2,
              p: 2,
              textAlign: "center",
            }}
          >
            <MuiButton variant="contained" size="small" startIcon={<ImageOutlinedIcon />}>
              选择图片
            </MuiButton>
            <Typography variant="caption" sx={{ color: "text.disabled", display: "block", mt: 1 }}>
              支持 PNG、JPG、WEBP、GIF 等格式
            </Typography>
          </Box>
        </Box>
      </Paper>

      {/* Center: canvas */}
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
        <Box
          sx={{
            flex: 1,
            bgcolor: "action.hover",
            borderRadius: 2.5,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            overflow: "auto",
            minHeight: 300,
            p: 1,
          }}
        >
          {busy ? (
            <CircularProgress />
          ) : result ? (
            urls.length > 0 ? (
              <Box
                sx={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr",
                  gap: 1,
                  width: "100%",
                }}
              >
                {urls.map((u, i) => (
                  <Box key={i} sx={{ position: "relative" }}>
                    <img src={u} alt={`gen-${i}`} style={{ width: "100%", borderRadius: 8 }} />
                    <IconButton
                      href={u}
                      download
                      component="a"
                      size="small"
                      sx={{ position: "absolute", top: 8, right: 8, bgcolor: "background.paper" }}
                    >
                      <DownloadIcon fontSize="small" />
                    </IconButton>
                  </Box>
                ))}
              </Box>
            ) : result.startsWith("data:image") ? (
              <img src={result} alt="gen" style={{ maxWidth: "100%", borderRadius: 8 }} />
            ) : (
              <Typography variant="body2" sx={{ whiteSpace: "pre-wrap" }}>
                {result}
              </Typography>
            )
          ) : (
            <Stack spacing={1} sx={{ alignItems: "center", color: "text.disabled" }}>
              <ImageOutlinedIcon sx={{ fontSize: 48 }} />
              <Typography variant="body2" sx={{ color: "text.secondary" }}>
                图片将在这里显示
              </Typography>
              <Typography variant="caption" sx={{ color: "text.disabled" }}>
                输入提示词并点击生成按钮
              </Typography>
            </Stack>
          )}
        </Box>

        <Typography variant="body2" sx={{ mt: 1.5, mb: 0.5, color: "text.secondary" }}>
          示例提示词
        </Typography>
        <Box sx={{ display: "flex", gap: 0.8, mb: 1.5, flexWrap: "wrap" }}>
          {EXAMPLES.map((ex) => (
            <MuiButton
              key={ex}
              size="small"
              variant="outlined"
              onClick={() => setPrompt(ex)}
              sx={{ textTransform: "none", fontSize: 11, borderRadius: 2 }}
            >
              {ex}
            </MuiButton>
          ))}
        </Box>

        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            gap: 1,
            p: 1,
            borderRadius: 2.5,
            border: 1,
            borderColor: "divider",
            bgcolor: "background.paper",
          }}
        >
          <Box sx={{ flex: 1 }}>
            <TextField
              multiline
              minRows={1}
              fullWidth
              placeholder="描述你想要生成的图片..."
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              disabled={busy}
              variant="standard"
              slotProps={{ input: { disableUnderline: true } }}
            />
          </Box>
          <MuiButton
            variant="contained"
            sx={{ minWidth: 0, borderRadius: "50%", width: 40, height: 40 }}
            disabled={!prompt.trim() || busy}
            onClick={() => void generate()}
          >
            {busy ? <CircularProgress size={18} /> : <SendIcon fontSize="small" />}
          </MuiButton>
        </Box>
      </Paper>

      {/* Right: history */}
      <Paper
        variant="outlined"
        sx={{ width: 300, p: 2, borderRadius: 3, display: "flex", flexDirection: "column" }}
      >
        <Box
          sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 1.5 }}
        >
          <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
            生成历史
          </Typography>
          <IconButton
            size="small"
            onClick={() => {
              setHistory([]);
              localStorage.removeItem(HISTORY_KEY);
            }}
          >
            <RefreshIcon fontSize="small" />
          </IconButton>
        </Box>
        <Box sx={{ flex: 1, overflowY: "auto" }}>
          {history.length === 0 ? (
            <Typography
              variant="body2"
              sx={{ color: "text.secondary", mt: 4, textAlign: "center" }}
            >
              暂无历史
            </Typography>
          ) : (
            history.map((h, i) => (
              <Box key={i} sx={{ p: 1.25, mb: 1, borderRadius: 2, bgcolor: "action.hover" }}>
                <Box sx={{ display: "flex", gap: 1 }}>
                  <EastIcon sx={{ fontSize: 16, color: "primary.main", mt: 0.3 }} />
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography
                      variant="body2"
                      sx={{
                        fontSize: 13,
                        display: "-webkit-box",
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: "vertical",
                        overflow: "hidden",
                      }}
                    >
                      {h.prompt}
                    </Typography>
                    <Typography variant="caption" sx={{ color: "text.secondary" }}>
                      {h.time}
                    </Typography>
                  </Box>
                  <IconButton size="small" sx={{ color: "error.main" }}>
                    <DeleteOutlineIcon fontSize="small" />
                  </IconButton>
                </Box>
                <Box sx={{ display: "flex", gap: 0.5, flexWrap: "wrap", mt: 1 }}>
                  {h.urls.map((u, j) => (
                    <img
                      key={j}
                      src={u}
                      alt=""
                      style={{ width: 56, height: 56, objectFit: "cover", borderRadius: 4 }}
                    />
                  ))}
                </Box>
              </Box>
            ))
          )}
        </Box>
        <Box sx={{ pt: 1, borderTop: 1, borderColor: "divider" }}>
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            共 {history.length} 条
          </Typography>
        </Box>
      </Paper>
    </Box>
  );
}

export default ImageGeneration;
