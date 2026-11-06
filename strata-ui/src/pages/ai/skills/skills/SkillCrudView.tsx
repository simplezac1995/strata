// SPDX-License-Identifier: Apache-2.0
// View of SkillCrud: the JSX, with the hook result passed in as `view`.

import {
  Box,
  Card,
  CardContent,
  Chip,
  Drawer,
  IconButton,
  Paper,
  Radio,
  Stack,
  Typography,
} from "@mui/material";
import { Button, ConfirmDialog, Dialog, Pagination, TextField } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import DescriptionIcon from "@mui/icons-material/Description";
import ChatIcon from "@mui/icons-material/Chat";
import PersonOutlineIcon from "@mui/icons-material/PersonOutlineOutlined";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import CloseIcon from "@mui/icons-material/Close";
import CloudUploadIcon from "@mui/icons-material/CloudUpload";
import AddIcon from "@mui/icons-material/Add";
import SearchIcon from "@mui/icons-material/Search";
import RefreshIcon from "@mui/icons-material/Refresh";
import type { useSkillCrud } from "./part-2/useSkillCrud";
import { SkillCrudViewDialogs } from "./SkillCrudViewDialogs";

export function SkillCrudView({
  view,
  type,
}: { view: ReturnType<typeof useSkillCrud> } & { type: "skill" | "interface" }) {
  const {
    canCreate,
    canUpdate,
    canDelete,
    navigate,
    filterName,
    setFilterName,
    filterEnabled,
    setFilterEnabled,
    uploadOpen,
    setUploadOpen,
    supportOpen,
    setSupportOpen,
    supportSkill,
    setSupportSkill,
    skills,
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
    filtered,
  } = view;

  return (
    <PageStack>
      {/* Filter bar */}
      <Box
        sx={{
          bgcolor: "background.paper",
          border: 1,
          borderColor: "divider",
          borderRadius: 2,
          p: 2,
          mb: 2,
          display: "flex",
          gap: 2,
          alignItems: "center",
          flexWrap: "wrap",
        }}
      >
        <TextField
          size="small"
          placeholder="请输入技能名称"
          value={filterName}
          onChange={(e) => setFilterName(e.target.value)}
          sx={{ width: 200 }}
        />
        <Box sx={{ minWidth: 140 }}>
          <select
            value={filterEnabled}
            onChange={(e) => setFilterEnabled(e.target.value)}
            style={{
              height: 32,
              border: "1px solid rgba(0,0,0,0.23)",
              borderRadius: 4,
              padding: "0 10px",
              fontSize: 14,
              background: "white",
            }}
          >
            <option value="all">全部状态</option>
            <option value="yes">启用</option>
            <option value="no">停用</option>
          </select>
        </Box>
        <Button variant="contained" size="small" onClick={() => reload()}>
          <SearchIcon sx={{ mr: 0.5 }} /> 查询
        </Button>
        <Button
          variant="outlined"
          size="small"
          onClick={() => {
            setFilterName("");
            setFilterEnabled("all");
          }}
        >
          <RefreshIcon sx={{ mr: 0.5 }} /> 重置
        </Button>
      </Box>

      {/* Action bar */}
      <Box sx={{ display: "flex", gap: 1, mb: 2 }}>
        {canCreate && (
          <Button variant="contained" size="small" onClick={openCreate}>
            <AddIcon sx={{ mr: 0.5 }} /> 新增技能
          </Button>
        )}
        <Button variant="contained" size="small" onClick={() => setUploadOpen(true)}>
          <CloudUploadIcon sx={{ mr: 0.5 }} /> 上传技能
        </Button>
        <Button variant="outlined" size="small" color="default">
          删除
        </Button>
      </Box>

      {/* Card grid */}
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(340px, 1fr))",
          gap: 2,
        }}
      >
        {filtered.map((s) => (
          <Card key={s.id} variant="outlined" sx={{ borderRadius: 3 }}>
            <CardContent>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mb: 1.5 }}>
                <Box
                  sx={{
                    width: 44,
                    height: 44,
                    borderRadius: 2,
                    bgcolor: "#9c27b0",
                    color: "#fff",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontSize: 18,
                    fontWeight: 700,
                  }}
                >
                  {s.name[0]?.toUpperCase()}
                </Box>
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Typography variant="subtitle2" sx={{ fontWeight: 600 }} noWrap>
                    {s.name}
                  </Typography>
                  <Typography variant="caption" sx={{ color: "text.secondary" }}>
                    ※ 1.0.0
                  </Typography>
                </Box>
                <Chip
                  label={s.enabled ? "启用中" : "停用"}
                  size="small"
                  color={s.enabled ? "success" : "default"}
                  variant="outlined"
                />
              </Box>
              <Typography
                variant="body2"
                sx={{
                  color: "text.secondary",
                  minHeight: 60,
                  display: "-webkit-box",
                  WebkitLineClamp: 3,
                  WebkitBoxOrient: "vertical",
                  overflow: "hidden",
                }}
              >
                {s.description || "暂无描述"}
              </Typography>
              <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, mt: 1 }}>
                <PersonOutlineIcon sx={{ fontSize: 14, color: "text.disabled" }} />
                <Typography
                  variant="caption"
                  sx={{ color: "text.disabled", fontFamily: "monospace" }}
                >
                  用户可调用: /{s.name}
                </Typography>
              </Box>
              <Box
                sx={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  mt: 1.5,
                  pt: 1.5,
                  borderTop: 1,
                  borderColor: "divider",
                }}
              >
                <Stack direction="row" spacing={0.5}>
                  {canUpdate && (
                    <IconButton size="small" onClick={() => openEdit(s)}>
                      <EditIcon fontSize="small" />
                    </IconButton>
                  )}
                  {canDelete && (
                    <IconButton
                      size="small"
                      sx={{ color: "error.main" }}
                      onClick={() => confirmDelete(s)}
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  )}
                  <IconButton
                    size="small"
                    onClick={() => {
                      setSupportSkill(s);
                      setSupportOpen(true);
                    }}
                  >
                    <DescriptionIcon fontSize="small" />
                  </IconButton>
                  <IconButton size="small" onClick={() => navigate("/ai/workspace")}>
                    <ChatIcon fontSize="small" />
                  </IconButton>
                </Stack>
                <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
                  <AccessTimeIcon sx={{ fontSize: 14, color: "text.disabled" }} />
                  <Typography variant="caption" sx={{ color: "text.disabled" }}>
                    2026-09-12
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        ))}
      </Box>

      <Pagination
        total={total}
        page={page}
        pageSize={pageSize}
        onPage={setPage}
        onPageSize={setPageSize}
      />

      {/* Edit/Create Drawer - two columns */}
      <Drawer
        anchor="right"
        open={open}
        onClose={closeDialog}
        sx={{ "& .MuiDrawer-paper": { width: "80%", maxWidth: 1100, p: 3 } }}
      >
        <Typography variant="h6" sx={{ mb: 2 }}>
          {edit ? "编辑技能" : "新增技能"}
        </Typography>
        <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 3, flex: 1 }}>
          {/* Left: basic info */}
          <Box>
            <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 2 }}>
              基本信息{" "}
              <Typography
                component="span"
                variant="body2"
                sx={{ color: "text.secondary", fontWeight: 400 }}
              >
                维护 Skill 的基础配置字段。
              </Typography>
            </Typography>
            <Stack spacing={2}>
              <TextField
                label="技能名称 *"
                placeholder="如 humanizer-zh"
                value={form.name}
                onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                fullWidth
              />
              <TextField
                label="版本号 *"
                value={form.version}
                onChange={(e) => setForm((f) => ({ ...f, version: e.target.value }))}
                fullWidth
              />
              <TextField
                label="描述 *"
                placeholder="请输入技能描述"
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                multiline
                rows={6}
                fullWidth
              />
              <Box>
                <Typography variant="body2" sx={{ mb: 1 }}>
                  是否启用 *
                </Typography>
                <Stack direction="row" sx={{ alignItems: "center", gap: 0.5 }}>
                  <Radio
                    size="small"
                    checked={!form.enabled}
                    onChange={() => setForm((f) => ({ ...f, enabled: false }))}
                  />
                  <Typography variant="body2">否</Typography>
                  <Radio
                    size="small"
                    checked={form.enabled}
                    onChange={() => setForm((f) => ({ ...f, enabled: true }))}
                  />
                  <Typography variant="body2">是</Typography>
                </Stack>
              </Box>
            </Stack>
          </Box>
          {/* Right: skill content */}
          <Box>
            <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 2 }}>
              Skill 内容{" "}
              <Typography
                component="span"
                variant="body2"
                sx={{ color: "text.secondary", fontWeight: 400 }}
              >
                请输入 Skill 正文内容，保存时按正文原样提交。
              </Typography>
            </Typography>
            <TextField
              placeholder="请输入 Skill 正文内容"
              value={form.content}
              onChange={(e) => setForm((f) => ({ ...f, content: e.target.value }))}
              multiline
              rows={18}
              fullWidth
              sx={{ fontFamily: "monospace", fontSize: 13 }}
            />
          </Box>
        </Box>
        <Box sx={{ display: "flex", gap: 1, justifyContent: "flex-end", mt: 2 }}>
          <Button variant="outlined" color="default" onClick={closeDialog}>
            取消
          </Button>
          <Button variant="contained" disabled={busy} onClick={() => void handleSave()}>
            保存
          </Button>
        </Box>
      </Drawer>

      {/* Upload dialog */}

      {/* Support files drawer */}
      <Drawer
        anchor="right"
        open={supportOpen}
        onClose={() => setSupportOpen(false)}
        sx={{ "& .MuiDrawer-paper": { width: 480, p: 3 } }}
      >
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 2 }}>
          <Typography variant="h6">技能支持文件</Typography>
          <IconButton size="small" onClick={() => setSupportOpen(false)}>
            <CloseIcon fontSize="small" />
          </IconButton>
        </Box>
        <Box sx={{ flex: 1, overflowY: "auto" }}>
          <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
            <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                SKILL.md
              </Typography>
              <Chip label="md" size="small" variant="outlined" />
            </Box>
            <Typography variant="caption" sx={{ color: "text.secondary" }}>
              skills/{supportSkill?.name ?? ""}/SKILL.md
            </Typography>
            <Typography
              variant="caption"
              sx={{ color: "text.secondary", display: "block", mt: 0.5 }}
            >
              大小:{" "}
              {supportSkill?.description
                ? Math.round(supportSkill.description.length / 102.4) / 10
                : 0}{" "}
              KB
            </Typography>
            <Typography variant="caption" sx={{ display: "block", mt: 1 }}>
              内容预览:
            </Typography>
            <Box
              component="pre"
              sx={{
                bgcolor: "action.hover",
                p: 1.5,
                borderRadius: 1,
                fontSize: 12,
                fontFamily: "monospace",
                mt: 0.5,
                color: "text.secondary",
                whiteSpace: "pre-wrap",
                wordBreak: "break-word",
                maxHeight: 400,
                overflow: "auto",
              }}
            >
              {supportSkill?.description || "暂无内容"}
            </Box>
          </Paper>
        </Box>
        <Box sx={{ display: "flex", justifyContent: "flex-end" }}>
          <Button variant="outlined" color="default" onClick={() => setSupportOpen(false)}>
            关闭
          </Button>
        </Box>
      </Drawer>

      <SkillCrudViewDialogs view={view} {...{ type }} />
    </PageStack>
  );
}
