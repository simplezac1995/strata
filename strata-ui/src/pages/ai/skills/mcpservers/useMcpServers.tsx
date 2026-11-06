// SPDX-License-Identifier: Apache-2.0
// State and handlers of McpServers, split out of McpServers.tsx.

import { useState } from "react";
import { useAuthStore } from "@/shared/auth/store";
import { mcpServerApi, type AiMcpServerPayload } from "@/shared/api/ai";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import { toast } from "@/shared/pageKit/Toast";
import type { AiMcpServerDto } from "@/shared/types";
import { McpForm, McpTool } from "./part-1";

export function useMcpServers() {
  const canCreate = useAuthStore((s) => s.hasAuthority("ai:mcp:create"));

  const canUpdate = useAuthStore((s) => s.hasAuthority("ai:mcp:update"));

  const canDelete = useAuthStore((s) => s.hasAuthority("ai:mcp:delete"));

  const [keyword, setKeyword] = useState("");

  const [toolsServer, setToolsServer] = useState<AiMcpServerDto | null>(null);

  const [tools, setTools] = useState<McpTool[]>([]);

  const [toolsLoading, setToolsLoading] = useState(false);

  const [testResult, setTestResult] = useState<string>("");

  const {
    rows: servers,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    reload,
  } = usePagedQuery<AiMcpServerDto>(["api", "ai", "mcp-servers"], mcpServerApi.page, {
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
  } = useResourceCrud<AiMcpServerDto, AiMcpServerPayload, AiMcpServerPayload, McpForm>({
    create: (payload) => mcpServerApi.create(payload),
    update: (id, payload) => mcpServerApi.update(id, payload),
    remove: (id) => mcpServerApi.remove(id),
    reload,
    initForm: () => ({
      name: "",
      description: "",
      type: "streamable-http",
      url: "",
      headers: "{}",
      enabled: true,
    }),
    toEditForm: (s) => ({
      name: s.name,
      description: ((s as Record<string, unknown>).description as string) ?? "",
      type: ((s as Record<string, unknown>).type as string) ?? "streamable-http",
      url: s.url ?? "",
      headers: ((s as Record<string, unknown>).headers as string) ?? "{}",
      enabled: s.enabled,
    }),
    toCreatePayload: (f) => f as unknown as AiMcpServerPayload,
    toUpdatePayload: (f) => f as unknown as AiMcpServerPayload,
  });

  const openTools = (s: AiMcpServerDto) => {
    setToolsServer(s);
    setTestResult("");
    setToolsLoading(true);
    setTools([]);
    mcpServerApi
      .tools(s.id)
      .then((res) => setTools(res))
      .catch(() => setTools([]))
      .finally(() => setToolsLoading(false));
  };

  const runTest = (id: string) => {
    setTestResult("testing");
    mcpServerApi
      .test(id)
      .then(() => {
        setTestResult("ok");
        toast.success("连接成功");
      })
      .catch(() => setTestResult("fail"));
  };

  const filtered = servers.filter(
    (s) => !keyword || s.name.toLowerCase().includes(keyword.toLowerCase()),
  );

  return {
    canCreate,
    canUpdate,
    canDelete,
    keyword,
    setKeyword,
    toolsServer,
    setToolsServer,
    tools,
    setTools,
    toolsLoading,
    setToolsLoading,
    testResult,
    setTestResult,
    servers,
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
    openTools,
    runTest,
    filtered,
  };
}
