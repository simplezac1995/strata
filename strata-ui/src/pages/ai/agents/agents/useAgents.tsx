// SPDX-License-Identifier: Apache-2.0
// State and handlers of Agents, split out of Agents.tsx.

import { useState } from "react";
import { agentApi, type AiAgentPayload } from "@/shared/api/ai";
import type { AiAgentDto } from "@/shared/types";
import { failureMessage } from "@/shared/hooks/useCrud";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { toast } from "@/shared/pageKit/Toast";
import { ShareTab } from "./part-1";

export function useAgents() {
  const [keyword, setKeyword] = useState("");

  const [menuAnchor, setMenuAnchor] = useState<null | { agent: AiAgentDto; el: HTMLElement }>(null);

  const [formOpen, setFormOpen] = useState(false);

  const [editing, setEditing] = useState<AiAgentDto | null>(null);

  const [name, setName] = useState("");

  const [desc, setDesc] = useState("");

  const [secret, setSecret] = useState("");

  const [saving, setSaving] = useState(false);

  const [shareOpen, setShareOpen] = useState(false);

  const [shareAgent, setShareAgent] = useState<AiAgentDto | null>(null);

  const [shareTab, setShareTab] = useState<ShareTab>("link");

  const [deleteTarget, setDeleteTarget] = useState<AiAgentDto | null>(null);

  const { rows, total, page, setPage, pageSize, setPageSize, loading, reload } =
    usePagedQuery<AiAgentDto>(
      ["api", "ai", "agents", { kw: keyword }],
      (p, s) => agentApi.page(p, s),
      { initialPageSize: 6 },
    );

  const openCreate = () => {
    setEditing(null);
    setName("");
    setDesc("");
    setSecret("");
    setFormOpen(true);
  };

  const openEdit = (a: AiAgentDto) => {
    setEditing(a);
    setName(a.name);
    setDesc(a.prompt ?? "");
    setSecret("");
    setFormOpen(true);
  };

  const handleSave = async () => {
    if (!name.trim()) {
      toast.info("请输入编排名称");
      return;
    }
    setSaving(true);
    try {
      const payload: AiAgentPayload = {
        name: name.trim(),
        model: editing?.model ?? null,
        prompt: desc || null,
        enabled: true,
      };
      if (editing) {
        await agentApi.update(editing.id, payload);
        toast.success("已保存");
      } else {
        await agentApi.create(payload);
        toast.success("已创建");
      }
      setFormOpen(false);
      reload();
    } catch (error) {
      toast.error(failureMessage(error));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await agentApi.remove(deleteTarget.id);
      toast.success("已删除");
      reload();
    } catch (error) {
      toast.error(failureMessage(error));
    } finally {
      setDeleteTarget(null);
    }
  };

  const openShare = (a: AiAgentDto) => {
    setShareAgent(a);
    setShareTab("link");
    setShareOpen(true);
  };

  const shareUrl = shareAgent
    ? `${window.location.origin}/bot/index.html#/flow/${shareAgent.id}/chat`
    : "";

  const iframeCode = shareAgent
    ? `<iframe src="${shareUrl}" width="400" height="600" frameborder="0"></iframe>`
    : "";

  const scriptCode = shareAgent
    ? `<script src="${window.location.origin}/bot/widget.js" data-flow="${shareAgent.id}"></script>`
    : "";

  return {
    keyword,
    setKeyword,
    menuAnchor,
    setMenuAnchor,
    formOpen,
    setFormOpen,
    editing,
    setEditing,
    name,
    setName,
    desc,
    setDesc,
    secret,
    setSecret,
    saving,
    setSaving,
    shareOpen,
    setShareOpen,
    shareAgent,
    setShareAgent,
    shareTab,
    setShareTab,
    deleteTarget,
    setDeleteTarget,
    rows,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    reload,
    openCreate,
    openEdit,
    handleSave,
    handleDelete,
    openShare,
    shareUrl,
    iframeCode,
    scriptCode,
  };
}
