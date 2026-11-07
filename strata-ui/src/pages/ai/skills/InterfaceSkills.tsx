// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import SearchIcon from "@mui/icons-material/Search";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import DescriptionIcon from "@mui/icons-material/Description";
import AddIcon from "@mui/icons-material/Add";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutlined";
import { Box, Chip, IconButton, Paper, Typography } from "@mui/material";
import { Button, TextField } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { toast } from "@/shared/pageKit/Toast";

interface ApiNode {
  controller: string;
  methods: { method: string; path: string; summary: string }[];
}

const MOCK_TREE: ApiNode[] = [
  {
    controller: "ai-chat-controller",
    methods: [
      { method: "GET", path: "/api/v1/ai/chat/sessions", summary: "会话列表" },
      { method: "GET", path: "/api/v1/ai/chat/sessions/{id}/messages", summary: "消息列表" },
      { method: "POST", path: "/api/v1/ai/chat/sessions", summary: "创建会话" },
      { method: "POST", path: "/api/v1/ai/chat/sessions/{id}/messages", summary: "发送消息" },
      { method: "DELETE", path: "/api/v1/ai/chat/sessions/{id}", summary: "删除会话" },
    ],
  },
  {
    controller: "user-controller",
    methods: [
      { method: "GET", path: "/api/v1/system/user/page", summary: "用户分页" },
      { method: "POST", path: "/api/v1/system/user", summary: "创建用户" },
      { method: "PUT", path: "/api/v1/system/user", summary: "更新用户" },
      { method: "DELETE", path: "/api/v1/system/user/{id}", summary: "删除用户" },
    ],
  },
  {
    controller: "model-controller",
    methods: [
      { method: "GET", path: "/api/v1/ai/models", summary: "模型列表" },
      { method: "POST", path: "/api/v1/ai/models", summary: "创建模型" },
    ],
  },
  {
    controller: "knowledge-controller",
    methods: [
      { method: "GET", path: "/api/v1/ai/knowledge-bases", summary: "知识库列表" },
      { method: "POST", path: "/api/v1/ai/knowledge-bases", summary: "创建知识库" },
      { method: "DELETE", path: "/api/v1/ai/knowledge-bases/{id}", summary: "删除知识库" },
    ],
  },
];

const METHOD_COLORS: Record<string, string> = {
  GET: "#2e7d32",
  POST: "#1976d2",
  PUT: "#ed6c02",
  DELETE: "#d32f2f",
};

export function InterfaceSkills() {
  const [expanded, setExpanded] = useState<Record<string, boolean>>({ "ai-chat-controller": true });
  const [selected, setSelected] = useState<{
    method: string;
    path: string;
    summary: string;
  } | null>(null);
  const [search, setSearch] = useState("");
  const [methodFilter, setMethodFilter] = useState<string | null>(null);
  const [selectedList, setSelectedList] = useState<{ method: string; path: string }[]>([]);
  const [desc, setDesc] = useState("");

  const toggleSelect = (m: { method: string; path: string; summary: string }) => {
    setSelected(m);
    setDesc(m.summary);
    setSelectedList((prev) => {
      const exists = prev.find((p) => p.path === m.path);
      if (exists) return prev.filter((p) => p.path !== m.path);
      if (prev.length >= 10) {
        toast.error("最多选择 10 个接口");
        return prev;
      }
      return [...prev, { method: m.method, path: m.path }];
    });
  };

  return (
    <PageStack>
      {/* Top bar */}
      <Box sx={{ display: "flex", gap: 1.5, alignItems: "center", mb: 2 }}>
        <TextField
          size="small"
          placeholder="搜索接口..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          sx={{ width: 260 }}
          slotProps={{
            input: { startAdornment: <SearchIcon sx={{ color: "text.disabled", mr: 1 }} /> },
          }}
        />
        {["GET", "POST", "PUT", "DELETE"].map((m) => (
          <Chip
            key={m}
            label={m}
            size="small"
            onClick={() => setMethodFilter(methodFilter === m ? null : m)}
            sx={{
              fontWeight: 700,
              cursor: "pointer",
              bgcolor: methodFilter === m ? METHOD_COLORS[m] : "transparent",
              color: methodFilter === m ? "#fff" : METHOD_COLORS[m],
              borderColor: METHOD_COLORS[m],
            }}
            variant="outlined"
          />
        ))}
        <Button
          variant="contained"
          size="small"
          disabled={selectedList.length === 0}
          onClick={() => toast.success(`已生成 ${selectedList.length} 个 Skills`)}
        >
          生成 Skills{selectedList.length > 0 ? ` (${selectedList.length})` : ""}
        </Button>
        {selectedList.length > 0 && (
          <>
            <Typography variant="caption" sx={{ color: "text.secondary" }}>
              已选
            </Typography>
            {selectedList.map((s) => (
              <Chip
                key={s.path}
                label={`${s.method} ${s.path}`}
                size="small"
                onDelete={() => setSelectedList((prev) => prev.filter((p) => p.path !== s.path))}
                sx={{ fontFamily: "monospace", fontSize: 11 }}
              />
            ))}
            <Typography
              variant="caption"
              sx={{ color: "primary.main", cursor: "pointer", ml: 1 }}
              onClick={() => setSelectedList([])}
            >
              清空
            </Typography>
          </>
        )}
        {selectedList.length === 0 && (
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            点击左侧接口勾选，最多选 10 个
          </Typography>
        )}
      </Box>

      <Box sx={{ display: "flex", gap: 2, height: "calc(100vh - 200px)" }}>
        {/* Left: API tree */}
        <Paper variant="outlined" sx={{ width: 340, p: 1, overflowY: "auto" }}>
          {MOCK_TREE.filter(
            (n) =>
              (!search || n.controller.includes(search)) &&
              (!methodFilter || n.methods.some((m) => m.method === methodFilter)),
          ).map((node) => (
            <Box key={node.controller}>
              <Box
                sx={{
                  display: "flex",
                  alignItems: "center",
                  gap: 0.5,
                  px: 1,
                  py: 0.75,
                  cursor: "pointer",
                }}
                onClick={() =>
                  setExpanded((p) => ({ ...p, [node.controller]: !p[node.controller] }))
                }
              >
                {expanded[node.controller] ? (
                  <ExpandMoreIcon fontSize="small" />
                ) : (
                  <ChevronRightIcon fontSize="small" />
                )}
                <Typography variant="body2" sx={{ fontWeight: 600, flex: 1 }}>
                  {node.controller}
                </Typography>
                <Chip label={node.methods.length} size="small" sx={{ height: 18, fontSize: 11 }} />
              </Box>
              {expanded[node.controller] &&
                node.methods
                  .filter((m) => !methodFilter || m.method === methodFilter)
                  .map((m) => (
                    <Box
                      key={m.path}
                      onClick={() => toggleSelect(m)}
                      sx={{
                        ml: 3,
                        px: 1,
                        py: 0.75,
                        cursor: "pointer",
                        borderRadius: 1,
                        bgcolor: selected?.path === m.path ? "action.selected" : "transparent",
                        "&:hover": { bgcolor: "action.hover" },
                      }}
                    >
                      <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                        <Chip
                          label={m.method}
                          size="small"
                          sx={{
                            height: 20,
                            fontSize: 10,
                            fontWeight: 700,
                            color: METHOD_COLORS[m.method],
                            bgcolor: "transparent",
                          }}
                        />
                        <Box sx={{ flex: 1, minWidth: 0 }}>
                          <Typography
                            variant="body2"
                            sx={{ fontSize: 12, fontFamily: "monospace", wordBreak: "break-all" }}
                          >
                            {m.path}
                          </Typography>
                          <Typography variant="caption" sx={{ color: "text.secondary" }}>
                            {m.summary}
                          </Typography>
                        </Box>
                        {selectedList.some((s) => s.path === m.path) && (
                          <Typography variant="caption" sx={{ color: "primary.main" }}>
                            ✓
                          </Typography>
                        )}
                      </Box>
                    </Box>
                  ))}
            </Box>
          ))}
        </Paper>

        {/* Right: detail */}
        <Paper variant="outlined" sx={{ flex: 1, p: 3, overflowY: "auto" }}>
          {selected ? (
            <>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mb: 3 }}>
                <Chip
                  label={selected.method}
                  size="small"
                  sx={{ fontWeight: 700, color: METHOD_COLORS[selected.method] }}
                />
                <Typography variant="h6" sx={{ fontFamily: "monospace" }}>
                  {selected.path}
                </Typography>
              </Box>

              <Typography variant="subtitle2" sx={{ mb: 1 }}>
                接口描述
              </Typography>
              <TextField
                fullWidth
                size="small"
                value={desc}
                onChange={(e) => setDesc(e.target.value)}
                sx={{ mb: 3 }}
                placeholder="留空则使用 Swagger 原始描述"
              />

              <Box
                sx={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  mb: 1,
                }}
              >
                <Typography variant="subtitle2">参数列表</Typography>
                <Button size="small" variant="text" startIcon={<AddIcon />}>
                  新增参数
                </Button>
              </Box>
              <Paper
                variant="outlined"
                sx={{ p: 1.5, mb: 3, display: "flex", alignItems: "center", gap: 1 }}
              >
                <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
                  key
                </Typography>
                <Chip label="integer" size="small" variant="outlined" />
                <Chip
                  label="必填"
                  size="small"
                  sx={{ color: "error.main", borderColor: "error.main" }}
                  variant="outlined"
                />
                <Typography variant="caption" sx={{ color: "text.secondary", flex: 1 }}>
                  无描述
                </Typography>
                <IconButton size="small" sx={{ color: "error.main" }}>
                  <DeleteOutlineIcon fontSize="small" />
                </IconButton>
              </Paper>

              {/* cURL preview */}
              <Typography variant="subtitle2" sx={{ mb: 1 }}>
                $ CURL 预览
              </Typography>
              <Paper sx={{ bgcolor: "#1e1e1e", p: 2.5, borderRadius: 2 }}>
                <Typography variant="caption" sx={{ color: "#888" }}>
                  QUERY 参数
                </Typography>
                <Box sx={{ display: "flex", alignItems: "center", gap: 1, mt: 1, mb: 2 }}>
                  <Typography variant="body2" sx={{ color: "#d4d4d4" }}>
                    key *
                  </Typography>
                  <TextField
                    size="small"
                    defaultValue="1"
                    sx={{ bgcolor: "#2d2d2d", input: { color: "#d4d4d4" } }}
                  />
                </Box>
                <Box
                  sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}
                >
                  <Typography variant="caption" sx={{ color: "#888" }}>
                    生成的 cURL 命令
                  </Typography>
                  <Button
                    size="small"
                    variant="text"
                    sx={{ color: "#aaa" }}
                    startIcon={<ContentCopyIcon fontSize="small" />}
                  >
                    复制
                  </Button>
                </Box>
                <Box
                  component="pre"
                  sx={{
                    color: "#4ec9b0",
                    fontFamily: "monospace",
                    fontSize: 13,
                    mt: 1,
                    whiteSpace: "pre-wrap",
                  }}
                >
                  {`curl -X ${selected.method} \\
  'http://localhost:8080${selected.path}?key=1' \\
  -H 'Authorization: Bearer <TOKEN>' \\
  -H 'TENANT-ID: 1'`}
                </Box>
              </Paper>
            </>
          ) : (
            <Box
              sx={{
                height: "100%",
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                justifyContent: "center",
                gap: 1,
              }}
            >
              <DescriptionIcon sx={{ fontSize: 48, color: "text.disabled" }} />
              <Typography sx={{ color: "text.secondary" }}>从左侧选择一个接口开始编辑</Typography>
            </Box>
          )}
        </Paper>
      </Box>
    </PageStack>
  );
}

export default InterfaceSkills;
