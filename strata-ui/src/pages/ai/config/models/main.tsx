// SPDX-License-Identifier: Apache-2.0
import { Box, Chip, Divider, Stack, Typography } from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import RefreshIcon from "@mui/icons-material/Refresh";
import SearchIcon from "@mui/icons-material/Search";
import {
  Button,
  TextField,
  Select,
  Dialog,
  ConfirmDialog,
  DataTable,
  Pagination,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { AiPageHeader } from "@/components/ui/index";
import { TYPE_TABS, PROVIDER_TYPES, PAGE_SIZE } from "./content";
import { ProviderRow } from "./ProviderRow";
import { useModels } from "./main/useModels";

export function Models() {
  const {
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
  } = useModels();

  return (
    <PageStack>
      <AiPageHeader
        title="模型配置"
        subtitle="管理模型厂商与可用模型；左侧选择厂商，右侧查看该厂商下的模型。"
        actions={
          canCreate ? (
            <Button variant="contained" onClick={openModelCreate}>
              添加模型
            </Button>
          ) : undefined
        }
      />

      <Box
        sx={{
          display: "flex",
          gap: 2,
          alignItems: "stretch",
          flexWrap: { xs: "wrap", lg: "nowrap" },
        }}
      >
        {/* ── Left: provider panel ── */}
        <Box
          sx={{
            width: { xs: "100%", lg: 280 },
            flexShrink: 0,
            display: "flex",
            flexDirection: "column",
            bgcolor: "background.paper",
            borderRadius: 1.5,
            border: 1,
            borderColor: "divider",
            p: 1.5,
          }}
        >
          <Stack direction="row" spacing={1} sx={{ mb: 1.5 }}>
            <TextField
              placeholder="搜索厂商或模型"
              value={providerKeyword}
              onChange={(e) => setProviderKeyword(e.target.value)}
              slotProps={{
                input: { startAdornment: <SearchIcon sx={{ fontSize: 18, mr: 0.5 }} /> },
              }}
            />
            <Button variant="outlined" color="default" onClick={() => setProviderKeyword("")}>
              重置
            </Button>
          </Stack>

          <Box sx={{ flex: 1, overflowY: "auto", maxHeight: 460 }}>
            <ProviderRow
              active={selectedProviderId === "all"}
              name="全部厂商"
              sub={`${providers.length} 个`}
              onClick={() => onSelectProvider("all")}
            />
            {filteredProviders.map((p) => (
              <ProviderRow
                key={p.id}
                active={selectedProviderId === p.id}
                name={p.name}
                sub={p.code}
                onClick={() => onSelectProvider(p.id)}
                onAdd={canCreateProvider ? () => providerCrud.openCreate() : undefined}
              />
            ))}
            {!providersLoading && filteredProviders.length === 0 && (
              <Typography
                variant="caption"
                sx={{ color: "text.secondary", px: 1, py: 2, display: "block" }}
              >
                暂无厂商
              </Typography>
            )}
          </Box>

          <Divider sx={{ my: 1 }} />
          <Stack direction="row" spacing={1}>
            {canCreateProvider && (
              <Button variant="contained" onClick={providerCrud.openCreate}>
                <AddIcon sx={{ fontSize: 18, mr: 0.5 }} />
                添加厂商
              </Button>
            )}
            <Button variant="outlined" color="default" disabled title="同步缓存（后端未开放）">
              <RefreshIcon sx={{ fontSize: 18, mr: 0.5 }} />
              同步缓存
            </Button>
          </Stack>
        </Box>

        {/* ── Right: type tabs + model table ── */}
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Box
            sx={{
              display: "flex",
              flexWrap: "wrap",
              gap: 0.5,
              mb: 1.5,
              p: 1,
              bgcolor: "background.paper",
              borderRadius: 1.5,
              border: 1,
              borderColor: "divider",
            }}
          >
            {TYPE_TABS.map((t) => (
              <Chip
                key={t.value}
                label={t.label}
                onClick={() => onSelectType(t.value)}
                variant={typeTab === t.value ? "filled" : "outlined"}
                color={typeTab === t.value ? "primary" : "default"}
                sx={{ cursor: "pointer" }}
              />
            ))}
          </Box>

          <DataTable
            columns={cols}
            data={pagedModels}
            loading={modelsLoading}
            rowKey={(m) => m.id}
          />

          <Stack
            direction="row"
            sx={{ alignItems: "center", justifyContent: "flex-end", mt: 1.5, gap: 1 }}
          >
            <Typography variant="caption" sx={{ color: "text.secondary" }}>
              共 {filteredModels.length} 条
            </Typography>
            <Pagination
              page={page}
              pageSize={PAGE_SIZE}
              total={filteredModels.length}
              onPage={setPage}
              onPageSize={() => undefined}
            />
          </Stack>
        </Box>
      </Box>

      {/* ── 添加厂商 dialog ── */}
      <Dialog
        open={providerCrud.open}
        onClose={providerCrud.closeDialog}
        title={providerCrud.edit ? "编辑厂商" : "添加厂商"}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label="厂商编码"
            value={providerCrud.form.code}
            onChange={(e) => providerCrud.setForm((f) => ({ ...f, code: e.target.value }))}
            placeholder="请输入大小写英文字母或中划线，例如 My-Provider"
          />
          <TextField
            label="厂商名称"
            value={providerCrud.form.name}
            onChange={(e) => providerCrud.setForm((f) => ({ ...f, name: e.target.value }))}
            placeholder="例如 DeepSeek"
          />
          <Select
            label="厂商类型"
            value={providerCrud.form.type}
            onChange={(e) => providerCrud.setForm((f) => ({ ...f, type: e.target.value }))}
            options={PROVIDER_TYPES}
          />
          <TextField
            label="Base URL"
            value={providerCrud.form.baseUrl}
            onChange={(e) => providerCrud.setForm((f) => ({ ...f, baseUrl: e.target.value }))}
            placeholder="https://api.example.com/v1"
          />
          <TextField
            label="API Key"
            type="password"
            value={providerCrud.form.apiKey}
            onChange={(e) => providerCrud.setForm((f) => ({ ...f, apiKey: e.target.value }))}
            placeholder="请输入 API Key"
          />
          <Select
            label="状态"
            value={providerCrud.form.enabled ? "true" : "false"}
            onChange={(e) =>
              providerCrud.setForm((f) => ({ ...f, enabled: e.target.value === "true" }))
            }
            options={[
              { value: "true", label: "启用" },
              { value: "false", label: "停用" },
            ]}
          />
        </Stack>
        <Box sx={{ display: "flex", gap: 1, justifyContent: "flex-end", mt: 2 }}>
          <Button variant="outlined" color="default" onClick={providerCrud.closeDialog}>
            取消
          </Button>
          <Button
            variant="contained"
            disabled={providerCrud.busy}
            onClick={() => void providerCrud.handleSave()}
          >
            确认
          </Button>
        </Box>
      </Dialog>

      {/* ── Model create/edit dialog ── */}
      <Dialog
        open={modelOpen}
        onClose={closeModelDialog}
        title={modelEdit ? "编辑模型" : "添加模型"}
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <Select
            label="厂商"
            value={modelForm.providerId}
            onChange={(e) => setModelForm((f) => ({ ...f, providerId: e.target.value }))}
            options={providers.map((p) => ({ value: p.id, label: p.name }))}
          />
          <TextField
            label="模型编码"
            value={modelForm.code}
            onChange={(e) => setModelForm((f) => ({ ...f, code: e.target.value }))}
            placeholder="gpt-4o"
          />
          <TextField
            label="模型名称"
            value={modelForm.name}
            onChange={(e) => setModelForm((f) => ({ ...f, name: e.target.value }))}
          />
          <Select
            label="类型"
            value={modelForm.type}
            onChange={(e) => setModelForm((f) => ({ ...f, type: e.target.value }))}
            options={TYPE_TABS.filter((t) => t.value !== "all")}
          />
          <Select
            label="状态"
            value={modelForm.enabled ? "true" : "false"}
            onChange={(e) => setModelForm((f) => ({ ...f, enabled: e.target.value === "true" }))}
            options={[
              { value: "true", label: "启用" },
              { value: "false", label: "停用" },
            ]}
          />
        </Stack>
        <Box sx={{ display: "flex", gap: 1, justifyContent: "flex-end", mt: 2 }}>
          <Button variant="outlined" color="default" onClick={closeModelDialog}>
            取消
          </Button>
          <Button variant="contained" disabled={modelBusy} onClick={() => void saveModel()}>
            保存
          </Button>
        </Box>
      </Dialog>

      <ConfirmDialog
        open={modelDeleting !== null}
        onClose={closeModelDelete}
        onConfirm={() => void runModelDelete()}
        title="删除模型"
        description={`确认删除模型「${modelDeleting?.name ?? ""}」？`}
        confirmColor="danger"
      />
    </PageStack>
  );
}
