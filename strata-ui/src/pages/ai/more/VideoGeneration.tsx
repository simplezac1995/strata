// SPDX-License-Identifier: Apache-2.0
import { useEffect, useRef, useState } from "react";
import { Box, CircularProgress, IconButton, Paper, Stack, Typography } from "@mui/material";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutlined";
import DownloadIcon from "@mui/icons-material/Download";
import RefreshIcon from "@mui/icons-material/Refresh";
import RocketLaunchOutlinedIcon from "@mui/icons-material/RocketLaunchOutlined";
import SendIcon from "@mui/icons-material/Send";
import SmartToyOutlinedIcon from "@mui/icons-material/SmartToyOutlined";
import { Button, Select, TextField } from "@/components/ui/index";
import { aiToolApi } from "@/shared/api/ai";
import { toast } from "@/shared/pageKit/Toast";
import { failureMessage } from "@/shared/hooks/useCrud";

const HISTORY_KEY = "ai_video_history";
type HistoryItem = { prompt: string; videoUrl: string; time: string; status: string };

const RATIOS = [
  { value: "16:9", label: "16:9" },
  { value: "9:16", label: "9:16" },
  { value: "1:1", label: "1:1" },
];

function randomSeed() {
  return String(Math.floor(10000 + Math.random() * 90000));
}

export function VideoGeneration() {
  const [prompt, setPrompt] = useState("");
  const [negative, setNegative] = useState("");
  const [ratio, setRatio] = useState("16:9");
  const [seed, setSeed] = useState(randomSeed());
  const [busy, setBusy] = useState(false);
  const [taskId, setTaskId] = useState<string | null>(null);
  const [, setStatus] = useState("");
  const [videoUrl, setVideoUrl] = useState<string | null>(null);
  const [history, setHistory] = useState<HistoryItem[]>([]);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  function stopPoll() {
    if (pollRef.current) {
      clearInterval(pollRef.current);
      pollRef.current = null;
    }
  }
  useEffect(() => {
    try {
      const s = localStorage.getItem(HISTORY_KEY);
      if (s) setHistory(JSON.parse(s));
    } catch {}
    return () => stopPoll();
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
    setStatus("SUBMITTING");
    setVideoUrl(null);
    setTaskId(null);
    try {
      const extra = [`ratio=${ratio}`, `seed=${seed}`];
      if (negative.trim()) extra.push(`negative=${negative.trim()}`);
      const res = await aiToolApi.run("video", `${content}\n[${extra.join(", ")}]`);
      const m = res.result.match(/task_id[=:]\s*([a-zA-Z0-9-]+)/);
      if (m) {
        const id = m[1];
        setTaskId(id);
        setStatus("PENDING");
        saveHistory({
          prompt: content,
          videoUrl: "",
          time: new Date().toLocaleString(),
          status: "InProgress",
        });
        pollRef.current = setInterval(async () => {
          try {
            const r = await fetch(`/api/v1/ai/tools/video/status/${id}`, {
              headers: { Authorization: `Bearer ${localStorage.getItem("strata_access_token")}` },
            }).then((x) => x.json());
            const s = r.data?.status ?? "UNKNOWN";
            setStatus(s);
            if (r.data?.videoUrl) {
              setVideoUrl(r.data.videoUrl);
              saveHistory({
                prompt: content,
                videoUrl: r.data.videoUrl,
                time: new Date().toLocaleString(),
                status: "Succeeded",
              });
            }
            if (s === "SUCCEEDED" || s === "FAILED" || s === "UNKNOWN") stopPoll();
          } catch {}
        }, 5000);
      } else setStatus("SUBMITTED");
    } catch (err) {
      toast.error(failureMessage(err));
      setStatus("FAILED");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Box sx={{ display: "flex", gap: 2, height: "calc(100vh - 140px)", p: 2 }}>
      {/* Left: video config */}
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
          <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
            视频配置
          </Typography>
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            视频引擎
          </Typography>
        </Box>

        <Box>
          <Typography variant="body2" sx={{ mb: 1, fontWeight: 600 }}>
            模型选择
          </Typography>
          <Select
            value=""
            onChange={() => {}}
            displayEmpty
            disabled
            options={[{ value: "", label: "请选择模型" }]}
          />
          <Typography variant="caption" sx={{ color: "success.main", display: "block", mt: 0.5 }}>
            ● 当前模型无需首帧图片
          </Typography>
        </Box>

        <Box>
          <Typography variant="body2" sx={{ mb: 1, fontWeight: 600 }}>
            视频比例
          </Typography>
          <Box sx={{ display: "flex", gap: 1.5 }}>
            {RATIOS.map((r) => (
              <Button
                key={r.value}
                variant={ratio === r.value ? "contained" : "outlined"}
                onClick={() => setRatio(r.value)}
                sx={{
                  flex: 1,
                  py: 1.2,
                  borderRadius: 2,
                  borderStyle: ratio === r.value ? "solid" : "dashed",
                }}
              >
                {r.label}
              </Button>
            ))}
          </Box>
        </Box>

        <Box>
          <Typography variant="body2" sx={{ mb: 1, fontWeight: 600 }}>
            生成种子
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
          <Typography variant="body2" sx={{ mb: 1, fontWeight: 600 }}>
            反向提示词
          </Typography>
          <TextField
            multiline
            minRows={4}
            fullWidth
            size="small"
            placeholder="色调艳丽、过曝、静态、模糊不清、字幕…"
            value={negative}
            onChange={(e) => setNegative(e.target.value)}
          />
        </Box>
      </Paper>

      {/* Center: workbench */}
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
        <Box sx={{ display: "flex", alignItems: "baseline", gap: 1.5, mb: 0.5 }}>
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            {taskId ? "等待任务" : "空闲"}
          </Typography>
          <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
            视频工作台
          </Typography>
        </Box>
        <Typography variant="caption" sx={{ color: "text.secondary", mb: 2 }}>
          使用先进的 AI 编排器创建高保真视频内容
        </Typography>

        <Box
          sx={{
            flex: 1,
            bgcolor: "action.hover",
            borderRadius: 2.5,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            overflow: "hidden",
            minHeight: 280,
          }}
        >
          {videoUrl ? (
            <Box sx={{ position: "relative", width: "100%", p: 2 }}>
              <video
                controls
                src={videoUrl}
                style={{ width: "100%", maxHeight: 420, borderRadius: 8 }}
              />
              <IconButton
                href={videoUrl}
                download
                component="a"
                target="_blank"
                sx={{ position: "absolute", top: 16, right: 16, bgcolor: "background.paper" }}
                size="small"
              >
                <DownloadIcon />
              </IconButton>
            </Box>
          ) : (
            <Stack spacing={1.5} sx={{ alignItems: "center", color: "text.disabled" }}>
              {busy ? <CircularProgress /> : <RocketLaunchOutlinedIcon sx={{ fontSize: 56 }} />}
              <Typography variant="subtitle1" sx={{ fontWeight: 700, color: "text.primary" }}>
                {busy ? "正在生成…" : "画布就绪"}
              </Typography>
              <Typography variant="body2" sx={{ color: "text.secondary" }}>
                在下方输入框中描述您的创意，开始生成视频
              </Typography>
            </Stack>
          )}
        </Box>

        <Typography variant="body2" sx={{ mt: 2, mb: 1, color: "primary.main", cursor: "pointer" }}>
          常用示例
        </Typography>
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
          <TextField
            multiline
            minRows={1}
            fullWidth
            placeholder="描述你想要生成的视频..."
            value={prompt}
            onChange={(e) => setPrompt(e.target.value)}
            disabled={busy}
            variant="standard"
            slotProps={{ input: { disableUnderline: true } }}
          />
          <Button
            variant="contained"
            disabled={!prompt.trim() || busy}
            onClick={() => void generate()}
            sx={{ borderRadius: 2, textTransform: "none" }}
          >
            {busy ? <CircularProgress size={18} sx={{ mr: 0.5 }} /> : <SendIcon sx={{ mr: 0.5 }} />}
            生成
          </Button>
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
                  {h.status === "InProgress" ? (
                    <CircularProgress size={14} sx={{ mt: 0.5 }} />
                  ) : (
                    <SmartToyOutlinedIcon sx={{ fontSize: 16, color: "primary.main", mt: 0.3 }} />
                  )}
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
                    {h.status === "InProgress" && (
                      <Typography
                        variant="caption"
                        sx={{ color: "text.disabled", display: "block" }}
                      >
                        InProgress
                      </Typography>
                    )}
                  </Box>
                  {h.videoUrl ? (
                    <IconButton
                      size="small"
                      href={h.videoUrl}
                      download
                      component="a"
                      target="_blank"
                    >
                      <DownloadIcon fontSize="small" />
                    </IconButton>
                  ) : (
                    <IconButton size="small" sx={{ color: "error.main" }}>
                      <DeleteOutlineIcon fontSize="small" />
                    </IconButton>
                  )}
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

export default VideoGeneration;
