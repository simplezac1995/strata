// SPDX-License-Identifier: Apache-2.0
// Dialogs of SkillCrud, extracted to keep the view under the 500-line cap.

import { Box, Typography } from "@mui/material";
import { Button, ConfirmDialog, Dialog } from "@/components/ui/index";
import CloudUploadIcon from "@mui/icons-material/CloudUpload";
import type { useSkillCrud } from "./part-2/useSkillCrud";

export function SkillCrudViewDialogs({
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
    <>
      <Dialog open={uploadOpen} onClose={() => setUploadOpen(false)} title="上传 Skill ZIP 包">
        <Box sx={{ mb: 2, p: 1.5, bgcolor: "action.hover", borderRadius: 1.5 }}>
          <Typography variant="body2" sx={{ color: "text.secondary" }}>
            上传前会自动检测越狱和挖矿风险，命中后直接阻断。
          </Typography>
        </Box>
        <Box
          sx={{
            border: 2,
            borderStyle: "dashed",
            borderColor: "primary.main",
            borderRadius: 2,
            p: 5,
            textAlign: "center",
            mb: 2,
            bgcolor: "action.hover",
            cursor: "pointer",
            "&:hover": { bgcolor: "action.selected" },
          }}
        >
          <CloudUploadIcon sx={{ fontSize: 48, color: "primary.main", mb: 1.5 }} />
          <Typography variant="body1" sx={{ fontWeight: 500, mb: 0.5 }}>
            拖拽 ZIP 到这里，或点击选择文件
          </Typography>
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            仅支持包含 SKILL.md 的 .zip 技能包
          </Typography>
        </Box>
        <Box sx={{ display: "flex", gap: 1, justifyContent: "flex-end", mt: 2, pb: 1 }}>
          <Button variant="outlined" color="default" onClick={() => setUploadOpen(false)}>
            取消
          </Button>
          <Button variant="contained" onClick={() => setUploadOpen(false)}>
            上传并检测
          </Button>
        </Box>
      </Dialog>

      <ConfirmDialog
        open={deleting !== null}
        onClose={closeDelete}
        onConfirm={() => void runDelete()}
        title="删除"
        description={`确定删除技能 "${deleting?.name}" 吗？`}
        confirmColor="danger"
      />
    </>
  );
}
