// SPDX-License-Identifier: Apache-2.0
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutlined";
import EditIcon from "@mui/icons-material/Edit";
import SearchIcon from "@mui/icons-material/Search";
import DownloadIcon from "@mui/icons-material/Download";
import RefreshIcon from "@mui/icons-material/Refresh";
import CloseIcon from "@mui/icons-material/Close";
import SettingsIcon from "@mui/icons-material/Settings";
import ChatIcon from "@mui/icons-material/Chat";
import HubIcon from "@mui/icons-material/Hub";
import LanguageIcon from "@mui/icons-material/Language";
import CodeIcon from "@mui/icons-material/Code";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import {
  Avatar,
  Box,
  Card as MuiCard,
  CardContent,
  Checkbox,
  Chip,
  Drawer,
  IconButton,
  Paper,
  Radio,
  Stack,
  Typography,
} from "@mui/material";
import { Button, TextField, Dialog, ConfirmDialog, Pagination } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { useMcpServers } from "./mcpservers/useMcpServers";

export function McpServers() {
  const {
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
  } = useMcpServers();

  return (
    <PageStack>
      {/* Filter bar */}
      <Box
        sx={{
          display: "flex",
          gap: 1.5,
          flexWrap: "wrap",
          alignItems: "center",
          "& .MuiTextField-root, & .MuiFormControl-root": { width: "auto", minWidth: 160 },
        }}
      >
        <Typography variant="body2">服务器名称</Typography>
        <TextField
          size="small"
          placeholder="请输入服务器名称"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          sx={{ width: 200 }}
        />
        <Typography variant="body2">类型</Typography>
        {["streamable-http", "stdio"].map((t) => (
          <Box key={t} sx={{ display: "flex", alignItems: "center" }}>
            <Radio size="small" />
            {t === "streamable-http" ? "Streamable HTTP" : "STDIO"}
          </Box>
        ))}
        <Typography variant="body2">是否启用</Typography>
        <Box sx={{ display: "flex", alignItems: "center" }}>
          <Radio size="small" /> 否
        </Box>
        <Box sx={{ display: "flex", alignItems: "center" }}>
          <Radio size="small" checked /> 是
        </Box>
        <Button
          variant="contained"
          size="small"
          startIcon={<SearchIcon />}
          onClick={() => reload()}
        >
          查询
        </Button>
        <Button
          variant="outlined"
          size="small"
          color="default"
          startIcon={<RefreshIcon />}
          onClick={() => setKeyword("")}
        >
          重置
        </Button>
      </Box>

      {/* Toolbar */}
      <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <Box sx={{ display: "flex", gap: 1 }}>
          {canCreate && (
            <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
              新增
            </Button>
          )}
          <Button variant="outlined" color="default" startIcon={<DeleteIcon />}>
            删除
          </Button>
        </Box>
        <Box sx={{ display: "flex", gap: 1 }}>
          {[SearchIcon, DownloadIcon, RefreshIcon].map((Icon, i) => (
            <IconButton
              key={i}
              sx={{ border: 1, borderColor: "divider", borderRadius: 1.5, width: 38, height: 38 }}
            >
              <Icon fontSize="small" />
            </IconButton>
          ))}
        </Box>
      </Box>

      {/* Card grid */}
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: { xs: "1fr", sm: "repeat(2, 1fr)", xl: "repeat(3, 1fr)" },
          gap: 2,
        }}
      >
        {filtered.map((s) => {
          const type = (s as Record<string, unknown>).type as string;
          const desc = (s as Record<string, unknown>).description as string;
          return (
            <MuiCard key={s.id} variant="outlined" sx={{ borderRadius: 2 }}>
              <CardContent>
                <Box sx={{ display: "flex", alignItems: "flex-start", gap: 1.5 }}>
                  <Avatar
                    sx={{ width: 44, height: 44, bgcolor: "secondary.main", borderRadius: 2 }}
                  >
                    <HubIcon />
                  </Avatar>
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Box
                      sx={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                      }}
                    >
                      <Typography variant="subtitle1" sx={{ fontWeight: 600 }} noWrap>
                        {s.name}
                      </Typography>
                      <Chip
                        label={s.enabled ? "启用中" : "已停用"}
                        size="small"
                        color={s.enabled ? "success" : "default"}
                        variant="outlined"
                      />
                    </Box>
                    <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
                      <LanguageIcon sx={{ fontSize: 14, color: "text.secondary" }} />
                      <Typography variant="caption" sx={{ color: "text.secondary" }}>
                        {type === "stdio" ? "STDIO" : "Streamable HTTP"}
                      </Typography>
                    </Box>
                  </Box>
                </Box>
                <Typography
                  variant="body2"
                  sx={{
                    color: "text.secondary",
                    mt: 1.5,
                    display: "-webkit-box",
                    WebkitLineClamp: 2,
                    WebkitBoxOrient: "vertical",
                    overflow: "hidden",
                    minHeight: 40,
                  }}
                >
                  {desc || "暂无描述"}
                </Typography>
                <Box sx={{ mt: 1 }}>
                  <Typography variant="caption" sx={{ color: "text.secondary" }}>
                    类型: {type === "stdio" ? "STDIO" : "Streamable HTTP"}
                  </Typography>
                  <br />
                  <Typography variant="caption" sx={{ color: "text.secondary" }}>
                    参数: 暂无
                  </Typography>
                </Box>
                <Stack
                  direction="row"
                  sx={{
                    mt: 1.5,
                    pt: 1.5,
                    borderTop: 1,
                    borderColor: "divider",
                    alignItems: "center",
                    justifyContent: "space-between",
                  }}
                >
                  <Stack direction="row" spacing={0.5}>
                    {canUpdate && (
                      <IconButton size="small" onClick={() => openEdit(s)} title="编辑">
                        <EditIcon fontSize="small" sx={{ color: "primary.main" }} />
                      </IconButton>
                    )}
                    {canDelete && (
                      <IconButton size="small" onClick={() => confirmDelete(s)} title="删除">
                        <DeleteIcon fontSize="small" sx={{ color: "error.main" }} />
                      </IconButton>
                    )}
                    <IconButton size="small" onClick={() => openTools(s)} title="工具能力">
                      <SettingsIcon fontSize="small" sx={{ color: "primary.main" }} />
                    </IconButton>
                    <IconButton size="small" title="对话">
                      <ChatIcon fontSize="small" sx={{ color: "primary.main" }} />
                    </IconButton>
                  </Stack>
                  <Checkbox size="small" />
                </Stack>
              </CardContent>
            </MuiCard>
          );
        })}
        {filtered.length === 0 && (
          <Box sx={{ gridColumn: "1 / -1" }}>
            <Paper variant="outlined" sx={{ p: 6, textAlign: "center", color: "text.secondary" }}>
              暂无 MCP 服务
            </Paper>
          </Box>
        )}
      </Box>

      <Pagination
        page={page}
        pageSize={pageSize}
        total={total}
        onPage={setPage}
        onPageSize={(size: number) => {
          setPageSize(size);
          setPage(1);
        }}
      />

      {/* Add / Edit dialog */}
      <Dialog open={open} onClose={closeDialog} title={edit ? "编辑" : "新增"} maxWidth="sm">
        <Stack spacing={2} sx={{ py: 1 }}>
          <Box sx={{ display: "flex", gap: 2, alignItems: "center" }}>
            <Typography sx={{ width: 80 }}>* 服务名称</Typography>
            <TextField
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              placeholder="请输入服务器名称"
            />
          </Box>
          <Box sx={{ display: "flex", gap: 2 }}>
            <Typography sx={{ width: 80, pt: 1 }}>描述</Typography>
            <TextField
              value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              placeholder="请输入描述"
              multiline
              minRows={2}
              sx={{ flex: 1 }}
            />
          </Box>
          <Box sx={{ display: "flex", gap: 2, alignItems: "center" }}>
            <Typography sx={{ width: 80 }}>* 类型</Typography>
            <Stack direction="row" spacing={1.5}>
              {[
                { value: "streamable-http", label: "Streamable HTTP" },
                { value: "stdio", label: "STDIO" },
              ].map((opt) => (
                <Box
                  key={opt.value}
                  onClick={() => setForm((f) => ({ ...f, type: opt.value }))}
                  sx={{
                    display: "flex",
                    alignItems: "center",
                    gap: 0.5,
                    px: 1.5,
                    py: 0.75,
                    border: 1,
                    borderRadius: 1,
                    cursor: "pointer",
                    borderColor: form.type === opt.value ? "primary.main" : "divider",
                  }}
                >
                  <Radio size="small" checked={form.type === opt.value} />
                  {opt.label}
                </Box>
              ))}
            </Stack>
          </Box>
          <Box sx={{ display: "flex", gap: 2 }}>
            <Typography sx={{ width: 80, pt: 1 }}>* 服务地址</Typography>
            <TextField
              value={form.url}
              onChange={(e) => setForm((f) => ({ ...f, url: e.target.value }))}
              placeholder="请输入 MCP 服务地址，Streamable HTTP 推荐使用 /mcp"
              multiline
              minRows={2}
              sx={{ flex: 1 }}
            />
          </Box>
          <Box sx={{ display: "flex", gap: 2, alignItems: "center" }}>
            <Typography sx={{ width: 80 }}>请求头配置</Typography>
            <TextField
              value={form.headers}
              onChange={(e) => setForm((f) => ({ ...f, headers: e.target.value }))}
              sx={{ flex: 1, fontFamily: "monospace" }}
            />
          </Box>
          <Box sx={{ display: "flex", gap: 2, alignItems: "center" }}>
            <Typography sx={{ width: 80 }}>* 启用</Typography>
            <Stack direction="row" spacing={2}>
              {[
                { value: false, label: "否" },
                { value: true, label: "是" },
              ].map((opt) => (
                <Box
                  key={opt.label}
                  onClick={() => setForm((f) => ({ ...f, enabled: opt.value }))}
                  sx={{
                    display: "flex",
                    alignItems: "center",
                    gap: 0.5,
                    px: 1.5,
                    py: 0.5,
                    border: 1,
                    borderRadius: 1,
                    cursor: "pointer",
                    borderColor: form.enabled === opt.value ? "primary.main" : "divider",
                  }}
                >
                  <Radio size="small" checked={form.enabled === opt.value} />
                  {opt.label}
                </Box>
              ))}
            </Stack>
          </Box>
        </Stack>
        <Box sx={{ display: "flex", gap: 1, justifyContent: "flex-end", mt: 2 }}>
          <Button variant="outlined" color="default" onClick={closeDialog}>
            取消
          </Button>
          <Button variant="contained" disabled={busy} onClick={() => void handleSave()}>
            确认
          </Button>
        </Box>
      </Dialog>

      <ConfirmDialog
        open={deleting !== null}
        onClose={closeDelete}
        onConfirm={() => void runDelete()}
        title="删除 MCP 服务"
        description={`确认删除 "${deleting?.name}"？`}
        confirmColor="danger"
      />

      {/* Right drawer — tool capabilities */}
      <Drawer
        anchor="right"
        open={toolsServer !== null}
        onClose={() => setToolsServer(null)}
        sx={{ "& .MuiDrawer-paper": { width: { xs: "100%", sm: 520 }, p: 0 } }}
      >
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            px: 3,
            pt: 2.5,
          }}
        >
          <Typography variant="h6">MCP 工具能力</Typography>
          <IconButton onClick={() => setToolsServer(null)}>
            <CloseIcon />
          </IconButton>
        </Box>

        <Box sx={{ px: 3, py: 1 }}>
          <Button
            size="small"
            variant="outlined"
            startIcon={testResult === "ok" ? <CheckCircleIcon /> : <LanguageIcon />}
            onClick={() => toolsServer && runTest(toolsServer.id)}
            disabled={testResult === "testing"}
          >
            {testResult === "ok" ? "连接成功" : testResult === "testing" ? "测试中…" : "测试连接"}
          </Button>
        </Box>

        <Box
          sx={{
            px: 3,
            py: 2,
            flex: 1,
            overflowY: "auto",
            display: "flex",
            flexDirection: "column",
            gap: 1.5,
          }}
        >
          {toolsLoading ? (
            <Typography
              variant="body2"
              sx={{ color: "text.secondary", textAlign: "center", py: 6 }}
            >
              加载中…
            </Typography>
          ) : tools.length === 0 ? (
            <Box
              sx={{
                flex: 1,
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                justifyContent: "center",
              }}
            >
              <CodeIcon sx={{ fontSize: 48, color: "text.disabled", mb: 2 }} />
              <Typography variant="body2" sx={{ color: "text.secondary" }}>
                暂无工具数据
              </Typography>
            </Box>
          ) : (
            tools.map((tool) => (
              <Paper key={tool.name} variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
                <Box
                  sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}
                >
                  <Typography variant="subtitle2" sx={{ fontWeight: 600, fontFamily: "monospace" }}>
                    {tool.name}
                  </Typography>
                  <Chip label="工具" size="small" color="primary" variant="outlined" />
                </Box>
                <Typography variant="body2" sx={{ color: "text.secondary", mt: 0.5 }}>
                  {tool.description || "暂无描述"}
                </Typography>
              </Paper>
            ))
          )}
        </Box>

        <Box
          sx={{
            p: 2.5,
            borderTop: 1,
            borderColor: "divider",
            display: "flex",
            justifyContent: "flex-end",
          }}
        >
          <Button variant="outlined" color="default" onClick={() => setToolsServer(null)}>
            关闭
          </Button>
        </Box>
      </Drawer>
    </PageStack>
  );
}

export default McpServers;
