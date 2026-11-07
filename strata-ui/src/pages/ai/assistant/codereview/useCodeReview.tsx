// SPDX-License-Identifier: Apache-2.0
// State and handlers of CodeReview, split out of CodeReview.tsx.

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
import { ReviewTask, RunLog, RunRecord, TIME_RANGES, EMPTY_FORM, RunPhase } from "./part-1";

export function useCodeReview() {
  const [tasks, setTasks] = useState<ReviewTask[]>([]);

  const [keyword, setKeyword] = useState("");

  const [statusFilter, setStatusFilter] = useState("");

  const [formOpen, setFormOpen] = useState(false);

  const [editing, setEditing] = useState<ReviewTask | null>(null);

  const [form, setForm] = useState({ ...EMPTY_FORM });

  const [deleting, setDeleting] = useState<ReviewTask | null>(null);

  const [runTask, setRunTask] = useState<ReviewTask | null>(null);

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
    { key: "name", title: "任务名称", align: "left" as const, render: (r: ReviewTask) => r.name },
    {
      key: "gitUrl",
      title: "Git地址",
      align: "left" as const,
      render: (r: ReviewTask) => r.gitUrl,
    },
    { key: "branch", title: "分支", render: (r: ReviewTask) => r.branch },
    {
      key: "timeRange",
      title: "时间范围",
      render: (r: ReviewTask) =>
        TIME_RANGES.find((x) => x.value === r.timeRange)?.label ?? r.timeRange,
    },
    {
      key: "status",
      title: "状态",
      render: (r: ReviewTask) => (
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
      render: (r: ReviewTask) => r.lastRunAt || "-",
    },
    {
      key: "createdAt",
      title: "创建时间",
      align: "left" as const,
      render: (r: ReviewTask) => r.createdAt,
    },
    {
      key: "actions",
      title: "操作",
      width: 280,
      render: (r: ReviewTask) => (
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

  function openEdit(t: ReviewTask) {
    setEditing(t);
    setForm({
      name: t.name,
      enabled: t.enabled,
      gitUrl: t.gitUrl,
      branch: t.branch,
      timeRange: t.timeRange,
      username: "",
      token: "",
      systemPrompt: t.systemPrompt,
    });
    setFormOpen(true);
  }

  function saveForm() {
    if (!form.name.trim()) {
      toast.error("请输入任务名称");
      return;
    }
    if (!form.gitUrl.trim()) {
      toast.error("请输入 Git 地址");
      return;
    }
    const now = new Date().toISOString().replace("T", " ").slice(0, 19);
    if (editing) {
      setTasks((prev) => prev.map((t) => (t.id === editing.id ? { ...t, ...form } : t)));
      toast.success("评审任务已更新");
    } else {
      setTasks((prev) => [
        ...prev,
        { id: `c-${Date.now()}`, ...form, lastRunAt: "", createdAt: now },
      ]);
      toast.success("评审任务已创建");
    }
    setFormOpen(false);
  }

  function openRun(t: ReviewTask) {
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
    const stepCommands: { tool: string; command: string }[] = [
      { tool: "Bash", command: `git clone ${runTask.gitUrl} /tmp/repo` },
      { tool: "Bash", command: `git checkout ${runTask.branch}` },
      { tool: "Glob", command: "src/**/*.{ts,tsx,java}" },
      { tool: "Read", command: "读取变更文件..." },
      { tool: "Grep", command: 'grep -rn "TODO|FIXME|HACK" src/' },
    ];
    stepCommands.forEach((step, i) => {
      setTimeout(
        () => {
          const time = new Date().toTimeString().slice(0, 8);
          setLogs((prev) => [...prev, { time, tool: step.tool, command: step.command }]);
        },
        (i + 1) * 450,
      );
    });
    try {
      const payload = `仓库: ${runTask.gitUrl}\n分支: ${runTask.branch}\n时间范围: ${runTask.timeRange}\n\n请对上述代码变更执行评审。`;
      const res = await aiToolApi.run("code-review", payload);
      const dur = Math.round(((Date.now() - startedAt) / 1000) * 10) / 10;
      setReport(res.result || "评审完成，未发现明显问题。");
      setDuration(dur);
      setPhase("done");
      setTasks((prev) =>
        prev.map((t) =>
          t.id === runTask.id
            ? { ...t, lastRunAt: new Date().toISOString().replace("T", " ").slice(0, 19) }
            : t,
        ),
      );
      const record: RunRecord = {
        id: `cr-${Date.now()}`,
        taskName: runTask.name,
        finishedAt: new Date().toISOString().replace("T", " ").slice(0, 19),
        durationSec: dur,
        logs: stepCommands.map((step, i) => ({
          time: `00:45:0${8 + i}`,
          tool: step.tool,
          command: step.command,
        })),
        report: res.result || "评审完成，未发现明显问题。",
      };
      setRunHistory((prev) => [record, ...prev]);
      toast.success("评审完成");
    } catch (e) {
      const dur = Math.round(((Date.now() - startedAt) / 1000) * 10) / 10;
      setReport(`评审失败: ${e instanceof Error ? e.message : String(e)}`);
      setDuration(dur);
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
    openRun,
    openHistory,
    startRun,
  };
}
