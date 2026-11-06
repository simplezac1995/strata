// SPDX-License-Identifier: Apache-2.0
// State and handlers of Documents, split out of Documents.tsx.

import { Stack, Typography } from "@mui/material";
import { Button, Badge } from "@/components/ui/index";
import { useAuthStore } from "@/shared/auth/store";
import { documentApi, knowledgeApi } from "@/shared/api/ai";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import type { AiDocumentDto, AiKnowledgeDto } from "@/shared/types";
import { useMemo, useRef, useState } from "react";
import { Visibility, MoreVert } from "@mui/icons-material";
import { DOC_STATUS_VARIANT, DOC_STATUS_LABEL, DocumentForm } from "./part-1";

export function useDocuments() {
  const canCreate = useAuthStore((s) => s.hasAuthority("ai:document:create"));

  const canDelete = useAuthStore((s) => s.hasAuthority("ai:document:delete"));

  const { data: kbs } = useListQuery<AiKnowledgeDto>(
    ["api", "ai", "knowledge-bases", "list"],
    knowledgeApi.list,
    { staleTime: 5 * 60 * 1000 },
  );

  const [kbFilter, setKbFilter] = useState("");

  const [statusFilter, setStatusFilter] = useState("");

  const [keyword, setKeyword] = useState("");

  const [appliedKb, setAppliedKb] = useState("");

  const [appliedStatus, setAppliedStatus] = useState("");

  const [appliedKeyword, setAppliedKeyword] = useState("");

  const fileInputRef = useRef<HTMLInputElement>(null);

  const [uploading, setUploading] = useState(false);

  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null);

  const [menuDoc, setMenuDoc] = useState<AiDocumentDto | null>(null);

  const [detail, setDetail] = useState<AiDocumentDto | null>(null);

  const [detailTab, setDetailTab] = useState(0);

  const {
    rows: documents,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    reload,
  } = usePagedQuery<AiDocumentDto>(
    ["api", "ai", "documents", { knowledgeId: appliedKb }],
    (pageIndex, size) => documentApi.page(pageIndex, size, appliedKb || undefined),
    { initialPageSize: 6 },
  );

  const {
    busy,
    open,
    deleting,
    form,
    setForm,
    openCreate,
    closeDialog,
    handleSave,
    confirmDelete,
    closeDelete,
    runDelete,
  } = useResourceCrud<AiDocumentDto, DocumentForm, DocumentForm, DocumentForm>({
    create: (payload) => documentApi.create(payload as never),
    remove: (id) => documentApi.remove(id),
    reload,
    initForm: () => ({
      knowledgeId: kbs[0]?.id ?? "",
      name: "",
      sizeKb: 0,
      status: "pending",
      chunks: 0,
    }),
    toEditForm: (doc) => ({
      knowledgeId: doc.knowledgeId,
      name: doc.name,
      sizeKb: doc.sizeKb ?? 0,
      status: doc.status,
      chunks: doc.chunks ?? 0,
    }),
  });

  const kbNames = new Map(kbs.map((k) => [k.id, k.name]));

  const kbName = (id: string) => kbNames.get(id) ?? id;

  const rows = useMemo(
    () =>
      documents.filter((d) => {
        if (appliedStatus && d.status !== appliedStatus) return false;
        if (appliedKeyword && !d.name.toLowerCase().includes(appliedKeyword.toLowerCase()))
          return false;
        return true;
      }),
    [documents, appliedStatus, appliedKeyword],
  );

  const openDetail = (doc: AiDocumentDto) => {
    setDetailTab(0);
    setDetail(doc);
  };

  const cols = [
    {
      key: "name",
      title: "文件名",
      render: (doc: AiDocumentDto) => (
        <Typography variant="body2" sx={{ fontWeight: 500 }}>
          {doc.name}
        </Typography>
      ),
    },
    {
      key: "knowledgeId",
      title: "知识库",
      render: (doc: AiDocumentDto) => (
        <Typography variant="body2" sx={{ color: "text.secondary" }}>
          {kbName(doc.knowledgeId)}
        </Typography>
      ),
    },
    {
      key: "sizeKb",
      title: "大小",
      render: (doc: AiDocumentDto) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }}>
          {doc.sizeKb} KB
        </Typography>
      ),
    },
    {
      key: "chunks",
      title: "切片数",
      render: (doc: AiDocumentDto) => (
        <Typography variant="caption" sx={{ fontFamily: "monospace" }}>
          {doc.chunks ?? 0}
        </Typography>
      ),
    },
    {
      key: "status",
      title: "状态",
      render: (doc: AiDocumentDto) => (
        <Badge variant={DOC_STATUS_VARIANT[doc.status] ?? "default"}>
          {DOC_STATUS_LABEL[doc.status] ?? doc.status}
        </Badge>
      ),
    },
    {
      key: "createdAt",
      title: "更新时间",
      render: (doc: AiDocumentDto) => (
        <Typography variant="caption" sx={{ color: "text.secondary" }}>
          {String(doc.createdAt ?? "").slice(0, 10)}
        </Typography>
      ),
    },
    {
      key: "actions",
      title: "操作",
      render: (doc: AiDocumentDto) => (
        <Stack direction="row" spacing={0.5} sx={{ alignItems: "center" }}>
          <Button
            variant="text"
            size="sm"
            startIcon={<Visibility sx={{ fontSize: 16 }} />}
            onClick={() => openDetail(doc)}
          >
            文档
          </Button>
          <Button
            variant="text"
            size="sm"
            endIcon={<MoreVert sx={{ fontSize: 16 }} />}
            onClick={(e) => {
              setMenuDoc(doc);
              setMenuAnchor(e.currentTarget);
            }}
          >
            更多
          </Button>
        </Stack>
      ),
    },
  ];

  return {
    canCreate,
    canDelete,
    kbs,
    kbFilter,
    setKbFilter,
    statusFilter,
    setStatusFilter,
    keyword,
    setKeyword,
    appliedKb,
    setAppliedKb,
    appliedStatus,
    setAppliedStatus,
    appliedKeyword,
    setAppliedKeyword,
    fileInputRef,
    uploading,
    setUploading,
    menuAnchor,
    setMenuAnchor,
    menuDoc,
    setMenuDoc,
    detail,
    setDetail,
    detailTab,
    setDetailTab,
    documents,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    loading,
    reload,
    busy,
    open,
    deleting,
    form,
    setForm,
    openCreate,
    closeDialog,
    handleSave,
    confirmDelete,
    closeDelete,
    runDelete,
    kbNames,
    kbName,
    rows,
    openDetail,
    cols,
  };
}
