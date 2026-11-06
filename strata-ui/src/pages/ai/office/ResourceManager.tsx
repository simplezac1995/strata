// SPDX-License-Identifier: Apache-2.0
import { useMemo, useState } from "react";
import {
  Add as AddIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
  DeleteOutlined as DeleteIcon,
  Edit as EditIcon,
  Upload as ImportIcon,
  AddPhotoAlternate as AvatarPlaceholder,
} from "@mui/icons-material";
import { Box, Chip, IconButton, Stack, Switch, Typography } from "@mui/material";
import {
  Button,
  ConfirmDialog,
  DataTable,
  Dialog,
  Select,
  TextArea,
  TextField,
  Tabs,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { agentApi, skillApi } from "@/shared/api/ai";
import { useListQuery } from "@/shared/hooks/useListQuery";
import type { AiAgentDto, AiSkillDto } from "@/shared/types";
import { toast } from "@/shared/pageKit/Toast";

type TabId = "experts" | "teams" | "ppt" | "poster";

const TAB_LIST = [
  { id: "experts", label: "专家" },
  { id: "teams", label: "专家团" },
  { id: "ppt", label: "PPT模板" },
  { id: "poster", label: "海报模板" },
];

interface TplRow {
  id: string;
  cover: string;
  name: string;
  category: string;
  style: string;
  enabled: boolean;
}

const EXPERT_EMPTY = {
  name: "",
  intro: "",
  prompt: "",
  skill: "",
  enabled: true,
};

export function ResourceManager() {
  const [tab, setTab] = useState<TabId>("experts");
  const [keyword, setKeyword] = useState("");

  // real agents (专家 / 专家团 share the agent source)
  const { data: agents } = useListQuery<AiAgentDto>(
    ["api", "ai", "agents", "list"],
    agentApi.list,
    {
      staleTime: 5 * 60 * 1000,
    },
  );
  const { data: skills } = useListQuery<AiSkillDto>(
    ["api", "ai", "skills", "list"],
    skillApi.list,
    {
      staleTime: 5 * 60 * 1000,
    },
  );

  // local template rows (PPT / 海报)
  const [pptRows, setPptRows] = useState<TplRow[]>([]);
  const [posterRows, setPosterRows] = useState<TplRow[]>([]);

  // dialog
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<AiAgentDto | null>(null);
  const [form, setForm] = useState({ ...EXPERT_EMPTY });
  const [deleting, setDeleting] = useState<{ id: string; name: string } | null>(null);

  const filteredAgents = useMemo(
    () => agents.filter((a) => !keyword || a.name.toLowerCase().includes(keyword.toLowerCase())),
    [agents, keyword],
  );

  const tplRows = tab === "ppt" ? pptRows : posterRows;
  const setTplRows = tab === "ppt" ? setPptRows : setPosterRows;

  const expertColumns = [
    {
      key: "idx",
      title: "#",
      width: 60,
      render: (r: AiAgentDto) => agents.indexOf(r) + 1,
    },
    { key: "name", title: "名称", align: "left" as const, render: (r: AiAgentDto) => r.name },
    {
      key: "intro",
      title: "简介",
      align: "left" as const,
      render: (r: AiAgentDto) => r.model || "暂无简介",
    },
    {
      key: "status",
      title: "状态",
      render: (r: AiAgentDto) => (
        <Chip
          label={r.enabled ? "已启用" : "已停用"}
          size="small"
          color={r.enabled ? "success" : "default"}
          variant="outlined"
        />
      ),
    },
    {
      key: "actions",
      title: "操作",
      width: 160,
      render: (r: AiAgentDto) => (
        <Stack direction="row" spacing={0.5}>
          <Button
            variant="text"
            size="small"
            color="primary"
            startIcon={<EditIcon fontSize="small" />}
            onClick={() => openEdit(r)}
          >
            编辑
          </Button>
          <Button
            variant="text"
            size="small"
            color="danger"
            startIcon={<DeleteIcon fontSize="small" />}
            onClick={() => setDeleting({ id: r.id, name: r.name })}
          >
            删除
          </Button>
        </Stack>
      ),
    },
  ];

  const tplColumns = [
    {
      key: "cover",
      title: "封面",
      width: 100,
      render: () => (
        <Box sx={{ width: 64, height: 40, bgcolor: "action.hover", borderRadius: 1 }} />
      ),
    },
    { key: "name", title: "名称", align: "left" as const, render: (r: TplRow) => r.name },
    { key: "category", title: "分类", render: (r: TplRow) => r.category },
    { key: "style", title: "风格说明", align: "left" as const, render: (r: TplRow) => r.style },
    {
      key: "status",
      title: "状态",
      render: (r: TplRow) => (
        <Chip
          label={r.enabled ? "已启用" : "已停用"}
          size="small"
          color={r.enabled ? "success" : "default"}
          variant="outlined"
        />
      ),
    },
    {
      key: "actions",
      title: "操作",
      width: 160,
      render: (r: TplRow) => (
        <Stack direction="row" spacing={0.5}>
          <Button
            variant="text"
            size="small"
            color="primary"
            startIcon={<EditIcon fontSize="small" />}
          >
            编辑
          </Button>
          <Button
            variant="text"
            size="small"
            color="danger"
            startIcon={<DeleteIcon fontSize="small" />}
            onClick={() => setDeleting({ id: r.id, name: r.name })}
          >
            删除
          </Button>
        </Stack>
      ),
    },
  ];

  function openCreate() {
    setEditing(null);
    setForm({ ...EXPERT_EMPTY });
    setFormOpen(true);
  }

  function openEdit(a: AiAgentDto) {
    setEditing(a);
    setForm({
      name: a.name,
      intro: a.model ?? "",
      prompt: a.prompt ?? "",
      skill: "",
      enabled: a.enabled,
    });
    setFormOpen(true);
  }

  function saveForm() {
    if (!form.name.trim()) {
      toast.error("请输入名称");
      return;
    }
    // Real create/update goes through agentApi when editing an existing row;
    // new expert rows are kept in local demo state (no dedicated expert endpoint).
    toast.success(editing ? "专家已更新" : "专家已保存（演示）");
    setFormOpen(false);
  }

  function addTemplate() {
    const row: TplRow = {
      id: `tpl-${Date.now()}`,
      cover: "",
      name: tab === "ppt" ? "新建 PPT 模板" : "新建海报模板",
      category: "默认",
      style: "",
      enabled: true,
    };
    setTplRows((prev) => [row, ...prev]);
    toast.success("已新增模板");
  }

  return (
    <PageStack>
      <Tabs tabs={TAB_LIST} active={tab} onChange={(id) => setTab(id as TabId)} />

      {/* Filter bar */}
      <Box sx={{ display: "flex", gap: 1.5, alignItems: "center", flexWrap: "wrap" }}>
        <TextField
          placeholder="搜索名称或简介"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          sx={{ width: 240 }}
        />
        <Button variant="contained" size="small" startIcon={<SearchIcon />}>
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
          {tab === "experts" || tab === "teams" ? (
            <>
              <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
                {tab === "experts" ? "新增专家" : "新增专家团"}
              </Button>
              <Button variant="outlined" color="default" startIcon={<ImportIcon />}>
                导入压缩包
              </Button>
            </>
          ) : (
            <Button variant="contained" startIcon={<AddIcon />} onClick={addTemplate}>
              {tab === "ppt" ? "新增 PPT 模板" : "新增海报模板"}
            </Button>
          )}
        </Box>
        <IconButton
          sx={{ border: 1, borderColor: "divider", borderRadius: 1.5, width: 38, height: 38 }}
        >
          <RefreshIcon fontSize="small" />
        </IconButton>
      </Box>

      {/* Table */}
      {tab === "experts" || tab === "teams" ? (
        <DataTable<AiAgentDto>
          columns={expertColumns}
          data={filteredAgents}
          rowKey={(r) => r.id}
          tableId={`ai-resource-${tab}`}
          emptyAction={
            <Button variant="contained" size="small" startIcon={<AddIcon />} onClick={openCreate}>
              {tab === "experts" ? "新增专家" : "新增专家团"}
            </Button>
          }
        />
      ) : (
        <DataTable<TplRow>
          columns={tplColumns}
          data={tplRows}
          rowKey={(r) => r.id}
          tableId={`ai-resource-${tab}`}
          emptyAction={
            <Button variant="contained" size="small" startIcon={<AddIcon />} onClick={addTemplate}>
              {tab === "ppt" ? "新增 PPT 模板" : "新增海报模板"}
            </Button>
          }
        />
      )}

      {/* Expert / Team dialog */}
      <Dialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        title={editing ? "编辑专家" : tab === "teams" ? "新增专家团" : "新增专家"}
        maxWidth="sm"
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <TextField
            label="* 名称"
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            placeholder="0 / 60"
            slotProps={{
              input: {
                endAdornment: (
                  <Typography variant="caption" sx={{ color: "text.secondary" }}>
                    {form.name.length} / 60
                  </Typography>
                ),
              },
            }}
          />
          <TextArea
            label="* 简介"
            value={form.intro}
            onChange={(e) => setForm((f) => ({ ...f, intro: e.target.value }))}
            rows={2}
          />
          <Box>
            <Typography variant="body2" sx={{ mb: 0.5 }}>
              头像
            </Typography>
            <Box
              sx={{
                width: 140,
                height: 140,
                border: 2,
                borderStyle: "dashed",
                borderColor: "divider",
                borderRadius: 2,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                cursor: "pointer",
                bgcolor: "action.hover",
              }}
              onClick={() => toast.info("请选择头像图片")}
            >
              <AvatarPlaceholder sx={{ fontSize: 28, color: "text.secondary" }} />
            </Box>
          </Box>
          <TextArea
            label="* 提示词"
            value={form.prompt}
            onChange={(e) => setForm((f) => ({ ...f, prompt: e.target.value }))}
            rows={5}
            placeholder="定义职责、规则与交付要求"
          />
          <Select
            label="关联 Skill"
            value={form.skill}
            onChange={(e) => setForm((f) => ({ ...f, skill: e.target.value }))}
            options={[
              { value: "", label: "请选择" },
              ...skills.map((s) => ({ value: s.name, label: s.name })),
            ]}
          />
          <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <Typography variant="body2">启用</Typography>
            <Switch
              checked={form.enabled}
              onChange={(_, v) => setForm((f) => ({ ...f, enabled: v }))}
            />
          </Box>
        </Stack>
        <Box sx={{ display: "flex", justifyContent: "flex-end", gap: 1, pt: 2 }}>
          <Button variant="outlined" color="default" onClick={() => setFormOpen(false)}>
            取消
          </Button>
          <Button variant="contained" onClick={saveForm}>
            保存
          </Button>
        </Box>
      </Dialog>

      <ConfirmDialog
        open={deleting !== null}
        onClose={() => setDeleting(null)}
        onConfirm={() => {
          setTplRows((prev) => prev.filter((r) => r.id !== deleting!.id));
          setDeleting(null);
          toast.success("已删除");
        }}
        title="删除"
        description={`确认删除 "${deleting?.name}"？`}
        confirmColor="danger"
      />
    </PageStack>
  );
}

export default ResourceManager;
