// SPDX-License-Identifier: Apache-2.0
// State and handlers of KnowledgeBases, split out of KnowledgeBases.tsx.

import { useState } from "react";
import { useAuthStore } from "@/shared/auth/store";
import { knowledgeApi, type AiKnowledgePayload } from "@/shared/api/ai";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import type { AiKnowledgeDto } from "@/shared/types";
import { KnowledgeForm } from "./part-1";

export function useKnowledgeBases() {
  const canCreate = useAuthStore((s) => s.hasAuthority("ai:knowledge:create"));

  const canUpdate = useAuthStore((s) => s.hasAuthority("ai:knowledge:update"));

  const canDelete = useAuthStore((s) => s.hasAuthority("ai:knowledge:delete"));

  const [modeOpen, setModeOpen] = useState(false);

  const [step, setStep] = useState(1);

  const [keyword, setKeyword] = useState("");

  const [shareKb, setShareKb] = useState<AiKnowledgeDto | null>(null);

  const {
    rows: kbs,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    reload,
  } = usePagedQuery<AiKnowledgeDto>(["api", "ai", "knowledge-bases"], knowledgeApi.page, {
    initialPageSize: 6,
  });

  const {
    busy,
    open,
    edit,
    deleting,
    form,
    setForm,
    openCreate,
    openEdit,
    closeDialog,
    handleSave,
    confirmDelete,
    closeDelete,
    runDelete,
  } = useResourceCrud<AiKnowledgeDto, AiKnowledgePayload, AiKnowledgePayload, KnowledgeForm>({
    create: (payload) => knowledgeApi.create(payload),
    update: (id, payload) => knowledgeApi.update(id, payload),
    remove: (id) => knowledgeApi.remove(id),
    reload,
    initForm: () => ({
      name: "",
      description: "",
      mode: "q2a",
      welcome: "",
      vectorStore: "pgvector",
      embeddingModel: "text-embedding-3-large",
      chunkSize: 512,
      chunkOverlap: 50,
      enabled: true,
    }),
    toEditForm: (kb) => ({
      name: kb.name,
      description: kb.description ?? "",
      mode: "q2a",
      welcome: "",
      vectorStore: kb.vectorStore ?? "pgvector",
      embeddingModel: kb.embeddingModel ?? "text-embedding-3-large",
      chunkSize: kb.chunkSize ?? 512,
      chunkOverlap: kb.chunkOverlap ?? 50,
      enabled: kb.enabled,
    }),
    toCreatePayload: (f) => ({
      name: f.name,
      description: f.description || null,
      embeddingModel: f.embeddingModel || null,
      vectorStore: f.vectorStore || null,
      chunkSize: f.chunkSize,
      chunkOverlap: f.chunkOverlap,
      enabled: f.enabled,
    }),
    toUpdatePayload: (f) => ({
      name: f.name,
      description: f.description || null,
      embeddingModel: f.embeddingModel || null,
      vectorStore: f.vectorStore || null,
      chunkSize: f.chunkSize,
      chunkOverlap: f.chunkOverlap,
      enabled: f.enabled,
    }),
  });

  const visible = kbs.filter(
    (kb) => !keyword || kb.name.toLowerCase().includes(keyword.toLowerCase()),
  );

  const pickMode = (mode: "q2a" | "q2q") => {
    setModeOpen(false);
    setStep(1);
    openCreate();
    setForm((f) => ({ ...f, mode }));
  };

  const startEdit = (kb: AiKnowledgeDto) => {
    setStep(1);
    openEdit(kb);
  };

  return {
    canCreate,
    canUpdate,
    canDelete,
    modeOpen,
    setModeOpen,
    step,
    setStep,
    keyword,
    setKeyword,
    shareKb,
    setShareKb,
    kbs,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    reload,
    busy,
    open,
    edit,
    deleting,
    form,
    setForm,
    openCreate,
    openEdit,
    closeDialog,
    handleSave,
    confirmDelete,
    closeDelete,
    runDelete,
    visible,
    pickMode,
    startEdit,
  };
}
