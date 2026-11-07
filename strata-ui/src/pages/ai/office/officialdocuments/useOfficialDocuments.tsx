// SPDX-License-Identifier: Apache-2.0
// State and handlers of OfficialDocuments, split out of OfficialDocuments.tsx.

import { useState } from "react";
import { officeApi } from "@/shared/api/ai";
import { toast } from "@/shared/pageKit/Toast";
import { DocTemplate } from "./part-1";

export function useOfficialDocuments() {
  const [templates, setTemplates] = useState<DocTemplate[]>([]);

  const [keyword, setKeyword] = useState("");

  const [editing, setEditing] = useState<DocTemplate | null>(null);

  const [formOpen, setFormOpen] = useState(false);

  const [formName, setFormName] = useState("");

  const [formPrompt, setFormPrompt] = useState("");

  const [deleting, setDeleting] = useState<DocTemplate | null>(null);

  const [selectedId, setSelectedId] = useState<string | null>(null);

  const [designerOpen, setDesignerOpen] = useState(false);

  const [designTitle, setDesignTitle] = useState("放假通知");

  const [showHeader, setShowHeader] = useState(true);

  const [writingOpen, setWritingOpen] = useState(false);

  const [topic, setTopic] = useState("");

  const [generated, setGenerated] = useState("");

  const [writing, setWriting] = useState(false);

  const filtered = templates.filter(
    (t) => !keyword || t.name.toLowerCase().includes(keyword.toLowerCase()),
  );

  function openCreate() {
    setEditing(null);
    setFormName("");
    setFormPrompt("");
    setFormOpen(true);
  }

  function openEdit(t: DocTemplate) {
    setEditing(t);
    setFormName(t.name);
    setFormPrompt(t.prompt);
    setFormOpen(true);
  }

  function saveForm() {
    if (!formName.trim()) {
      toast.error("请输入模板名称");
      return;
    }
    const now = new Date().toISOString().replace("T", " ").slice(0, 19);
    if (editing) {
      setTemplates((prev) =>
        prev.map((t) =>
          t.id === editing.id ? { ...t, name: formName, prompt: formPrompt, updatedAt: now } : t,
        ),
      );
      toast.success("模板已更新");
    } else {
      setTemplates((prev) => [
        ...prev,
        { id: `d-${Date.now()}`, name: formName, prompt: formPrompt, updatedAt: now },
      ]);
      toast.success("模板已创建");
    }
    setFormOpen(false);
  }

  function openDesigner(t: DocTemplate) {
    setDesignTitle(t.name);
    setDesignerOpen(true);
  }

  async function generateDoc() {
    if (!topic.trim()) {
      toast.error("请输入公文主题");
      return;
    }
    setWriting(true);
    setGenerated("");
    try {
      const res = await officeApi.run("generate", topic);
      setGenerated(res.content);
    } catch (e) {
      setGenerated(`生成失败: ${e instanceof Error ? e.message : String(e)}`);
    } finally {
      setWriting(false);
    }
  }

  return {
    templates,
    setTemplates,
    keyword,
    setKeyword,
    editing,
    setEditing,
    formOpen,
    setFormOpen,
    formName,
    setFormName,
    formPrompt,
    setFormPrompt,
    deleting,
    setDeleting,
    selectedId,
    setSelectedId,
    designerOpen,
    setDesignerOpen,
    designTitle,
    setDesignTitle,
    showHeader,
    setShowHeader,
    writingOpen,
    setWritingOpen,
    topic,
    setTopic,
    generated,
    setGenerated,
    writing,
    setWriting,
    filtered,
    openCreate,
    openEdit,
    saveForm,
    openDesigner,
    generateDoc,
  };
}
