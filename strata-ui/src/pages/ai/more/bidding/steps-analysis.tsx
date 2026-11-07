// SPDX-License-Identifier: Apache-2.0
// Steps 2-3: requirement/score extraction and the response outline.

import { Box, IconButton, LinearProgress, Paper, Typography } from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import AutoAwesomeIcon from "@mui/icons-material/AutoAwesome";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutlined";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import { Button, Chip, Select, TextField, TextArea } from "@/components/ui/index";
import { toast } from "@/shared/pageKit/Toast";
import { ReqEntry, OutlineRow } from "./content";
import { Caption, StatChip } from "./shared";

export function RequirementsStep({
  tab,
  onTab,
  extractState,
  responseEntries,
  scoreEntries,
  invalidEntries,
  setResponseEntries,
  setScoreEntries,
  setInvalidEntries,
  onExtract,
}: {
  tab: "response" | "score" | "invalid";
  onTab: (t: "response" | "score" | "invalid") => void;
  extractState: "idle" | "running" | "done";
  responseEntries: ReqEntry[];
  scoreEntries: ReqEntry[];
  invalidEntries: ReqEntry[];
  setResponseEntries: (v: ReqEntry[]) => void;
  setScoreEntries: (v: ReqEntry[]) => void;
  setInvalidEntries: (v: ReqEntry[]) => void;
  onExtract: () => void;
}) {
  const entries =
    tab === "response" ? responseEntries : tab === "score" ? scoreEntries : invalidEntries;
  const setEntries =
    tab === "response" ? setResponseEntries : tab === "score" ? setScoreEntries : setInvalidEntries;
  const desc =
    tab === "response"
      ? "资格、商务与技术响应事项"
      : tab === "score"
        ? "评分项、分值与得分依据"
        : "可能导致无效投标的刚性条件";

  function addEntry() {
    setEntries([
      ...entries,
      {
        id: `n${Date.now()}`,
        title: "",
        required: true,
        content: "",
        evidenceLoc: "",
        evidenceText: "",
        score: tab === "score" ? "0.00" : undefined,
        riskLevel: tab === "invalid" ? "严重" : undefined,
      },
    ]);
  }
  function removeEntry(id: string) {
    setEntries(entries.filter((e) => e.id !== id));
  }
  function update(id: string, patch: Partial<ReqEntry>) {
    setEntries(entries.map((e) => (e.id === id ? { ...e, ...patch } : e)));
  }

  return (
    <Box>
      {extractState === "running" && (
        <Box sx={{ mb: 2, p: 2, borderRadius: 2, bgcolor: "action.hover" }}>
          <Box sx={{ display: "flex", justifyContent: "space-between", mb: 1 }}>
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              提取需求与评分项
            </Typography>
            <Typography variant="body2" sx={{ color: "primary.main", fontWeight: 700 }}>
              38%
            </Typography>
          </Box>
          <LinearProgress variant="determinate" value={38} />
        </Box>
      )}

      <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 2, flexWrap: "wrap" }}>
        <StatChip>
          共 {responseEntries.length + scoreEntries.length + invalidEntries.length} 项
        </StatChip>
        <StatChip>必响应 {responseEntries.filter((r) => r.required).length} 项</StatChip>
        <StatChip>
          评分合计 {scoreEntries.reduce((a, s) => a + Number(s.score ?? 0), 0)} 分
        </StatChip>
        <Box sx={{ flex: 1 }} />
        <Button
          variant="outlined"
          size="small"
          startIcon={<AutoAwesomeIcon />}
          onClick={onExtract}
          disabled={extractState === "running"}
        >
          重新提取
        </Button>
        <Button variant="contained" size="small" onClick={() => toast.success("common.saved")}>
          保存矩阵
        </Button>
      </Box>

      <Box sx={{ borderBottom: 1, borderColor: "divider", display: "flex", gap: 3, mb: 2 }}>
        {(
          [
            ["response", `响应要求 ${responseEntries.length}`],
            ["score", `评分标准 ${scoreEntries.length}`],
            ["invalid", `废标条款 ${invalidEntries.length}`],
          ] as const
        ).map(([id, label]) => (
          <Typography
            key={id}
            onClick={() => onTab(id)}
            sx={{
              cursor: "pointer",
              pb: 1,
              fontWeight: 600,
              color: tab === id ? "primary.main" : "text.secondary",
              borderBottom: tab === id ? 2 : 0,
              borderColor: "primary.main",
            }}
          >
            {label}
          </Typography>
        ))}
      </Box>

      <Box sx={{ display: "flex", alignItems: "center", mb: 2 }}>
        <Caption>{desc}</Caption>
        <Box sx={{ flex: 1 }} />
        <Typography
          onClick={addEntry}
          sx={{ color: "primary.main", cursor: "pointer", fontWeight: 600 }}
        >
          ＋ 新增条目
        </Typography>
      </Box>

      {entries.map((e, idx) => (
        <Paper
          key={e.id}
          variant="outlined"
          sx={{ p: 2.5, mb: 2, borderLeft: 3, borderColor: "primary.light" }}
        >
          <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mb: 1.5 }}>
            <Chip
              label={String(idx + 1).padStart(2, "0")}
              sx={{ bgcolor: "action.hover", fontWeight: 700 }}
            />
            <Typography variant="subtitle2" sx={{ fontWeight: 700, flex: 1 }}>
              {e.title || "未命名条目"}
            </Typography>
            <IconButton size="small" sx={{ color: "error.main" }} onClick={() => removeEntry(e.id)}>
              <DeleteOutlineIcon fontSize="small" />
            </IconButton>
          </Box>

          <Box sx={{ display: "flex", gap: 2, alignItems: "flex-start", mb: 1.5 }}>
            <Box sx={{ flex: 1 }}>
              <Caption sx={{ mb: 0.5, display: "block" }}>
                <Box component="span" sx={{ color: "error.main" }}>
                  *
                </Box>{" "}
                条目标题
              </Caption>
              <TextField
                value={e.title}
                onChange={(ev) => update(e.id, { title: ev.target.value })}
              />
            </Box>
            {tab === "response" && (
              <Box sx={{ width: 160 }}>
                <Caption sx={{ mb: 0.5, display: "block" }}>响应属性</Caption>
                <Select
                  value={e.required ? "required" : "optional"}
                  onChange={(ev) => update(e.id, { required: ev.target.value === "required" })}
                  options={[
                    { value: "required", label: "必选" },
                    { value: "optional", label: "可选" },
                  ]}
                />
              </Box>
            )}
            {tab === "score" && (
              <Box sx={{ width: 140 }}>
                <Caption sx={{ mb: 0.5, display: "block" }}>最高分值</Caption>
                <TextField
                  type="number"
                  value={e.score}
                  onChange={(ev) => update(e.id, { score: ev.target.value })}
                />
              </Box>
            )}
            {tab === "invalid" && (
              <Box sx={{ width: 140 }}>
                <Caption sx={{ mb: 0.5, display: "block" }}>风险级别</Caption>
                <Select
                  value={e.riskLevel ?? "严重"}
                  onChange={(ev) => update(e.id, { riskLevel: ev.target.value })}
                  options={[
                    { value: "严重", label: "严重" },
                    { value: "高", label: "高" },
                    { value: "中", label: "中" },
                  ]}
                />
              </Box>
            )}
          </Box>

          <Caption sx={{ mb: 0.5, display: "block" }}>
            <Box component="span" sx={{ color: "error.main" }}>
              *
            </Box>{" "}
            具体要求
          </Caption>
          <TextArea
            rows={2}
            value={e.content}
            onChange={(ev) => update(e.id, { content: ev.target.value })}
            sx={{ mb: 1.5 }}
          />

          <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: "action.hover" }}>
            <Typography variant="caption" sx={{ color: "primary.main", fontWeight: 700 }}>
              原文证据
            </Typography>
            <Caption sx={{ display: "inline", ml: 1 }}>用于人工回查，不会在保存时丢弃</Caption>
            <Box sx={{ display: "flex", gap: 2, mt: 1 }}>
              <Box sx={{ flex: 1 }}>
                <Caption sx={{ mb: 0.5, display: "block" }}>证据定位</Caption>
                <TextField
                  value={e.evidenceLoc}
                  onChange={(ev) => update(e.id, { evidenceLoc: ev.target.value })}
                />
              </Box>
              <Box sx={{ flex: 1 }}>
                <Caption sx={{ mb: 0.5, display: "block" }}>原文摘录</Caption>
                <TextField
                  value={e.evidenceText}
                  onChange={(ev) => update(e.id, { evidenceText: ev.target.value })}
                />
              </Box>
            </Box>
          </Box>
        </Paper>
      ))}
    </Box>
  );
}

/* ============================== 步骤 3：标书大纲 ============================== */
export function OutlineStep({
  state,
  rows,
  onRows,
  onRun,
}: {
  state: "idle" | "generating" | "done";
  rows: OutlineRow[];
  onRows: (v: OutlineRow[]) => void;
  onRun: () => void;
}) {
  function addRow(level: 1 | 2 | 3) {
    onRows([...rows, { id: `o${Date.now()}`, level, num: "new", title: "", mapping: "" }]);
  }
  function removeRow(id: string) {
    onRows(rows.filter((r) => r.id !== id));
  }
  function update(id: string, patch: Partial<OutlineRow>) {
    onRows(rows.map((r) => (r.id === id ? { ...r, ...patch } : r)));
  }

  return (
    <Box>
      {state === "generating" && (
        <Box sx={{ mb: 2, p: 2, borderRadius: 2, bgcolor: "action.hover" }}>
          <Box sx={{ display: "flex", justifyContent: "space-between", mb: 1 }}>
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              生成标书大纲
            </Typography>
            <Typography variant="body2" sx={{ color: "primary.main", fontWeight: 700 }}>
              38%
            </Typography>
          </Box>
          <LinearProgress variant="determinate" value={38} />
        </Box>
      )}

      <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 2 }}>
        <StatChip>共 {rows.length} 个章节</StatChip>
        <StatChip>当前 3 层</StatChip>
        <StatChip>树形保存</StatChip>
        <Box sx={{ flex: 1 }} />
        <Button
          variant="outlined"
          size="small"
          startIcon={<AutoAwesomeIcon />}
          onClick={onRun}
          disabled={state === "generating"}
        >
          {state === "done" ? "重新生成大纲" : "AI 生成大纲"}
        </Button>
        <Button variant="contained" size="small" onClick={() => toast.success("common.saved")}>
          保存大纲
        </Button>
      </Box>

      <Paper variant="outlined">
        <Box sx={{ display: "flex", alignItems: "center", p: 2, pb: 1 }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
            章节树
          </Typography>
          <Caption sx={{ ml: 1 }}>拖动序号调整同级顺序，也可继续新增子级</Caption>
          <Box sx={{ flex: 1 }} />
          <Typography
            onClick={() => addRow(1)}
            sx={{ color: "primary.main", cursor: "pointer", fontWeight: 600 }}
          >
            ＋ 新增一级章节
          </Typography>
        </Box>

        {rows.length === 0 ? (
          <Box sx={{ py: 6, textAlign: "center" }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
              尚未生成大纲
            </Typography>
            <Caption sx={{ mb: 2 }}>点击下方按钮手动创建一级章节</Caption>
            <Button variant="outlined" size="small" onClick={() => addRow(1)}>
              手动创建一级章节
            </Button>
          </Box>
        ) : (
          <Box sx={{ px: 2, pb: 2 }}>
            {rows.map((r) => (
              <Box
                key={r.id}
                sx={{
                  display: "flex",
                  alignItems: "center",
                  gap: 1,
                  pl: (r.level - 1) * 28,
                  py: 0.75,
                }}
              >
                <ExpandMoreIcon sx={{ fontSize: 16, color: "text.secondary" }} />
                <Chip label={`L${r.level}`} sx={{ bgcolor: "action.hover", height: 22 }} />
                <Box sx={{ display: "flex", alignItems: "center", color: "text.secondary" }}>
                  <AddIcon sx={{ fontSize: 14, mr: 0.5 }} />
                  <Typography variant="caption">{r.num}</Typography>
                </Box>
                <TextField
                  value={r.title}
                  onChange={(ev) => update(r.id, { title: ev.target.value })}
                  sx={{ flex: 1, "& .MuiOutlinedInput-root": { borderRadius: 1.5 } }}
                />
                {r.level === 3 && (
                  <Select
                    value={r.mapping}
                    onChange={(ev) => update(r.id, { mapping: ev.target.value })}
                    sx={{ width: 220 }}
                    options={[
                      { value: "", label: "不映射" },
                      { value: "废标·签字盖章不完整", label: "废标·签字盖章不完整" },
                      { value: "需求·交付与验收期限", label: "需求·交付与验收期限" },
                      {
                        value: "需求·国产化台式终端技术响应",
                        label: "需求·国产化台式终端技术响应",
                      },
                      { value: "需求·笔记本续航与重量", label: "需求·笔记本续航与重量" },
                    ]}
                  />
                )}
                <IconButton size="small" onClick={() => addRow((r.level + 1) as 1 | 2 | 3)}>
                  <AddIcon fontSize="small" />
                </IconButton>
                <IconButton
                  size="small"
                  sx={{ color: "error.main" }}
                  onClick={() => removeRow(r.id)}
                >
                  <DeleteOutlineIcon fontSize="small" />
                </IconButton>
              </Box>
            ))}
          </Box>
        )}
      </Paper>
    </Box>
  );
}

/* ============================== 步骤 4：专业撰写 ============================== */
