// SPDX-License-Identifier: Apache-2.0
// State and handlers of AiReports, split out of AiReports.tsx.

import { useState } from "react";
import { officeApi } from "@/shared/api/ai";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { modelApi } from "@/shared/api/ai";
import type { AiModelDto } from "@/shared/types";
import { toast } from "@/shared/pageKit/Toast";
import { ReportTpl } from "./part-1";

export function useAiReports() {
  const [tpls, setTpls] = useState<ReportTpl[]>([]);

  const [titleKw, setTitleKw] = useState("");

  const [promptKw, setPromptKw] = useState("");

  const [editing, setEditing] = useState<ReportTpl | null>(null);

  const [formOpen, setFormOpen] = useState(false);

  const [formName, setFormName] = useState("");

  const [formDesc, setFormDesc] = useState("");

  const [deleting, setDeleting] = useState<ReportTpl | null>(null);

  const [selectedId, setSelectedId] = useState<string | null>(null);

  const [workTpl, setWorkTpl] = useState<ReportTpl | null>(null);

  const [model, setModel] = useState("");

  const [polish, setPolish] = useState(true);

  const [prompt, setPrompt] = useState("");

  const [result, setResult] = useState("");

  const [generating, setGenerating] = useState(false);

  const { data: models } = useListQuery<AiModelDto>(
    ["api", "ai", "models", "list"],
    modelApi.list,
    {
      staleTime: 5 * 60 * 1000,
    },
  );

  const filtered = tpls.filter(
    (t) =>
      (!titleKw || t.name.toLowerCase().includes(titleKw.toLowerCase())) &&
      (!promptKw || t.description.toLowerCase().includes(promptKw.toLowerCase())),
  );

  function openCreate() {
    setEditing(null);
    setFormName("");
    setFormDesc("");
    setFormOpen(true);
  }

  function openEdit(t: ReportTpl) {
    setEditing(t);
    setFormName(t.name);
    setFormDesc(t.description);
    setFormOpen(true);
  }

  function saveForm() {
    if (!formName.trim()) {
      toast.error("请输入标题");
      return;
    }
    const now = new Date().toISOString().replace("T", " ").slice(0, 19);
    if (editing) {
      setTpls((prev) =>
        prev.map((t) =>
          t.id === editing.id ? { ...t, name: formName, description: formDesc, updatedAt: now } : t,
        ),
      );
      toast.success("已保存");
    } else {
      setTpls((prev) => [
        ...prev,
        { id: `r-${Date.now()}`, name: formName, description: formDesc, updatedAt: now },
      ]);
      toast.success("已创建");
    }
    setFormOpen(false);
  }

  function openWorkbench(t: ReportTpl) {
    setWorkTpl(t);
    setPrompt(t.description);
    setResult("");
  }

  async function generate() {
    if (!prompt.trim()) {
      toast.error("请输入报告要求");
      return;
    }
    setGenerating(true);
    setResult("");
    try {
      const res = await officeApi.run("report", prompt);
      setResult(res.content);
    } catch (e) {
      setResult(`生成失败: ${e instanceof Error ? e.message : String(e)}`);
    } finally {
      setGenerating(false);
    }
  }

  return {
    tpls,
    setTpls,
    titleKw,
    setTitleKw,
    promptKw,
    setPromptKw,
    editing,
    setEditing,
    formOpen,
    setFormOpen,
    formName,
    setFormName,
    formDesc,
    setFormDesc,
    deleting,
    setDeleting,
    selectedId,
    setSelectedId,
    workTpl,
    setWorkTpl,
    model,
    setModel,
    polish,
    setPolish,
    prompt,
    setPrompt,
    result,
    setResult,
    generating,
    setGenerating,
    models,
    filtered,
    openCreate,
    openEdit,
    saveForm,
    openWorkbench,
    generate,
  };
}
