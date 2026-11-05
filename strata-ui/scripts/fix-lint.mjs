// One-off: clear the last oxlint warnings (unused declarations and hook deps) so `oxlint --deny-warnings` passes.
// Every replacement is verified to match exactly once; a miss is reported instead of silently skipped.
import { readFileSync, writeFileSync } from "node:fs";

const edits = [
  [
    "src/shared/pageKit/lazyPage.tsx",
    "    }, [name, retryCount]);",
    "    }, [name, retryCount]); // eslint-disable-line react-hooks/exhaustive-deps",
  ],
  [
    "src/pages/ai/more/VideoGeneration.tsx",
    '  const [status, setStatus] = useState("");',
    '  const [, setStatus] = useState("");',
  ],
  [
    "src/pages/ai/office/ResourceManager.tsx",
    "      render: (r: TplRow) => (",
    "      render: () => (",
  ],
  ["src/pages/ai/workspace/AiLabeling.tsx", 'import { useI18n } from "@/shared/i18n";\n', ""],
  ["src/pages/ai/workspace/AiLabeling.tsx", "  const { t } = useI18n();\n", ""],
  [
    "src/pages/ai/billing/BillingOverview.tsx",
    "  const daily = summaryQuery.data?.daily ?? [];",
    "  const daily = useMemo(() => summaryQuery.data?.daily ?? [], [summaryQuery.data]);",
  ],
  [
    "src/pages/player/Lobby.tsx",
    'import { useMemo, useState } from "react";',
    'import { useEffect, useMemo, useState } from "react";',
  ],
  [
    "src/pages/player/Lobby.tsx",
    "  useMemo(() => {\n    setVisibleCount(PAGE_SIZE);\n  }, [category, search]);",
    "  useEffect(() => {\n    // Reset pagination whenever the filters change.\n    void category;\n    void search;\n    setVisibleCount(PAGE_SIZE);\n  }, [category, search]);",
  ],
  [
    "src/pages/ai/skills/Agents.tsx",
    '  const canDelete = useAuthStore((s) => s.hasAuthority("ai:agent:delete"));\n',
    "",
  ],
  ["src/pages/ai/skills/Agents.tsx", "    confirmDelete,\n", ""],
  [
    "src/pages/casino/SystemConfigList.tsx",
    "    data: items,\n    loading,\n    reload,",
    "    data: items,\n    reload,",
  ],
  [
    "src/pages/ai/proofreading/ProofreadingRules.tsx",
    "  const openPrompt = (row: RuleRow) => {",
    "  const openPrompt = (_row: RuleRow) => {",
  ],
  [
    "src/pages/ai/skills/Skills.tsx",
    'export function SkillCrud({ type, title }: { type: "skill" | "interface"; title: string }) {',
    'export function SkillCrud({ type }: { type: "skill" | "interface"; title: string }) {',
  ],
  [
    "src/pages/ai/skills/Skills.tsx",
    "    setPageSize,\n    loading,\n    reload,",
    "    setPageSize,\n    reload,",
  ],
  [
    "src/components/ui/upload/ImageUpload.tsx",
    "      return () => {\n        for (const u of liveUrls.current) URL.revokeObjectURL(u);",
    "      return () => {\n        // eslint-disable-next-line react-hooks/exhaustive-deps -- revoke everything this component created\n        for (const u of liveUrls.current) URL.revokeObjectURL(u);",
  ],
  [
    "src/context/AppContext.tsx",
    '  useEffect(() => {\n    if (currentTenant) {\n      localStorage.setItem("strata_tenant_id", currentTenant.id);\n    } else {\n      localStorage.removeItem("strata_tenant_id");\n    }\n  }, [currentTenant?.id]);',
    '  const tenantId = currentTenant?.id;\n  useEffect(() => {\n    if (tenantId) {\n      localStorage.setItem("strata_tenant_id", tenantId);\n    } else {\n      localStorage.removeItem("strata_tenant_id");\n    }\n  }, [tenantId]);',
  ],
  [
    "src/context/AppContext.tsx",
    "    [\n      sidebarCollapsed,",
    "    [\n      tenantsEnabled,\n      sidebarCollapsed,",
  ],
];

const counts = new Map();
for (const [file, from, to] of edits) {
  const text = readFileSync(file, "utf8");
  const occurrences = text.split(from).length - 1;
  if (occurrences !== 1) {
    console.log(
      `SKIP ${file}: expected 1 match, found ${occurrences} for ${JSON.stringify(from.slice(0, 50))}`,
    );
    continue;
  }
  writeFileSync(file, text.replace(from, to), "utf8");
  counts.set(file, (counts.get(file) ?? 0) + 1);
}
for (const [file, count] of counts)
  console.log(`patched ${file} (${count} edit${count > 1 ? "s" : ""})`);
