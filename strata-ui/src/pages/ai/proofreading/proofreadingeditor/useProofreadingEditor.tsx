// SPDX-License-Identifier: Apache-2.0
// State and handlers of ProofreadingEditor, split out of ProofreadingEditor.tsx.

import { useMemo, useState } from "react";
import { aiToolApi } from "@/shared/api/ai";
import { failureMessage } from "@/shared/hooks/useCrud";
import { toast } from "@/shared/pageKit/Toast";
import { RULE_GROUPS } from "./part-1";

export function useProofreadingEditor() {
  const [originalText, setOriginalText] = useState("");

  const [busy, setBusy] = useState(false);

  const [checked, setChecked] = useState(false);

  const [resultText, setResultText] = useState("");

  const [checkedAt, setCheckedAt] = useState("");

  const [tab, setTab] = useState("通用");

  const [rulesOpen, setRulesOpen] = useState(false);

  const [enabledRules, setEnabledRules] = useState<Record<string, boolean>>(() => {
    const init: Record<string, boolean> = {};
    for (const g of RULE_GROUPS) for (const it of g.items) init[it.name] = true;
    return init;
  });

  const charCount = useMemo(() => originalText.length, [originalText]);

  const selectedCount = useMemo(
    () => Object.values(enabledRules).filter(Boolean).length,
    [enabledRules],
  );

  const runCheck = async () => {
    if (!originalText.trim()) {
      toast.info("请先粘贴或上传需要检查的原文");
      return;
    }
    setBusy(true);
    try {
      const res = await aiToolApi.run("proofread", originalText);
      setResultText(res.result ?? "");
      setChecked(true);
      setCheckedAt(new Date().toLocaleString("zh-CN", { hour12: false }));
    } catch (error) {
      toast.error(failureMessage(error));
    } finally {
      setBusy(false);
    }
  };

  const toggleRule = (key: string) => setEnabledRules((p) => ({ ...p, [key]: !p[key] }));

  return {
    originalText,
    setOriginalText,
    busy,
    setBusy,
    checked,
    setChecked,
    resultText,
    setResultText,
    checkedAt,
    setCheckedAt,
    tab,
    setTab,
    rulesOpen,
    setRulesOpen,
    enabledRules,
    setEnabledRules,
    charCount,
    selectedCount,
    runCheck,
    toggleRule,
  };
}
