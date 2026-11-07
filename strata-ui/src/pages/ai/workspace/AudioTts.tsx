// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import {
  Box,
  Button as MuiButton,
  CircularProgress,
  IconButton,
  Paper,
  Slider,
  Stack,
  Typography,
} from "@mui/material";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutlined";
import GraphicEqIcon from "@mui/icons-material/GraphicEq";
import HeadphonesOutlinedIcon from "@mui/icons-material/HeadphonesOutlined";
import HistoryIcon from "@mui/icons-material/History";
import SendIcon from "@mui/icons-material/Send";
import { Select, TextField } from "@/components/ui/index";
import { failureMessage } from "@/shared/hooks/useCrud";
import { aiToolApi } from "@/shared/api/ai";
import { toast } from "@/shared/pageKit/Toast";

const VOICES = [
  { value: "longxiaochun", label: "龙小淳 (女)" },
  { value: "longhua", label: "龙华 (男)" },
  { value: "longshu", label: "龙书 (女)" },
  { value: "longjielidou", label: "龙杰力斗 (男)" },
];

const HISTORY_KEY = "ai_tts_history";
type HistoryItem = { text: string; audioUrl: string; time: string };

// 装饰性波形柱（无真实音频分析，仅占位可视化）
const BARS = [
  30, 45, 60, 40, 80, 55, 95, 70, 50, 85, 65, 100, 75, 55, 90, 60, 80, 45, 70, 95, 50, 65, 85, 40,
];

export function AudioTts() {
  const [text, setText] = useState("");
  const [speed, setSpeed] = useState(1);
  const [volume, setVolume] = useState(0);
  const [voice, setVoice] = useState("longxiaochun");
  const [result, setResult] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [history, setHistory] = useState<HistoryItem[]>([]);

  useEffect(() => {
    try {
      const s = localStorage.getItem(HISTORY_KEY);
      if (s) setHistory(JSON.parse(s));
    } catch {}
  }, []);

  function saveHistory(item: HistoryItem) {
    const next = [item, ...history].slice(0, 20);
    setHistory(next);
    localStorage.setItem(HISTORY_KEY, JSON.stringify(next));
  }

  async function generate() {
    const content = text.trim();
    if (!content) return;
    setBusy(true);
    setResult(null);
    try {
      const res = await aiToolApi.run(
        "tts",
        `${content}\n[voice=${voice}, speed=${speed}, volume=${volume}]`,
      );
      setResult(res.result);
      if (res.result.startsWith("http") || res.result.startsWith("data:")) {
        saveHistory({ text: content, audioUrl: res.result, time: new Date().toLocaleString() });
      }
    } catch (err) {
      toast.error(failureMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Box sx={{ display: "flex", gap: 2, height: "calc(100vh - 140px)", p: 2 }}>
      {/* Left: voice config */}
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
            语音配置
          </Typography>
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            语音引擎
          </Typography>
        </Box>

        <Box>
          <Typography variant="body2" sx={{ mb: 1, fontWeight: 600 }}>
            模型
          </Typography>
          <Select
            value=""
            onChange={() => {}}
            disabled
            displayEmpty
            options={[{ value: "", label: "未配置默认模型，将尝试本地 Cosy" }]}
          />
        </Box>

        <Box>
          <Typography variant="body2" sx={{ mb: 1, fontWeight: 600 }}>
            音色
          </Typography>
          <Select value={voice} onChange={(e) => setVoice(e.target.value)} options={VOICES} />
        </Box>

        <Box>
          <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              倍速控制
            </Typography>
            <Typography variant="body2" sx={{ color: "primary.main", fontWeight: 700 }}>
              {speed.toFixed(1)}x
            </Typography>
          </Box>
          <Slider
            min={0.5}
            max={2}
            step={0.1}
            value={speed}
            onChange={(_, v) => setSpeed(v as number)}
          />
        </Box>

        <Box>
          <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              音量增益
            </Typography>
            <Typography variant="body2" sx={{ color: "primary.main", fontWeight: 700 }}>
              {volume} dB
            </Typography>
          </Box>
          <Slider
            min={-10}
            max={10}
            step={1}
            value={volume}
            onChange={(_, v) => setVolume(v as number)}
          />
        </Box>
      </Paper>

      {/* Center: waveform + player */}
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
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
            gap: 2,
            p: 3,
            overflow: "hidden",
          }}
        >
          {busy ? (
            <CircularProgress />
          ) : result ? (
            result.startsWith("http") || result.startsWith("data:") ? (
              <Box sx={{ width: "100%", maxWidth: 480 }}>
                <audio controls src={result} style={{ width: "100%" }} />
              </Box>
            ) : (
              <Typography variant="body2" sx={{ whiteSpace: "pre-wrap", p: 2 }}>
                {result}
              </Typography>
            )
          ) : (
            <>
              <Box sx={{ display: "flex", alignItems: "center", gap: 0.8, height: 120 }}>
                {BARS.map((h, i) => (
                  <Box
                    key={i}
                    sx={{
                      width: 8,
                      height: h,
                      borderRadius: 4,
                      bgcolor: "primary.light",
                      opacity: 0.35,
                    }}
                  />
                ))}
              </Box>
              <Stack spacing={1} sx={{ alignItems: "center" }}>
                <HeadphonesOutlinedIcon sx={{ fontSize: 40, color: "text.disabled" }} />
                <Typography variant="body2" sx={{ color: "text.secondary" }}>
                  波形可视化占位
                </Typography>
                <Typography variant="caption" sx={{ color: "text.disabled" }}>
                  输入描述并点击生成按钮
                </Typography>
              </Stack>
            </>
          )}
        </Box>

        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            gap: 1,
            p: 1,
            mt: 2,
            borderRadius: 2.5,
            border: 1,
            borderColor: "divider",
            bgcolor: "background.paper",
          }}
        >
          <GraphicEqIcon sx={{ color: "text.disabled", ml: 0.5 }} />
          <Box sx={{ flex: 1 }}>
            <TextField
              multiline
              minRows={1}
              fullWidth
              placeholder="描述你想要生成的音频..."
              value={text}
              onChange={(e) => setText(e.target.value)}
              disabled={busy}
              variant="standard"
              slotProps={{ input: { disableUnderline: true } }}
            />
          </Box>
          <MuiButton
            variant="contained"
            sx={{ minWidth: 0, borderRadius: "50%", width: 40, height: 40 }}
            disabled={!text.trim() || busy}
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
            <HistoryIcon fontSize="small" />
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
                  <GraphicEqIcon sx={{ fontSize: 16, color: "primary.main", mt: 0.3 }} />
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
                      {h.text}
                    </Typography>
                    <Typography variant="caption" sx={{ color: "text.secondary" }}>
                      {h.time}
                    </Typography>
                  </Box>
                  <IconButton size="small" sx={{ color: "error.main" }}>
                    <DeleteOutlineIcon fontSize="small" />
                  </IconButton>
                </Box>
                <audio
                  controls
                  src={h.audioUrl}
                  style={{ width: "100%", marginTop: 4, height: 32 }}
                />
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

export default AudioTts;
