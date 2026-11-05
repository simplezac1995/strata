// SPDX-License-Identifier: Apache-2.0
import { useRef, useState } from "react";
import {
  Refresh as ResetIcon,
  ContentCopy as CopyIcon,
  Send as SendIcon,
  SmartToy as AgentIcon,
} from "@mui/icons-material";
import { Avatar, Box, Chip, Paper, Stack, Typography } from "@mui/material";
import { Button, Select, TextField, TextArea } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";
import { officeApi } from "@/shared/api/ai";
import { toast } from "@/shared/pageKit/Toast";

interface ChatMsg {
  role: "user" | "agent";
  text: string;
}

const STAGES = [
  { value: "initial", label: "初步接洽" },
  { value: "intent", label: "意向阶段" },
  { value: "quote", label: "报价阶段" },
  { value: "deal", label: "成交" },
];

const FOLLOW_STATUS = [
  { value: "pending", label: "待跟进" },
  { value: "following", label: "跟进中" },
  { value: "done", label: "已成交" },
];

export function SuperForm() {
  // CRM form state
  const [customer, setCustomer] = useState("上海云启科技有限公司");
  const [contact, setContact] = useState("王晓峰");
  const [phone, setPhone] = useState("13800138000");
  const [product, setProduct] = useState("PIG AI 知识库");
  const [stage, setStage] = useState("initial");
  const [followStatus, setFollowStatus] = useState("pending");
  const [amount, setAmount] = useState("50000");
  const [nextFollow, setNextFollow] = useState("2026-07-06T10:00");
  const [note, setNote] = useState("客户关注私有化部署和权限控制，需补充报价方案。");

  // agent panel chat state
  const [messages, setMessages] = useState<ChatMsg[]>([
    { role: "agent", text: "可以直接描述客户最新跟进情况，我会更新左侧 CRM 表单。" },
  ]);
  const [draft, setDraft] = useState("");
  const [sending, setSending] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  const stageLabel = STAGES.find((s) => s.value === stage)?.label ?? stage;

  function resetForm() {
    setCustomer("");
    setContact("");
    setPhone("");
    setProduct("");
    setAmount("");
    setNote("");
    toast.success("已重置");
  }

  function copySummary() {
    const text = `${customer} · ${contact} · ${phone}\n阶段：${stageLabel}\n预计金额：¥${amount}\n跟进备注：${note}`;
    void navigator.clipboard?.writeText(text).catch(() => undefined);
    toast.success("摘要已复制");
  }

  async function send() {
    const text = draft.trim();
    if (!text || sending) return;
    setMessages((prev) => [...prev, { role: "user", text }]);
    setDraft("");
    setSending(true);
    try {
      const res = await officeApi.run("rewrite", text);
      setMessages((prev) => [
        ...prev,
        { role: "agent", text: res.content || "已收到你的跟进描述，我会据此更新左侧字段。" },
      ]);
    } catch (e) {
      setMessages((prev) => [
        ...prev,
        { role: "agent", text: `处理失败: ${e instanceof Error ? e.message : String(e)}` },
      ]);
    } finally {
      setSending(false);
      bottomRef.current?.scrollIntoView({ behavior: "smooth" });
    }
  }

  return (
    <PageStack>
      <Box sx={{ display: "flex", gap: 2, alignItems: "flex-start" }}>
        {/* Left: CRM form */}
        <Paper variant="outlined" sx={{ flex: 1, p: 2.5, borderRadius: 2 }}>
          <Stack
            direction="row"
            sx={{ alignItems: "flex-start", justifyContent: "space-between", mb: 2 }}
          >
            <Box>
              <Stack direction="row" spacing={1} sx={{ alignItems: "center", mb: 0.5 }}>
                <Typography variant="h6">CRM 跟进助手</Typography>
                <Chip label={stageLabel} size="small" color="warning" variant="outlined" />
                <Chip
                  label={FOLLOW_STATUS.find((s) => s.value === followStatus)?.label}
                  size="small"
                  variant="outlined"
                />
              </Stack>
              <Typography variant="body2" sx={{ color: "text.secondary" }}>
                {customer} · {contact} · {phone}
              </Typography>
            </Box>
            <Stack direction="row" spacing={1}>
              <Button
                variant="outlined"
                color="default"
                size="small"
                startIcon={<ResetIcon />}
                onClick={resetForm}
              >
                重置
              </Button>
              <Button
                variant="outlined"
                color="default"
                size="small"
                startIcon={<CopyIcon />}
                onClick={copySummary}
              >
                复制摘要
              </Button>
            </Stack>
          </Stack>

          {/* KPI row */}
          <Box sx={{ display: "flex", gap: 1.5, mb: 2.5 }}>
            {[
              { label: "预计金额", value: `¥${Number(amount || 0).toLocaleString()}` },
              { label: "意向产品", value: product || "-" },
              { label: "下次跟进", value: nextFollow.replace("T", " ") || "-" },
            ].map((k) => (
              <Paper key={k.label} variant="outlined" sx={{ flex: 1, p: 1.5 }}>
                <Typography variant="caption" sx={{ color: "text.secondary" }}>
                  {k.label}
                </Typography>
                <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                  {k.value}
                </Typography>
              </Paper>
            ))}
          </Box>

          {/* 客户档案 */}
          <Stack
            direction="row"
            sx={{ alignItems: "center", justifyContent: "space-between", mb: 1 }}
          >
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
              客户档案
            </Typography>
            <Typography variant="caption" sx={{ color: "text.secondary" }}>
              基础信息
            </Typography>
          </Stack>
          <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 1.5, mb: 2.5 }}>
            <TextField
              label="客户名称"
              value={customer}
              onChange={(e) => setCustomer(e.target.value)}
            />
            <TextField
              label="联系人"
              value={contact}
              onChange={(e) => setContact(e.target.value)}
            />
            <TextField label="手机号" value={phone} onChange={(e) => setPhone(e.target.value)} />
            <TextField
              label="意向产品"
              value={product}
              onChange={(e) => setProduct(e.target.value)}
            />
          </Box>

          {/* 跟进状态 */}
          <Stack
            direction="row"
            sx={{ alignItems: "center", justifyContent: "space-between", mb: 1 }}
          >
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
              跟进状态
            </Typography>
            <Typography variant="caption" sx={{ color: "text.secondary" }}>
              Agent 可直接更新
            </Typography>
          </Stack>
          <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 1.5 }}>
            <Select
              label="客户阶段"
              value={stage}
              onChange={(e) => setStage(e.target.value)}
              options={STAGES}
            />
            <Select
              label="跟进状态"
              value={followStatus}
              onChange={(e) => setFollowStatus(e.target.value)}
              options={FOLLOW_STATUS}
            />
            <TextField
              label="预计金额"
              type="number"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
            />
            <TextField
              label="下次跟进"
              type="datetime-local"
              value={nextFollow}
              onChange={(e) => setNextFollow(e.target.value)}
            />
          </Box>
          <Box sx={{ mt: 1.5 }}>
            <TextArea
              label="跟进备注"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              rows={3}
            />
          </Box>
        </Paper>

        {/* Right: Agent Panel */}
        <Paper
          variant="outlined"
          sx={{
            width: 400,
            flexShrink: 0,
            p: 2.5,
            borderRadius: 2,
            display: "flex",
            flexDirection: "column",
            height: "calc(100vh - 200px)",
          }}
        >
          <Stack
            direction="row"
            sx={{ alignItems: "center", justifyContent: "space-between", mb: 0.5 }}
          >
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
              Agent Panel
            </Typography>
            <Chip label="idle" size="small" color="success" variant="outlined" />
          </Stack>
          <Typography variant="caption" sx={{ color: "text.secondary", mb: 2 }}>
            描述跟进进展，Agent 会更新左侧字段
          </Typography>

          <Box
            sx={{ flex: 1, overflowY: "auto", display: "flex", flexDirection: "column", gap: 1.5 }}
          >
            {messages.map((m, i) =>
              m.role === "user" ? (
                <Box key={i} sx={{ alignSelf: "flex-end", maxWidth: "85%" }}>
                  <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2 }}>
                    <Typography variant="body2">{m.text}</Typography>
                  </Paper>
                </Box>
              ) : (
                <Box key={i} sx={{ alignSelf: "flex-start", maxWidth: "90%" }}>
                  <Stack direction="row" spacing={1} sx={{ alignItems: "center", mb: 0.5 }}>
                    <Avatar sx={{ width: 24, height: 24, bgcolor: "primary.main" }}>
                      <AgentIcon sx={{ fontSize: 14 }} />
                    </Avatar>
                    <Typography variant="caption" sx={{ color: "text.secondary" }}>
                      Agent
                    </Typography>
                  </Stack>
                  <Paper
                    variant="outlined"
                    sx={{ p: 1.5, borderRadius: 2, bgcolor: "action.hover" }}
                  >
                    <Typography variant="body2">{m.text}</Typography>
                  </Paper>
                </Box>
              ),
            )}
            <div ref={bottomRef} />
          </Box>

          <Box sx={{ pt: 1.5 }}>
            <TextField
              placeholder="例如：客户预算 8 万，下周二回访，阶段改成商机推进。"
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") void send();
              }}
              multiline
              minRows={2}
            />
            <Stack direction="row" sx={{ justifyContent: "space-between", mt: 1 }}>
              <Button
                variant="text"
                size="small"
                onClick={() => setMessages([{ role: "agent", text: "对话已清空。" }])}
              >
                清空对话
              </Button>
              <Button
                variant="contained"
                size="small"
                disabled={!draft.trim() || sending}
                startIcon={<SendIcon />}
                onClick={() => void send()}
              >
                {sending ? "发送中..." : "发送"}
              </Button>
            </Stack>
          </Box>
        </Paper>
      </Box>
    </PageStack>
  );
}

export default SuperForm;
