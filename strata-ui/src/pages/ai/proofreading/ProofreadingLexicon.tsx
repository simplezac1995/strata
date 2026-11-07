// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { Box, Stack, Typography } from "@mui/material";
import {
  AiPageHeader,
  Button,
  Chip,
  DataTable,
  Dialog,
  Pagination,
  Select,
  Tabs,
  TextArea,
  TextField,
} from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { Add, Edit, Refresh, Search, Science } from "@mui/icons-material";

interface LexiconRow {
  id: string;
  word: string;
  type: string;
  remark: string;
  createdAt: string;
}

const TYPE_OPTIONS = [
  { value: "", label: "请选择类型" },
  { value: "black", label: "黑名单" },
  { value: "white", label: "白名单" },
];

const TABS = [
  { id: "sensitive", label: "敏感词库" },
  { id: "leader", label: "领导人信息" },
];

export function ProofreadingLexicon() {
  const [tab, setTab] = useState("sensitive");
  const [word, setWord] = useState("");
  const [type, setType] = useState("");
  const [rows] = useState<LexiconRow[]>([]);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<LexiconRow | null>(null);
  const [content, setContent] = useState("");
  const [lexiconType, setLexiconType] = useState("black");
  const [remark, setRemark] = useState("");

  const openCreate = () => {
    setEditing(null);
    setContent("");
    setLexiconType("black");
    setRemark("");
    setDialogOpen(true);
  };

  const openEdit = (row: LexiconRow) => {
    setEditing(row);
    setContent(row.word);
    setLexiconType(row.type === "白名单" ? "white" : "black");
    setRemark(row.remark);
    setDialogOpen(true);
  };

  const cols = [
    { key: "word", title: "敏感词", align: "left" as const },
    {
      key: "type",
      title: "类型",
      render: (r: LexiconRow) => (
        <Chip
          label={r.type}
          size="small"
          color={r.type === "白名单" ? "success" : "default"}
          variant="outlined"
        />
      ),
    },
    { key: "remark", title: "备注", align: "left" as const },
    { key: "createdAt", title: "创建时间" },
    {
      key: "actions",
      title: "操作",
      render: (r: LexiconRow) => (
        <Stack direction="row" spacing={0.5}>
          <Button
            variant="text"
            size="sm"
            startIcon={<Edit sx={{ fontSize: 14 }} />}
            onClick={() => openEdit(r)}
          >
            编辑
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
      <AiPageHeader title="审校词库" subtitle="管理自定义敏感词、领导人相关词汇和禁用词" />

      <Tabs tabs={TABS} active={tab} onChange={setTab} />

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
          敏感词
        </Typography>
        <TextField
          size="small"
          placeholder="请输入敏感词关键词"
          value={word}
          onChange={(e) => setWord(e.target.value)}
          sx={{ width: 220 }}
        />
        <Typography variant="body2" sx={{ color: "text.secondary", ml: 1 }}>
          类型
        </Typography>
        <Select
          size="small"
          value={type}
          onChange={(e) => setType(e.target.value)}
          options={TYPE_OPTIONS}
          sx={{ width: 160 }}
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
            setWord("");
            setType("");
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
        <Button
          variant="outlined"
          size="small"
          color="default"
          startIcon={<Science sx={{ fontSize: 14 }} />}
        >
          测试
        </Button>
        <Button
          variant="outlined"
          size="small"
          color="default"
          startIcon={<Refresh sx={{ fontSize: 14 }} />}
        >
          刷新
        </Button>
        <Button variant="outlined" size="small" color="default" disabled>
          删除
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

      {/* Add / Edit dialog */}
      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={editing ? "编辑敏感词" : "新增敏感词"}
        maxWidth="sm"
      >
        <Stack spacing={2} sx={{ py: 1 }}>
          <Box>
            <Typography variant="body2" sx={{ mb: 0.5 }}>
              <Box component="span" sx={{ color: "error.main" }}>
                *{" "}
              </Box>
              敏感词内容
            </Typography>
            <TextArea
              rows={4}
              placeholder="一行一个敏感词，批量新增请换行输入"
              value={content}
              onChange={(e) => setContent(e.target.value)}
            />
            <Typography variant="caption" sx={{ color: "text.disabled" }}>
              支持批量添加，将自动忽略空白行
            </Typography>
          </Box>
          <Box>
            <Typography variant="body2" sx={{ mb: 0.5 }}>
              <Box component="span" sx={{ color: "error.main" }}>
                *{" "}
              </Box>
              词库类型
            </Typography>
            <Select
              size="small"
              value={lexiconType}
              onChange={(e) => setLexiconType(e.target.value)}
              options={[
                { value: "black", label: "黑名单" },
                { value: "white", label: "白名单" },
              ]}
            />
          </Box>
          <TextField
            label="备注"
            placeholder="备注信息（可选）"
            value={remark}
            onChange={(e) => setRemark(e.target.value)}
          />
        </Stack>
        <Box sx={{ display: "flex", gap: 1, justifyContent: "flex-end", pt: 2 }}>
          <Button
            variant="outlined"
            color="default"
            size="small"
            onClick={() => setDialogOpen(false)}
          >
            取消
          </Button>
          <Button variant="contained" size="small" onClick={() => setDialogOpen(false)}>
            {editing ? "保存修改" : "新增词条"}
          </Button>
        </Box>
      </Dialog>
    </PageStack>
  );
}

export default ProofreadingLexicon;
