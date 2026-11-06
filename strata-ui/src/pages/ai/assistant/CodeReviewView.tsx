// SPDX-License-Identifier: Apache-2.0
// View of CodeReview: the JSX, with the hook result passed in as `view`.

import {
  Add as AddIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
  DeleteOutlined as DeleteIcon,
  PlayArrow as RunIcon,
  Description as ReportIcon,
  Article as ArticleIcon,
  Close as CloseIcon,
  Schedule as ClockIcon,
  Folder as FolderIcon,
  CheckCircle as DoneIcon,
  Stop as StopIcon,
  HourglassEmpty as HourglassIcon,
} from "@mui/icons-material";
import {
  Box,
  Chip,
  CircularProgress,
  Divider,
  IconButton,
  LinearProgress,
  Paper,
  Radio,
  Stack,
  Typography,
} from "@mui/material";
import {
  Button,
  ConfirmDialog,
  DataTable,
  Dialog,
  Select,
  TextArea,
  TextField,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { toast } from "@/shared/pageKit/Toast";
import { ReviewTask, TIME_RANGES, DEFAULT_SYSTEM_PROMPT } from "./codereview/part-1";
import type { useCodeReview } from "./codereview/useCodeReview";
import { CodeReviewViewDialogs } from "./CodeReviewViewDialogs";

export function CodeReviewView({ view }: { view: ReturnType<typeof useCodeReview> }) {
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
    openRun,
    openHistory,
    startRun,
  } = view;

  return (
    <>
      <PageStack>
        <Box
          sx={{
            display: "flex",
            gap: 1.5,
            alignItems: "center",
            flexWrap: "wrap",
            "& .MuiTextField-root, & .MuiFormControl-root": { width: "auto", minWidth: 160 },
          }}
        >
          <Typography variant="body2">任务名称</Typography>
          <TextField
            placeholder="请输入任务名称"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            sx={{ width: 220 }}
          />
          <Typography variant="body2">状态</Typography>
          <Select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            sx={{ width: 160 }}
            options={[
              { value: "", label: "请选择状态" },
              { value: "enabled", label: "启用" },
              { value: "disabled", label: "停用" },
            ]}
          />
          <Button variant="contained" size="small" startIcon={<SearchIcon />}>
            查询
          </Button>
          <Button
            variant="outlined"
            size="small"
            color="default"
            startIcon={<RefreshIcon />}
            onClick={() => {
              setKeyword("");
              setStatusFilter("");
            }}
          >
            重置
          </Button>
        </Box>

        <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <Box sx={{ display: "flex", gap: 1 }}>
            <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
              新建任务
            </Button>
            <Button variant="outlined" color="default" startIcon={<DeleteIcon />}>
              删除
            </Button>
          </Box>
          <Box sx={{ display: "flex", gap: 1 }}>
            <IconButton
              sx={{ border: 1, borderColor: "divider", borderRadius: 1.5, width: 38, height: 38 }}
            >
              <SearchIcon fontSize="small" />
            </IconButton>
            <IconButton
              sx={{ border: 1, borderColor: "divider", borderRadius: 1.5, width: 38, height: 38 }}
            >
              <RefreshIcon fontSize="small" />
            </IconButton>
          </Box>
        </Box>

        <DataTable<ReviewTask>
          columns={columns}
          data={filtered}
          rowKey={(r) => r.id}
          selectable
          tableId="ai-code-review-tasks"
          emptyAction={
            <Button variant="contained" size="small" startIcon={<AddIcon />} onClick={openCreate}>
              新建任务
            </Button>
          }
        />
        <CodeReviewViewDialogs view={view} />
      </PageStack>

      {/* Create / Edit dialog */}

      {/* Execution dialog */}

      {/* History report full view */}
      {historyOpen && (
        <Box
          sx={{
            position: "fixed",
            inset: 0,
            zIndex: 1300,
            bgcolor: "background.default",
            p: 3,
            overflow: "auto",
          }}
        >
          <Box
            sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mb: 2 }}
          >
            <Typography variant="h6">代码评审历史报告</Typography>
            <IconButton onClick={() => setHistoryOpen(false)}>
              <CloseIcon />
            </IconButton>
          </Box>
          <Box sx={{ display: "flex", gap: 2, height: "calc(100vh - 120px)" }}>
            <Paper variant="outlined" sx={{ width: 280, p: 2, overflowY: "auto" }}>
              <Typography variant="subtitle2" sx={{ mb: 1 }}>
                执行记录
              </Typography>
              {runHistory.length === 0 ? (
                <Box sx={{ textAlign: "center", py: 8 }}>
                  <FolderIcon sx={{ fontSize: 40, color: "text.disabled", mb: 1 }} />
                  <Typography variant="body2" sx={{ color: "text.secondary" }}>
                    暂无执行记录
                  </Typography>
                </Box>
              ) : (
                <Stack spacing={1}>
                  {runHistory.map((rec) => (
                    <Paper
                      key={rec.id}
                      variant="outlined"
                      onClick={() => setActiveRecord(rec)}
                      sx={{
                        p: 1.5,
                        cursor: "pointer",
                        borderColor: activeRecord?.id === rec.id ? "primary.main" : "divider",
                      }}
                    >
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
                        {rec.taskName}
                      </Typography>
                      <Typography variant="caption" sx={{ color: "text.secondary" }}>
                        {rec.finishedAt}
                      </Typography>
                    </Paper>
                  ))}
                </Stack>
              )}
            </Paper>
            <Paper variant="outlined" sx={{ flex: 1, p: 3, overflowY: "auto" }}>
              {!activeRecord ? (
                <Box
                  sx={{
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    justifyContent: "center",
                    height: "100%",
                    gap: 1,
                  }}
                >
                  <DoneIcon sx={{ fontSize: 48, color: "text.disabled" }} />
                  <Typography variant="body2" sx={{ color: "text.secondary" }}>
                    选择左侧记录查看报告
                  </Typography>
                </Box>
              ) : (
                <>
                  <Typography variant="h6" sx={{ mb: 1 }}>
                    代码评审报告
                  </Typography>
                  <Typography variant="body2" sx={{ color: "text.secondary", mb: 2 }}>
                    {activeRecord.taskName} · {activeRecord.finishedAt} · 耗时{" "}
                    {activeRecord.durationSec.toFixed(1)}s
                  </Typography>
                  <Divider sx={{ mb: 2 }} />
                  <Typography variant="body2" sx={{ whiteSpace: "pre-wrap", lineHeight: 1.7 }}>
                    {activeRecord.report}
                  </Typography>
                </>
              )}
            </Paper>
          </Box>
        </Box>
      )}
    </>
  );
}
