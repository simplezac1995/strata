// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import AddIcon from "@mui/icons-material/Add";
import SendIcon from "@mui/icons-material/Send";
import MicIcon from "@mui/icons-material/Mic";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import DeleteOutlinedIcon from "@mui/icons-material/DeleteOutlined";
import ArticleOutlinedIcon from "@mui/icons-material/ArticleOutlined";
import InfoOutlinedIcon from "@mui/icons-material/InfoOutlined";
import { Avatar, Box, IconButton, MenuItem, Paper, Typography } from "@mui/material";
import { Button, TextField, Select } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { aiLinkageApi } from "@/shared/api/aiLinkage";
import { modelApi } from "@/shared/api/ai";
import { useListQuery } from "@/shared/hooks/useListQuery";

const PRESET = "(创建用户) 帮我创建一个系统用户，用户名 tom 密码123456 手机号 13312341235";

export function LinkageRules() {
  const navigate = useNavigate();
  const [input, setInput] = useState("");
  const [model, setModel] = useState("");

  // Real linkage rules as the session history list; real models for the selector.
  const { data: rules } = useListQuery(["api", "ai", "linkage", "rules"], aiLinkageApi.list);
  const { data: models } = useListQuery(["api", "ai", "models"], modelApi.list);

  const send = (text: string) => {
    if (!text.trim()) return;
    navigate("/ai/workspace", { state: { preset: text } });
  };

  return (
    <PageStack>
      <Box sx={{ display: "flex", gap: 2, height: "calc(100vh - 180px)", minHeight: 460 }}>
        {/* Left: session / rule history */}
        <Paper variant="outlined" sx={{ width: 300, p: 2, overflowY: "auto", flexShrink: 0 }}>
          <Box
            sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mb: 1.5 }}
          >
            <Button
              size="small"
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => send("")}
            >
              新建聊天
            </Button>
            <IconButton size="small">
              <MoreVertIcon fontSize="small" />
            </IconButton>
          </Box>
          <Box sx={{ display: "flex", flexDirection: "column", gap: 1 }}>
            <Paper
              variant="outlined"
              sx={{ p: 1.5, cursor: "pointer" }}
              onClick={() => send(PRESET)}
            >
              <Typography variant="body2" noWrap>
                {PRESET}
              </Typography>
              <Typography variant="caption" sx={{ color: "text.secondary" }}>
                快速开始
              </Typography>
            </Paper>
            {rules.map((r) => (
              <Paper
                key={r.id}
                variant="outlined"
                sx={{ p: 1.5, cursor: "pointer" }}
                onClick={() => send(r.name)}
              >
                <Typography variant="body2" noWrap>
                  {r.name}
                </Typography>
                <Typography variant="caption" sx={{ color: "text.secondary" }}>
                  {r.object} · {r.action}
                </Typography>
              </Paper>
            ))}
            {rules.length === 0 && (
              <Typography
                variant="body2"
                sx={{ color: "text.secondary", textAlign: "center", py: 3 }}
              >
                暂无联动记录
              </Typography>
            )}
          </Box>
        </Paper>

        {/* Right: main chat area */}
        <Paper
          variant="outlined"
          sx={{ flex: 1, display: "flex", flexDirection: "column", overflow: "hidden" }}
        >
          {/* top bar: model selector + trash */}
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              justifyContent: "flex-end",
              gap: 1,
              p: 1.5,
            }}
          >
            <Select
              size="small"
              value={model}
              onChange={(e) => setModel(e.target.value)}
              sx={{
                width: 180,
                "& .MuiOutlinedInput-root": { borderRadius: 5, bgcolor: "action.hover" },
              }}
            >
              <MenuItem value="">qwen3.7-flash</MenuItem>
              {models.map((m) => (
                <MenuItem key={m.id} value={m.id}>
                  {m.name}
                </MenuItem>
              ))}
            </Select>
            <IconButton size="small">
              <DeleteOutlinedIcon fontSize="small" />
            </IconButton>
          </Box>

          {/* scrollable welcome + embedded form card */}
          <Box sx={{ flex: 1, overflowY: "auto", p: 3 }}>
            <Box sx={{ display: "flex", flexDirection: "column", alignItems: "center", mt: 4 }}>
              <Avatar sx={{ width: 64, height: 64, bgcolor: "primary.main", mb: 2.5 }}>
                <ArticleOutlinedIcon />
              </Avatar>
              <Typography variant="h6" sx={{ mb: 0.5 }}>
                你好，我是AI表单填写助手，我可以提供表单填写功能，例如：
              </Typography>
              <Typography variant="caption" sx={{ color: "text.secondary", my: 2 }}>
                快速开始
              </Typography>
              <Paper
                variant="outlined"
                sx={{
                  width: "100%",
                  maxWidth: 620,
                  p: 2,
                  display: "flex",
                  alignItems: "center",
                  gap: 1.5,
                  cursor: "pointer",
                  "&:hover": { borderColor: "primary.main" },
                }}
                onClick={() => send(PRESET)}
              >
                <InfoOutlinedIcon sx={{ color: "primary.main" }} />
                <Typography variant="body2" sx={{ flex: 1 }}>
                  {PRESET}
                </Typography>
                <ChevronRightIcon sx={{ color: "text.secondary" }} />
              </Paper>
            </Box>

            {/* Embedded confirmation form card */}
            <Box sx={{ maxWidth: 680, mx: "auto", mt: 4 }}>
              <Typography variant="body1" sx={{ mb: 1.5 }}>
                请检查表单信息是否正确，无误后点击确认
              </Typography>
              <Paper variant="outlined" sx={{ p: 2.5, borderStyle: "dashed" }}>
                <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 2 }}>
                  <ArticleOutlinedIcon fontSize="small" />
                  <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                    请确认表单信息是否正确
                  </Typography>
                </Box>
                <Box sx={{ display: "flex", gap: 2, mb: 1.5, flexWrap: "wrap" }}>
                  <Box
                    sx={{ display: "flex", alignItems: "center", gap: 1, flex: 1, minWidth: 220 }}
                  >
                    <Typography variant="body2">* 用户名</Typography>
                    <TextField size="small" defaultValue="tom" sx={{ flex: 1 }} />
                  </Box>
                  <Box
                    sx={{ display: "flex", alignItems: "center", gap: 1, flex: 1, minWidth: 220 }}
                  >
                    <Typography variant="body2">* 手机号</Typography>
                    <TextField size="small" defaultValue="13312341235" sx={{ flex: 1 }} />
                  </Box>
                </Box>
                <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 2.5, maxWidth: 360 }}>
                  <Typography variant="body2">* 密码</Typography>
                  <TextField size="small" type="password" defaultValue="123456" sx={{ flex: 1 }} />
                </Box>
                <Button variant="contained" size="small" onClick={() => send(PRESET)}>
                  确认提交
                </Button>
              </Paper>
            </Box>
          </Box>

          {/* bottom input bar */}
          <Box sx={{ p: 2 }}>
            <Paper
              elevation={2}
              sx={{ p: 0.75, display: "flex", alignItems: "center", borderRadius: 3, gap: 1 }}
            >
              <IconButton size="small">
                <AddIcon />
              </IconButton>
              <TextField
                fullWidth
                placeholder="请输入您的问题"
                variant="standard"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") send(input);
                }}
                slotProps={{ input: { disableUnderline: true } }}
              />
              <IconButton size="small">
                <MicIcon />
              </IconButton>
              <IconButton color="primary" onClick={() => send(input)}>
                <SendIcon />
              </IconButton>
            </Paper>
          </Box>
        </Paper>
      </Box>
    </PageStack>
  );
}

export default LinkageRules;
