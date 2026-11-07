// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, FormControlLabel, Radio, RadioGroup, Stack, Typography } from "@mui/material";
import {
  AiPageHeader,
  Button,
  DataTable,
  Dialog,
  Pagination,
  Select,
  TextArea,
  TextField,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { Add, Edit, Refresh, Search } from "@mui/icons-material";

interface RuleRow {
  id: string;
  name: string;
  code: string;
  desc: string;
  weight: number;
  enabled: boolean;
}

const STATUS_OPTIONS = [
  { value: "", label: "请选择状态" },
  { value: "enabled", label: "启用" },
  { value: "disabled", label: "禁用" },
];

const PARENT_OPTIONS = [{ value: "", label: "留空为根节点" }];

export function ProofreadingRules() {
  const [name, setName] = useState("");
  const [code, setCode] = useState("");
  const [status, setStatus] = useState("");
  const [rows] = useState<RuleRow[]>([]);

  // Add / edit dimension dialog
  const [dimOpen, setDimOpen] = useState(false);
  const [editing, setEditing] = useState<RuleRow | null>(null);
  const [parent, setParent] = useState("");
  const [dimName, setDimName] = useState("");
  const [dimCode, setDimCode] = useState("");
  const [dimDesc, setDimDesc] = useState("");
  const [dimCategory, setDimCategory] = useState("");
  const [dimSort, setDimSort] = useState(0);
  const [dimEnabled, setDimEnabled] = useState(true);

  // Prompt editor dialog
  const [promptOpen, setPromptOpen] = useState(false);
  const [promptCode, setPromptCode] = useState("");

  const openCreate = () => {
    setEditing(null);
    setParent("");
    setDimName("");
    setDimCode("");
    setDimDesc("");
    setDimCategory("");
    setDimSort(0);
    setDimEnabled(true);
    setDimOpen(true);
  };

  const openEdit = (row: RuleRow) => {
    setEditing(row);
    setParent("");
    setDimName(row.name);
    setDimCode(row.code);
    setDimDesc(row.desc);
    setDimCategory("");
    setDimSort(0);
    setDimEnabled(row.enabled);
    setDimOpen(true);
  };

  const openPrompt = (_row: RuleRow) => {
    setPromptCode("");
    setPromptOpen(true);
  };

  const cols = [
    {
      key: "name",
      title: "维度名称",
      align: "left" as const,
      render: (r: RuleRow) => (
        <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
          <Typography variant="body2" sx={{ fontWeight: 500 }}>
            {r.name}
          </Typography>
        </Box>
      ),
    },
    {
      key: "code",
      title: "维度编码",
      render: (r: RuleRow) => (
        <Box component="code" sx={{ fontSize: 12, fontFamily: "monospace" }}>
          {r.code}
        </Box>
      ),
    },
    { key: "desc", title: "描述", align: "left" as const },
    { key: "weight", title: "权重" },
    {
      key: "enabled",
      title: "默认启用",
      render: (r: RuleRow) => (
        <Box
          component="span"
          sx={{
            px: 1,
            py: 0.25,
            borderRadius: 0.5,
            fontSize: 12,
            color: "success.main",
            bgcolor: "action.hover",
          }}
        >
          {r.enabled ? "是" : "否"}
        </Box>
      ),
    },
    {
      key: "actions",
      title: "操作",
      render: (r: RuleRow) => (
        <Stack direction="row" spacing={0.25}>
          <Button variant="text" size="sm" startIcon={<Add sx={{ fontSize: 14 }} />}>
            新增
          </Button>
          <Button
            variant="text"
            size="sm"
            startIcon={<Edit sx={{ fontSize: 14 }} />}
            onClick={() => openEdit(r)}
          >
            编辑
          </Button>
          <Button variant="text" size="sm" onClick={() => openPrompt(r)}>
            提示词
          </Button>
          <Button variant="text" size="sm" color="danger" disabled>
            删除
          </Button>
        </Stack>
      ),
    },
  ];

  return (
    <PageStack>
      <AiPageHeader title="校对规则" subtitle="配置审校引擎的检查规则，保存后对所有新检查生效" />

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
        <Typography variant="body2" sx={{ color: "text.secondary" }}>
          维度名称
        </Typography>
        <TextField
          size="small"
          placeholder="请输入维度名称"
          value={name}
          onChange={(e) => setName(e.target.value)}
          sx={{ width: 200 }}
        />
        <Typography variant="body2" sx={{ color: "text.secondary", ml: 1 }}>
          维度编码
        </Typography>
        <TextField
          size="small"
          placeholder="请输入维度编码"
          value={code}
          onChange={(e) => setCode(e.target.value)}
          sx={{ width: 200 }}
        />
        <Typography variant="body2" sx={{ color: "text.secondary", ml: 1 }}>
          状态
        </Typography>
        <Select
          size="small"
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          options={STATUS_OPTIONS}
          sx={{ width: 140 }}
        />
        <Button variant="contained" size="small" startIcon={<Search sx={{ fontSize: 14 }} />}>
          查询
        </Button>
        <Button
          variant="outlined"
          size="small"
          color="default"
          startIcon={<Refresh sx={{ fontSize: 14 }} />}
          onClick={() => {
            setName("");
            setCode("");
            setStatus("");
          }}
        >
          重置
        </Button>
      </Box>

      {/* Toolbar */}
      <Box sx={{ display: "flex", gap: 1 }}>
        <Button
          variant="contained"
          size="small"
          startIcon={<Add sx={{ fontSize: 14 }} />}
          onClick={openCreate}
        >
          新增
        </Button>
        <Button variant="outlined" size="small" color="default" disabled>
          删除
        </Button>
        <Button variant="outlined" size="small" color="default">
          全部收起
        </Button>
      </Box>

      <DataTable columns={cols} data={rows} rowKey={(r) => r.id} selectable />
      <Pagination
        page={1}
        pageSize={10}
        total={rows.length}
        onPage={() => {}}
        onPageSize={() => {}}
      />

      {/* Add / edit dimension dialog */}
      <Dialog
        open={dimOpen}
        onClose={() => setDimOpen(false)}
        title={editing ? "编辑维度" : "新增维度"}
        maxWidth="sm"
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <Select
            label="上级维度"
            value={parent}
            onChange={(e) => setParent(e.target.value)}
            options={PARENT_OPTIONS}
          />
          <TextField
            label="维度名称"
            required
            value={dimName}
            onChange={(e) => setDimName(e.target.value)}
          />
          <TextField
            label="维度编码"
            required
            value={dimCode}
            onChange={(e) => setDimCode(e.target.value)}
            placeholder="如 CUSTOM_LEADER"
          />
          <TextArea
            label="描述"
            rows={3}
            value={dimDesc}
            onChange={(e) => setDimDesc(e.target.value)}
          />
          <TextField
            label="分类"
            value={dimCategory}
            onChange={(e) => setDimCategory(e.target.value)}
            placeholder="如 CUSTOM"
          />
          <TextField
            label="排序"
            type="number"
            value={dimSort}
            onChange={(e) => setDimSort(Number(e.target.value))}
          />
          <Box>
            <Typography variant="body2" sx={{ mb: 0.5 }}>
              启用
            </Typography>
            <RadioGroup
              row
              value={dimEnabled ? "true" : "false"}
              onChange={(_, v) => setDimEnabled(v === "true")}
            >
              <FormControlLabel value="true" control={<Radio size="small" />} label="是" />
              <FormControlLabel value="false" control={<Radio size="small" />} label="否" />
            </RadioGroup>
          </Box>
        </Stack>
        <Box sx={{ display: "flex", gap: 1, justifyContent: "flex-end", pt: 2 }}>
          <Button variant="outlined" color="default" size="small" onClick={() => setDimOpen(false)}>
            取消
          </Button>
          <Button variant="contained" size="small" onClick={() => setDimOpen(false)}>
            确认
          </Button>
        </Box>
      </Dialog>

      {/* Prompt editor dialog */}
      <Dialog
        open={promptOpen}
        onClose={() => setPromptOpen(false)}
        title="维护提示词"
        maxWidth="md"
      >
        <TextArea
          rows={12}
          value={promptCode}
          onChange={(e) => setPromptCode(e.target.value)}
          placeholder="在此编写该维度的提示词…"
          sx={{
            "& .MuiInputBase-input": {
              fontFamily: "'JetBrains Mono', Consolas, monospace",
              fontSize: 13,
              lineHeight: 1.6,
            },
          }}
        />
        <Box sx={{ display: "flex", gap: 1, justifyContent: "flex-end", pt: 2 }}>
          <Button
            variant="outlined"
            color="default"
            size="small"
            onClick={() => setPromptOpen(false)}
          >
            取消
          </Button>
          <Button variant="contained" size="small" onClick={() => setPromptOpen(false)}>
            保存
          </Button>
        </Box>
      </Dialog>
    </PageStack>
  );
}

export default ProofreadingRules;
