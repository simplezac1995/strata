// SPDX-License-Identifier: Apache-2.0
import { useState } from "react";
import {
  Box,
  CircularProgress,
  Divider,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Stack,
  Step,
  StepLabel,
  Stepper,
  Tooltip as MuiTooltip,
  Typography,
} from "@mui/material";
import ArrowUpwardIcon from "@mui/icons-material/ArrowUpward";
import ArrowDownwardIcon from "@mui/icons-material/ArrowDownward";
import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import SaveIcon from "@mui/icons-material/Save";
import AutoAwesomeIcon from "@mui/icons-material/AutoAwesome";
import BuildIcon from "@mui/icons-material/Build";
import CallSplitIcon from "@mui/icons-material/CallSplit";
import HubIcon from "@mui/icons-material/Hub";
import { Button, TextField, TextArea } from "@/components/ui/index";
import { AiPageHeader } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import {
  aiAgentCanvasApi,
  type AiAgentCanvasNode,
  type AiAgentNodeType,
  type AiAgentRunResult,
} from "@/shared/api/aiAgentCanvas";
import { failureMessage } from "@/shared/hooks/useCrud";
import { toast } from "@/shared/pageKit/Toast";
import { PageStack, ContentCard } from "@/shared/pageKit/PageLayout";

const AGENT_ID = "canvas";

const NODE_TYPES: { type: AiAgentNodeType; labelKey: string; icon: React.ReactNode }[] = [
  { type: "llm", labelKey: "ai.agents.nodeLlm", icon: <AutoAwesomeIcon fontSize="small" /> },
  { type: "tool", labelKey: "ai.agents.nodeTool", icon: <BuildIcon fontSize="small" /> },
  { type: "branch", labelKey: "ai.agents.nodeBranch", icon: <CallSplitIcon fontSize="small" /> },
  { type: "subagent", labelKey: "ai.agents.nodeSubagent", icon: <HubIcon fontSize="small" /> },
];

let nodeSeq = 1;

export function AgentCanvas() {
  const { t } = useI18n();
  const [nodes, setNodes] = useState<AiAgentCanvasNode[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [saving, setSaving] = useState(false);
  const [runInput, setRunInput] = useState("");
  const [runResult, setRunResult] = useState<AiAgentRunResult | null>(null);
  const [rightTab, setRightTab] = useState<"properties" | "result">("properties");

  const selected = nodes.find((n) => n.id === selectedId) ?? null;

  function addNode(type: AiAgentNodeType) {
    const node: AiAgentCanvasNode = {
      id: `node-${nodeSeq++}`,
      type,
      label: t(NODE_TYPES.find((n) => n.type === type)?.labelKey ?? type),
      config: {},
    };
    setNodes((prev) => [...prev, node]);
    setSelectedId(node.id);
  }

  function move(id: string, dir: -1 | 1) {
    setNodes((prev) => {
      const idx = prev.findIndex((n) => n.id === id);
      const target = idx + dir;
      if (idx < 0 || target < 0 || target >= prev.length) return prev;
      const next = [...prev];
      [next[idx], next[target]] = [next[target], next[idx]];
      return next;
    });
  }

  function updateSelected(patch: Partial<AiAgentCanvasNode>) {
    if (!selected) return;
    setNodes((prev) => prev.map((n) => (n.id === selected.id ? { ...n, ...patch } : n)));
  }

  async function runAgent() {
    if (busy) return;
    setBusy(true);
    setRunResult(null);
    try {
      const res = await aiAgentCanvasApi.run(AGENT_ID, runInput);
      setRunResult(res);
      setRightTab("result");
      toast.success("toast.saved");
    } catch (err) {
      toast.error(failureMessage(err, "toast.saveFailed"));
    } finally {
      setBusy(false);
    }
  }

  async function saveNodes() {
    if (saving) return;
    setSaving(true);
    try {
      await aiAgentCanvasApi.saveNodes(AGENT_ID, nodes);
      toast.success("toast.saved");
    } catch (err) {
      toast.error(failureMessage(err, "toast.saveFailed"));
    } finally {
      setSaving(false);
    }
  }

  return (
    <PageStack>
      <AiPageHeader
        title={t("ai.agents.title")}
        subtitle={t("ai.agents.subtitle")}
        actions={
          <Stack direction="row" spacing={1}>
            <MuiTooltip title={t("ai.agents.saveTooltip")} arrow>
              <span>
                <Button variant="outlined" onClick={() => void saveNodes()} disabled={saving}>
                  <SaveIcon fontSize="small" sx={{ mr: 0.5 }} />
                  {saving ? t("common.savingEllipsis") : t("ai.agents.save")}
                </Button>
              </span>
            </MuiTooltip>
            <MuiTooltip title={t("ai.agents.runTooltip")} arrow>
              <span>
                <Button variant="contained" onClick={() => void runAgent()} disabled={busy}>
                  <PlayArrowIcon fontSize="small" sx={{ mr: 0.5 }} />
                  {busy ? t("ai.common.runningEllipsis") : t("ai.agents.run")}
                </Button>
              </span>
            </MuiTooltip>
          </Stack>
        }
      />

      <Box sx={{ display: "flex", gap: 2.5, height: 600 }}>
        {/* Left: node type palette */}
        <ContentCard padding={1.5} sx={{ width: 220, flexShrink: 0, overflowY: "auto" }}>
          <Typography
            variant="caption"
            color="text.secondary"
            sx={{ px: 0.5, display: "block", mb: 1 }}
          >
            {t("ai.agents.nodeTypes")}
          </Typography>
          <Stack spacing={1}>
            {NODE_TYPES.map((nt) => (
              <Button
                key={nt.type}
                variant="outlined"
                size="sm"
                startIcon={nt.icon}
                onClick={() => addNode(nt.type)}
                sx={{ justifyContent: "flex-start" }}
              >
                {t(nt.labelKey)}
              </Button>
            ))}
          </Stack>
        </ContentCard>

        {/* Middle: run input + node chain */}
        <ContentCard padding={1.5} sx={{ flex: 1, minWidth: 0, overflowY: "auto" }}>
          <TextField
            label={t("ai.agents.runInputLabel")}
            value={runInput}
            onChange={(e) => setRunInput(e.target.value)}
            placeholder={t("ai.agents.runInputPlaceholder")}
            disabled={busy}
            sx={{ mb: 1.5 }}
          />
          <Typography
            variant="caption"
            color="text.secondary"
            sx={{ px: 0.5, display: "block", mb: 1 }}
          >
            {t("ai.agents.chain")}
          </Typography>
          {nodes.length === 0 ? (
            <Typography variant="body2" color="text.secondary" sx={{ pt: 4, textAlign: "center" }}>
              {t("ai.agents.emptyHint")}
            </Typography>
          ) : (
            <List dense>
              {nodes.map((node, idx) => (
                <Box key={node.id}>
                  <ListItem
                    disablePadding
                    secondaryAction={
                      <Stack direction="row" spacing={0.25}>
                        <IconButton
                          size="small"
                          aria-label={t("common.moveUp")}
                          disabled={idx === 0}
                          onClick={() => move(node.id, -1)}
                        >
                          <ArrowUpwardIcon fontSize="small" />
                        </IconButton>
                        <IconButton
                          size="small"
                          aria-label={t("common.moveDown")}
                          disabled={idx === nodes.length - 1}
                          onClick={() => move(node.id, 1)}
                        >
                          <ArrowDownwardIcon fontSize="small" />
                        </IconButton>
                      </Stack>
                    }
                  >
                    <ListItemButton
                      selected={node.id === selectedId}
                      onClick={() => {
                        setSelectedId(node.id);
                        setRightTab("properties");
                      }}
                    >
                      <ListItemText
                        primary={`${idx + 1}. ${node.label}`}
                        secondary={t(
                          NODE_TYPES.find((n) => n.type === node.type)?.labelKey ?? node.type,
                        )}
                      />
                    </ListItemButton>
                  </ListItem>
                  {idx < nodes.length - 1 && <Divider variant="inset" component="li" />}
                </Box>
              ))}
            </List>
          )}
        </ContentCard>

        {/* Right: properties or run result */}
        <ContentCard padding={1.5} sx={{ width: 320, flexShrink: 0, overflowY: "auto" }}>
          <Stack
            direction="row"
            spacing={1}
            sx={{ mb: 1.5, borderBottom: 1, borderColor: "divider" }}
          >
            <Button
              variant={rightTab === "properties" ? "contained" : "text"}
              size="sm"
              onClick={() => setRightTab("properties")}
            >
              {t("ai.agents.properties")}
            </Button>
            <Button
              variant={rightTab === "result" ? "contained" : "text"}
              size="sm"
              onClick={() => setRightTab("result")}
            >
              {t("ai.agents.runResult")}
            </Button>
          </Stack>

          {rightTab === "properties" ? (
            !selected ? (
              <Typography variant="body2" color="text.secondary">
                {t("ai.agents.selectNodeHint")}
              </Typography>
            ) : (
              <Stack spacing={2}>
                <TextField
                  label={t("ai.agents.nodeLabel")}
                  value={selected.label}
                  onChange={(e) => updateSelected({ label: e.target.value })}
                />
                <TextField
                  label={t("ai.agents.nodeType")}
                  value={t(
                    NODE_TYPES.find((n) => n.type === selected.type)?.labelKey ?? selected.type,
                  )}
                  disabled
                />
                <TextArea
                  label={t("ai.agents.prompt")}
                  value={String(selected.config.prompt ?? "")}
                  onChange={(e) =>
                    updateSelected({ config: { ...selected.config, prompt: e.target.value } })
                  }
                  rows={4}
                />
              </Stack>
            )
          ) : busy ? (
            <Stack spacing={1.5} sx={{ alignItems: "center", pt: 4 }}>
              <CircularProgress size={28} />
              <Typography variant="caption" color="text.secondary">
                {t("ai.common.runningEllipsis")}
              </Typography>
            </Stack>
          ) : !runResult ? (
            <Typography variant="body2" color="text.secondary">
              {t("ai.agents.runResultEmpty")}
            </Typography>
          ) : (
            <Stack spacing={2}>
              <Box>
                <Typography
                  variant="caption"
                  color="text.secondary"
                  sx={{ display: "block", mb: 0.5 }}
                >
                  {t("ai.agents.finalOutput")}
                </Typography>
                <Typography
                  variant="body2"
                  sx={{ whiteSpace: "pre-wrap", bgcolor: "action.hover", p: 1.5, borderRadius: 1 }}
                >
                  {runResult.finalOutput || t("ai.agents.runResultEmpty")}
                </Typography>
              </Box>
              <Box>
                <Typography
                  variant="caption"
                  color="text.secondary"
                  sx={{ display: "block", mb: 0.5 }}
                >
                  {t("ai.agents.steps")}
                </Typography>
                <Stepper
                  activeStep={runResult.steps.findIndex((s) => s.status === "running")}
                  orientation="vertical"
                >
                  {runResult.steps.map((step, idx) => (
                    <Step key={`${step.nodeId}-${idx}`}>
                      <StepLabel
                        error={step.status === "failed"}
                        optional={
                          step.error ? (
                            <Typography variant="caption" color="error">
                              {step.error}
                            </Typography>
                          ) : step.output ? (
                            <Typography variant="caption" color="text.secondary">
                              {step.output.length > 80
                                ? `${step.output.slice(0, 80)}…`
                                : step.output}
                            </Typography>
                          ) : undefined
                        }
                      >
                        {step.label}
                        <Typography
                          variant="caption"
                          color="text.secondary"
                          sx={{ display: "block" }}
                        >
                          {t(`ai.agents.stepStatus.${step.status}`)}
                        </Typography>
                      </StepLabel>
                    </Step>
                  ))}
                </Stepper>
              </Box>
            </Stack>
          )}
        </ContentCard>
      </Box>
    </PageStack>
  );
}

export default AgentCanvas;
