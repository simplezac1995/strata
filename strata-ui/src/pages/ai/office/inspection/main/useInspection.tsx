// SPDX-License-Identifier: Apache-2.0
// State and handlers of Inspection, split out of main.tsx.

import { useMemo, useState } from "react";
import {
  DeleteOutlined as DeleteIcon,
  Edit as EditIcon,
  PlayArrow as RunIcon,
  Description as ReportIcon,
} from "@mui/icons-material";
import { Chip, Stack } from "@mui/material";
import { Button } from "@/components/ui/index";
import { aiToolApi } from "@/shared/api/ai";
import { toast } from "@/shared/pageKit/Toast";
import { InspectionTask, RunLog, RunRecord, TIME_RANGES, EMPTY_FORM, RunPhase } from "../model";

export function useInspection() {
  const [tasks, setTasks] = useState<InspectionTask[]>([]);

  const [keyword, setKeyword] = useState("");

  const [statusFilter, setStatusFilter] = useState("");

  const [formOpen, setFormOpen] = useState(false);

  const [editing, setEditing] = useState<InspectionTask | null>(null);

  const [form, setForm] = useState({ ...EMPTY_FORM });

  const [deleting, setDeleting] = useState<InspectionTask | null>(null);

  const [runTask, setRunTask] = useState<InspectionTask | null>(null);

  const [phase, setPhase] = useState<RunPhase>("idle");

  const [logs, setLogs] = useState<RunLog[]>([]);

  const [report, setReport] = useState("");

  const [duration, setDuration] = useState(0);

  const [historyOpen, setHistoryOpen] = useState(false);

  const [runHistory, setRunHistory] = useState<RunRecord[]>([]);

  const [activeRecord, setActiveRecord] = useState<RunRecord | null>(null);

  const filtered = useMemo(
    () =>
      tasks.filter(
        (t) =>
          (!keyword || t.name.toLowerCase().includes(keyword.toLowerCase())) &&
          (!statusFilter || (statusFilter === "enabled" ? t.enabled : !t.enabled)),
      ),
    [tasks, keyword, statusFilter],
  );

  const columns = [
    {
      key: "name",
      title: "任务名称",
      align: "left" as const,
      render: (r: InspectionTask) => r.name,
    },
    {
      key: "logDir",
      title: "日志目录",
      align: "left" as const,
      render: (r: InspectionTask) => r.logDir,
    },
    { key: "fileMatch", title: "文件匹配", render: (r: InspectionTask) => r.fileMatch },
    {
      key: "timeRange",
      title: "时间范围",
      render: (r: InspectionTask) =>
        TIME_RANGES.find((x) => x.value === r.timeRange)?.label ?? r.timeRange,
    },
    {
      key: "status",
      title: "状态",
      render: (r: InspectionTask) => (
        <Chip
          label={r.enabled ? "启用" : "停用"}
          size="small"
          color={r.enabled ? "success" : "default"}
          variant="outlined"
        />
      ),
    },
    {
      key: "lastRunAt",
      title: "最近执行",
      align: "left" as const,
      render: (r: InspectionTask) => r.lastRunAt || "-",
    },
    {
      key: "createdAt",
      title: "创建时间",
      align: "left" as const,
      render: (r: InspectionTask) => r.createdAt,
    },
    {
      key: "actions",
      title: "操作",
      width: 280,
      render: (r: InspectionTask) => (
        <Stack direction="row" spacing={0.5}>
          <Button
            variant="text"
            size="small"
            color="primary"
            startIcon={<RunIcon fontSize="small" />}
            onClick={() => openRun(r)}
          >
            执行
          </Button>
          <Button
            variant="text"
            size="small"
            color="primary"
            startIcon={<ReportIcon fontSize="small" />}
            onClick={() => openHistory()}
          >
            报告
          </Button>
          <Button
            variant="text"
            size="small"
            color="primary"
            startIcon={<EditIcon fontSize="small" />}
            onClick={() => openEdit(r)}
          >
            编辑
          </Button>
          <Button
            variant="text"
            size="small"
            color="danger"
            startIcon={<DeleteIcon fontSize="small" />}
            onClick={() => setDeleting(r)}
          >
            删除
          </Button>
        </Stack>
      ),
    },
  ];

  function openCreate() {
    setEditing(null);
    setForm({ ...EMPTY_FORM });
    setFormOpen(true);
  }

  function openEdit(t: InspectionTask) {
    setEditing(t);
    setForm({
      name: t.name,
      enabled: t.enabled,
      logDir: t.logDir,
      fileMatch: t.fileMatch,
      timeRange: t.timeRange,
      systemPrompt: t.systemPrompt,
    });
    setFormOpen(true);
  }

  function saveForm() {
    if (!form.name.trim()) {
      toast.error("请输入任务名称");
      return;
    }
    if (!form.logDir.trim()) {
      toast.error("请输入日志目录");
      return;
    }
    if (editing) {
      setTasks((prev) => prev.map((t) => (t.id === editing.id ? { ...t, ...form } : t)));
      toast.success("巡检任务已更新");
    } else {
      const now = new Date().toISOString().replace("T", " ").slice(0, 19);
      setTasks((prev) => [
        ...prev,
        { id: `t-${Date.now()}`, ...form, lastRunAt: "", createdAt: now },
      ]);
      toast.success("巡检任务已创建");
    }
    setFormOpen(false);
  }

  function confirmDelete() {
    if (!deleting) return;
    setTasks((prev) => prev.filter((t) => t.id !== deleting.id));
    setDeleting(null);
    toast.success("已删除");
  }

  function openRun(t: InspectionTask) {
    setRunTask(t);
    setPhase("idle");
    setLogs([]);
    setReport("");
    setDuration(0);
  }

  function openHistory() {
    setActiveRecord(runHistory[0] ?? null);
    setHistoryOpen(true);
  }

  async function startRun() {
    if (!runTask) return;
    setPhase("analyzing");
    setLogs([]);
    setReport("");
    const startedAt = Date.now();
    // Simulated on-host collection steps while the real LLM analysis runs.
    const stepCommands = [
      `ls -la ${runTask.logDir}`,
      `stat -f "%Sm" -t "%Y-%m-%d %H:%M:%S" ${runTask.logDir}`,
      `wc -l ${runTask.logDir}/${runTask.fileMatch}`,
      `grep -n "ERROR" ${runTask.logDir}/${runTask.fileMatch}`,
      `grep -n "WARN" ${runTask.logDir}/${runTask.fileMatch}`,
    ];
    stepCommands.forEach((cmd, i) => {
      setTimeout(
        () => {
          const time = new Date().toTimeString().slice(0, 8);
          setLogs((prev) => [...prev, { time, tool: "Bash", command: cmd }]);
        },
        (i + 1) * 450,
      );
    });
    try {
      const payload = `任务: ${runTask.name}\n日志目录: ${runTask.logDir}\n文件匹配: ${runTask.fileMatch}\n时间范围: ${runTask.timeRange}\n\n请对上述日志执行安全巡检与异常分析。`;
      const res = await aiToolApi.run("safety", payload);
      setReport(res.result || "巡检完成，未发现异常。");
      setDuration(Math.round(((Date.now() - startedAt) / 1000) * 10) / 10);
      setPhase("done");
      setTasks((prev) =>
        prev.map((t) =>
          t.id === runTask.id
            ? { ...t, lastRunAt: new Date().toISOString().replace("T", " ").slice(0, 19) }
            : t,
        ),
      );
      const record: RunRecord = {
        id: `r-${Date.now()}`,
        taskName: runTask.name,
        finishedAt: new Date().toISOString().replace("T", " ").slice(0, 19),
        durationSec: Math.round(((Date.now() - startedAt) / 1000) * 10) / 10,
        logs: stepCommands.map((cmd, i) => ({
          time: `00:31:0${7 + i}`,
          tool: "Bash",
          command: cmd,
        })),
        report: res.result || "巡检完成，未发现异常。",
      };
      setRunHistory((prev) => [record, ...prev]);
      toast.success("巡检完成");
    } catch (e) {
      setReport(`巡检失败: ${e instanceof Error ? e.message : String(e)}`);
      setDuration(Math.round(((Date.now() - startedAt) / 1000) * 10) / 10);
      setPhase("done");
    }
  }

  return {
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
  };
}
