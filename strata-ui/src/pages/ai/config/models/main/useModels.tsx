// SPDX-License-Identifier: Apache-2.0
// State and handlers of Models, split out of main.tsx.

import { useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Box, Chip, Stack, Typography } from "@mui/material";
import { Button, StatusChip } from "@/components/ui/index";
import { useAuthStore } from "@/shared/auth/store";
import {
  providerApi,
  modelApi,
  type AiProviderPayload,
  type AiModelPayload,
  type AiModelUpdatePayload,
} from "@/shared/api/ai";
import { useListQuery } from "@/shared/hooks/useListQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import type { AiProviderDto, AiModelDto } from "@/shared/types";
import { MODEL_TYPE_COLOR, TYPE_TABS, ProviderForm, ModelForm, PAGE_SIZE } from "../content";

export function useModels() {
  const queryClient = useQueryClient();

  const canCreateProvider = useAuthStore((s) => s.hasAuthority("ai:provider:create"));

  const canCreate = useAuthStore((s) => s.hasAuthority("ai:model:create"));

  const canUpdate = useAuthStore((s) => s.hasAuthority("ai:model:update"));

  const canDelete = useAuthStore((s) => s.hasAuthority("ai:model:delete"));

  const [selectedProviderId, setSelectedProviderId] = useState<string>("all");

  const [typeTab, setTypeTab] = useState<string>("all");

  const [providerKeyword, setProviderKeyword] = useState("");

  const [page, setPage] = useState(1);

  const { data: providers, loading: providersLoading } = useListQuery<AiProviderDto>(
    ["api", "ai", "providers", "list"],
    () => providerApi.list(),
    { staleTime: 5 * 60 * 1000 },
  );

  const { data: models, loading: modelsLoading } = useListQuery<AiModelDto>(
    ["api", "ai", "models", "list"],
    () => modelApi.list(),
    { staleTime: 5 * 60 * 1000 },
  );

  const providerCrud = useResourceCrud<
    AiProviderDto,
    AiProviderPayload,
    AiProviderPayload,
    ProviderForm
  >({
    create: (payload) => providerApi.create(payload),
    update: (id, payload) => providerApi.update(id, payload),
    remove: (id) => providerApi.remove(id),
    reload: () => void queryClient.invalidateQueries({ queryKey: ["api", "ai", "providers"] }),
    initForm: () => ({
      code: "",
      name: "",
      type: "openai",
      baseUrl: "",
      apiKey: "",
      enabled: true,
    }),
    toEditForm: (p) => ({
      code: p.code,
      name: p.name,
      type: p.type ?? "openai",
      baseUrl: p.baseUrl ?? "",
      apiKey: p.apiKey ?? "",
      enabled: p.enabled,
    }),
    toCreatePayload: (f) => f as unknown as AiProviderPayload,
    toUpdatePayload: (f) => f as unknown as AiProviderPayload,
  });

  const {
    busy: modelBusy,
    open: modelOpen,
    edit: modelEdit,
    deleting: modelDeleting,
    form: modelForm,
    setForm: setModelForm,
    openCreate: openModelCreate,
    openEdit: openModelEdit,
    closeDialog: closeModelDialog,
    handleSave: saveModel,
    confirmDelete: confirmModelDelete,
    closeDelete: closeModelDelete,
    runDelete: runModelDelete,
  } = useResourceCrud<AiModelDto, AiModelPayload, AiModelUpdatePayload, ModelForm>({
    create: (payload) => modelApi.create(payload),
    update: (id, payload) => modelApi.update(id, payload),
    remove: (id) => modelApi.remove(id),
    reload: () => void queryClient.invalidateQueries({ queryKey: ["api", "ai", "models"] }),
    initForm: () => ({
      providerId: providers[0]?.id ?? "",
      code: "",
      name: "",
      type: "chat",
      enabled: true,
    }),
    toEditForm: (m) => ({
      providerId: m.providerId,
      code: m.code,
      name: m.name,
      type: m.type ?? "chat",
      enabled: m.enabled,
    }),
    toCreatePayload: (f) => f as unknown as AiModelPayload,
    toUpdatePayload: (f) => f as unknown as AiModelUpdatePayload,
  });

  const providerName = (id: string) => providers.find((p) => p.id === id)?.name ?? id;

  const filteredProviders = useMemo(() => {
    const kw = providerKeyword.trim().toLowerCase();
    if (!kw) return providers;
    return providers.filter(
      (p) => p.name.toLowerCase().includes(kw) || p.code.toLowerCase().includes(kw),
    );
  }, [providers, providerKeyword]);

  const filteredModels = useMemo(() => {
    return models.filter((m) => {
      if (selectedProviderId !== "all" && m.providerId !== selectedProviderId) return false;
      if (typeTab !== "all" && m.type !== typeTab) return false;
      return true;
    });
  }, [models, selectedProviderId, typeTab]);

  const pagedModels = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filteredModels.slice(start, start + PAGE_SIZE);
  }, [filteredModels, page]);

  const onSelectProvider = (id: string) => {
    setSelectedProviderId(id);
    setPage(1);
  };

  const onSelectType = (v: string) => {
    setTypeTab(v);
    setPage(1);
  };

  const cols = [
    {
      key: "providerId",
      title: "厂商",
      render: (m: AiModelDto) => (
        <Typography variant="body2" sx={{ fontWeight: 600 }}>
          {providerName(m.providerId)}
        </Typography>
      ),
    },
    {
      key: "type",
      title: "类型",
      render: (m: AiModelDto) => (
        <Chip
          label={TYPE_TABS.find((t) => t.value === m.type)?.label ?? m.type}
          size="small"
          color={MODEL_TYPE_COLOR[m.type] ?? "default"}
          variant="outlined"
        />
      ),
    },
    {
      key: "code",
      title: "模型",
      render: (m: AiModelDto) => (
        <Box component="code" sx={{ fontSize: 13, fontFamily: "monospace", fontWeight: 600 }}>
          {m.code}
        </Box>
      ),
    },
    {
      key: "name",
      title: "平台别名",
      render: (m: AiModelDto) => (
        <Typography variant="caption" sx={{ color: "text.secondary", fontFamily: "monospace" }}>
          {m.name}
        </Typography>
      ),
    },
    {
      key: "enabled",
      title: "启用",
      render: (m: AiModelDto) => <StatusChip enabled={m.enabled} />,
    },
    {
      key: "default",
      title: "默认",
      render: () => <Chip label="否" size="small" variant="outlined" color="default" />,
    },
    {
      key: "actions",
      title: "操作",
      render: (m: AiModelDto) => (
        <Stack direction="row" spacing={0.5}>
          {canUpdate && (
            <Button variant="text" size="sm" onClick={() => openModelEdit(m)}>
              编辑
            </Button>
          )}
          {canDelete && (
            <Button variant="text" size="sm" color="danger" onClick={() => confirmModelDelete(m)}>
              删除
            </Button>
          )}
        </Stack>
      ),
    },
  ];

  return {
    queryClient,
    canCreateProvider,
    canCreate,
    canUpdate,
    canDelete,
    selectedProviderId,
    setSelectedProviderId,
    typeTab,
    setTypeTab,
    providerKeyword,
    setProviderKeyword,
    page,
    setPage,
    providers,
    providersLoading,
    models,
    modelsLoading,
    providerCrud,
    modelBusy,
    modelOpen,
    modelEdit,
    modelDeleting,
    modelForm,
    setModelForm,
    openModelCreate,
    openModelEdit,
    closeModelDialog,
    saveModel,
    confirmModelDelete,
    closeModelDelete,
    runModelDelete,
    providerName,
    filteredProviders,
    filteredModels,
    pagedModels,
    onSelectProvider,
    onSelectType,
    cols,
  };
}
