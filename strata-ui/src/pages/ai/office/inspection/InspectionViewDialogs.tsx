// SPDX-License-Identifier: Apache-2.0
// Dialogs of Inspection, extracted to keep the view under the 500-line cap.

import {
  PlayArrow as RunIcon,
  Description as ReportIcon,
  Article as ArticleIcon,
  Schedule as ClockIcon,
  Science as ProbeIcon,
  Stop as StopIcon,
  HourglassEmpty as HourglassIcon,
} from "@mui/icons-material";
import {
  Box,
  Chip,
  CircularProgress,
  LinearProgress,
  Paper,
  Radio,
  Stack,
  Typography,
} from "@mui/material";
import { Button, ConfirmDialog, Dialog, Select, TextArea, TextField } from "@/components/ui/index";
import { toast } from "@/shared/pageKit/Toast";
import { TIME_RANGES, DEFAULT_SYSTEM_PROMPT } from "./model";
import type { useInspection } from "./main/useInspection";

export function InspectionViewDialogs({ view }: { view: ReturnType<typeof useInspection> }) {
  const {
    tasks,
    setTasks,
    keyword,
    setKeyword,
    statusFilter,
    setStatusFilter,
    formOpen,
    setFormOpen,
    editing,
    setEditing,
    form,
    setForm,
    deleting,
    setDeleting,
    runTask,
    setRunTask,
    phase,
    setPhase,
    logs,
    setLogs,
    report,
    setReport,
    duration,
    setDuration,
    historyOpen,
    setHistoryOpen,
    runHistory,
    setRunHistory,
    activeRecord,
    setActiveRecord,
    filtered,
    columns,
    openCreate,
    openEdit,
    saveForm,
    confirmDelete,
    openRun,
    openHistory,
    startRun,
  } = view;

  return (
    <>
      <Dialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        title={editing ? "编辑巡检任务" : "新建巡检任务"}
        maxWidth="md"
      >
        <Stack spacing={2.5} sx={{ py: 1 }}>
          <Box sx={{ display: "flex", gap: 2, alignItems: "center" }}>
            <Typography sx={{ width: 110, flexShrink: 0 }}>* 任务名称</Typography>
            <TextField
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              placeholder="请输入任务名称"
              sx={{ flex: 1 }}
            />
          </Box>
          <Box sx={{ display: "flex", gap: 2, alignItems: "center" }}>
            <Typography sx={{ width: 110, flexShrink: 0 }}>* 启用状态</Typography>
            <Stack direction="row" spacing={1.5}>
              {[
                { value: true, label: "启用" },
                { value: false, label: "停用" },
              ].map((opt) => (
                <Box
                  key={opt.label}
                  onClick={() => setForm((f) => ({ ...f, enabled: opt.value }))}
                  sx={{
                    display: "flex",
                    alignItems: "center",
                    gap: 0.5,
                    px: 1.5,
                    py: 0.5,
                    border: 1,
                    borderRadius: 1,
                    cursor: "pointer",
                    borderColor: form.enabled === opt.value ? "primary.main" : "divider",
                  }}
                >
                  <Radio size="small" checked={form.enabled === opt.value} />
                  {opt.label}
                </Box>
              ))}
            </Stack>
          </Box>
          <Box sx={{ display: "flex", gap: 2, alignItems: "center" }}>
            <Typography sx={{ width: 110, flexShrink: 0 }}>* 日志目录</Typography>
            <TextField
              value={form.logDir}
              onChange={(e) => setForm((f) => ({ ...f, logDir: e.target.value }))}
              placeholder="/var/logs/app"
              sx={{ flex: 1 }}
            />
            <Button
              variant="outlined"
              size="small"
              color="default"
              startIcon={<ProbeIcon />}
              onClick={() => form.logDir.trim() && toast.success("探测到日志目录")}
            >
              探测
            </Button>
          </Box>
          <Box sx={{ display: "flex", gap: 2, alignItems: "center" }}>
            <Typography sx={{ width: 110, flexShrink: 0 }}>文件匹配</Typography>
            <TextField
              value={form.fileMatch}
              onChange={(e) => setForm((f) => ({ ...f, fileMatch: e.target.value }))}
              placeholder="*.log"
              sx={{ flex: 1 }}
            />
          </Box>
          <Box sx={{ display: "flex", gap: 2, alignItems: "center" }}>
            <Typography sx={{ width: 110, flexShrink: 0 }}>时间范围</Typography>
            <Select
              value={form.timeRange}
              onChange={(e) => setForm((f) => ({ ...f, timeRange: e.target.value }))}
              sx={{ flex: 1 }}
              options={TIME_RANGES}
            />
          </Box>
          <Box sx={{ display: "flex", gap: 2 }}>
            <Typography sx={{ width: 110, flexShrink: 0, pt: 1 }}>系统提示词</Typography>
            <Box sx={{ flex: 1 }}>
              <TextArea
                value={form.systemPrompt}
                onChange={(e) => setForm((f) => ({ ...f, systemPrompt: e.target.value }))}
                rows={4}
                slotProps={{ input: { sx: { fontFamily: "monospace" } } }}
              />
              <Box sx={{ mt: 0.5, textAlign: "right" }}>
                <Button
                  variant="text"
                  size="small"
                  onClick={() => setForm((f) => ({ ...f, systemPrompt: DEFAULT_SYSTEM_PROMPT }))}
                >
                  恢复默认
                </Button>
              </Box>
            </Box>
          </Box>
        </Stack>
        <Box sx={{ display: "flex", gap: 1, justifyContent: "flex-end", px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={() => setFormOpen(false)}>
            取消
          </Button>
          <Button variant="contained" onClick={saveForm}>
            确认
          </Button>
        </Box>
      </Dialog>

      <Dialog
        open={runTask !== null}
        onClose={() => setRunTask(null)}
        title={`实时巡检 - ${runTask?.name ?? ""} [演示模式]`}
        maxWidth="lg"
      >
        <Paper variant="outlined" sx={{ borderRadius: 2 }}>
          <Box sx={{ display: "flex", borderBottom: 1, borderColor: "divider" }}>
            <Box
              sx={{
                width: 120,
                p: 1.5,
                bgcolor: "action.hover",
                borderRight: 1,
                borderColor: "divider",
              }}
            >
              <Typography variant="caption" sx={{ color: "text.secondary" }}>
                日志目录
              </Typography>
            </Box>
            <Box sx={{ flex: 1, p: 1.5 }}>{runTask?.logDir || "-"}</Box>
            <Box
              sx={{
                width: 120,
                p: 1.5,
                bgcolor: "action.hover",
                borderRight: 1,
                borderColor: "divider",
              }}
            >
              <Typography variant="caption" sx={{ color: "text.secondary" }}>
                时间范围
              </Typography>
            </Box>
            <Box sx={{ flex: 1, p: 1.5 }}>
              {TIME_RANGES.find((x) => x.value === runTask?.timeRange)?.label ?? "-"}
            </Box>
          </Box>
          <Box sx={{ display: "flex" }}>
            <Box
              sx={{
                width: 120,
                p: 1.5,
                bgcolor: "action.hover",
                borderRight: 1,
                borderColor: "divider",
              }}
            >
              <Typography variant="caption" sx={{ color: "text.secondary" }}>
                状态
              </Typography>
            </Box>
            <Box sx={{ flex: 1, p: 1.5, display: "flex", alignItems: "center", gap: 1 }}>
              {phase === "idle" && <Chip label="未开始" size="small" variant="outlined" />}
              {phase === "analyzing" && <Chip label="分析中..." size="small" color="warning" />}
              {phase === "done" && (
                <>
                  <Chip label="巡检完成" size="small" color="success" />
                  <Typography variant="caption" sx={{ color: "text.secondary" }}>
                    耗时: {duration.toFixed(1)}s
                  </Typography>
                </>
              )}
            </Box>
          </Box>
        </Paper>

        {phase === "analyzing" && (
          <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mt: 2 }}>
            <LinearProgress sx={{ flex: 1, borderRadius: 1 }} />
            <Typography variant="caption" sx={{ color: "text.secondary" }}>
              准备中...
            </Typography>
          </Box>
        )}

        <Box sx={{ display: "flex", gap: 2, mt: 2, height: 360 }}>
          {/* 执行过程 */}
          <Box
            sx={{
              flex: 1,
              border: 1,
              borderColor: "divider",
              borderRadius: 2,
              p: 2,
              overflowY: "auto",
            }}
          >
            <Stack direction="row" spacing={1} sx={{ alignItems: "center", mb: 1.5 }}>
              <ClockIcon fontSize="small" sx={{ color: "text.secondary" }} />
              <Typography variant="subtitle2">执行过程</Typography>
            </Stack>
            {logs.length === 0 ? (
              <Box
                sx={{
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  justifyContent: "center",
                  height: "80%",
                  gap: 1,
                }}
              >
                <HourglassIcon sx={{ fontSize: 40, color: "text.disabled" }} />
                <Typography variant="body2" sx={{ color: "text.secondary" }}>
                  等待执行...
                </Typography>
              </Box>
            ) : (
              <Stack spacing={1.5}>
                {logs.map((log, i) => (
                  <Box key={i} sx={{ pl: 1 }}>
                    <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                      <Typography
                        variant="caption"
                        sx={{ color: "text.secondary", fontFamily: "monospace" }}
                      >
                        {log.time}
                      </Typography>
                      <Chip
                        label={log.tool}
                        size="small"
                        variant="outlined"
                        sx={{ fontFamily: "monospace" }}
                      />
                    </Stack>
                    <Typography
                      variant="caption"
                      sx={{ fontFamily: "monospace", display: "block", pl: 4 }}
                    >
                      {log.command}
                    </Typography>
                  </Box>
                ))}
                {phase === "analyzing" && <CircularProgress size={18} sx={{ ml: 4 }} />}
              </Stack>
            )}
          </Box>

          {/* 巡检报告 */}
          <Box
            sx={{
              flex: 1,
              border: 1,
              borderColor: "divider",
              borderRadius: 2,
              p: 2,
              overflowY: "auto",
            }}
          >
            <Stack direction="row" spacing={1} sx={{ alignItems: "center", mb: 1.5 }}>
              <ArticleIcon fontSize="small" sx={{ color: "text.secondary" }} />
              <Typography variant="subtitle2">巡检报告</Typography>
            </Stack>
            {phase !== "done" ? (
              <Box
                sx={{
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  justifyContent: "center",
                  height: "80%",
                  gap: 1,
                }}
              >
                <ReportIcon sx={{ fontSize: 48, color: "text.disabled" }} />
                <Typography variant="body2" sx={{ color: "text.secondary" }}>
                  {phase === "analyzing" ? "正在分析日志..." : '点击下方"开始巡检"按钮执行分析'}
                </Typography>
              </Box>
            ) : (
              <Typography variant="body2" sx={{ whiteSpace: "pre-wrap", lineHeight: 1.7 }}>
                {report}
              </Typography>
            )}
          </Box>
        </Box>

        <Box sx={{ display: "flex", gap: 1, justifyContent: "flex-end", px: 0, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={() => setRunTask(null)}>
            关闭
          </Button>
          {phase === "analyzing" ? (
            <Button variant="contained" color="primary" startIcon={<StopIcon />} disabled>
              停止
            </Button>
          ) : (
            <Button variant="contained" onClick={() => void startRun()} startIcon={<RunIcon />}>
              {phase === "done" ? "再次巡检" : "开始巡检"}
            </Button>
          )}
        </Box>
      </Dialog>

      <ConfirmDialog
        open={deleting !== null}
        onClose={() => setDeleting(null)}
        onConfirm={confirmDelete}
        title="删除巡检任务"
        description={`确认删除 "${deleting?.name}"？`}
        confirmColor="danger"
      />
    </>
  );
}
