// SPDX-License-Identifier: Apache-2.0
// State and handlers of SkillCrud, split out of part-2.tsx.

import { useState } from "react";
import { useAuthStore } from "@/shared/auth/store";
import { skillApi, type AiSkillPayload, type AiSkillUpdatePayload } from "@/shared/api/ai";
import { usePagedQuery } from "@/shared/hooks/usePagedQuery";
import { useResourceCrud } from "@/shared/hooks/useResourceCrud";
import type { AiSkillDto } from "@/shared/types";
import { useNavigate } from "react-router-dom";
import { SkillForm } from "../part-1";

export function useSkillCrud({ type }: { type: "skill" | "interface" }) {
  const canCreate = useAuthStore((s) => s.hasAuthority("ai:skill:create"));

  const canUpdate = useAuthStore((s) => s.hasAuthority("ai:skill:update"));

  const canDelete = useAuthStore((s) => s.hasAuthority("ai:skill:delete"));

  const navigate = useNavigate();

  const [filterName, setFilterName] = useState("");

  const [filterEnabled, setFilterEnabled] = useState<string>("all");

  const [uploadOpen, setUploadOpen] = useState(false);

  const [supportOpen, setSupportOpen] = useState(false);

  const [supportSkill, setSupportSkill] = useState<AiSkillDto | null>(null);

  const {
    rows: skills,
    total,
    page,
    setPage,
    pageSize,
    setPageSize,
    reload,
  } = usePagedQuery<AiSkillDto>(
    ["api", "ai", "skills", { type, filterName, filterEnabled }],
    (pageIndex, size) => skillApi.page(type, pageIndex, size),
    { initialPageSize: 6 },
  );

  const {
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
  } = useResourceCrud<AiSkillDto, AiSkillPayload, AiSkillUpdatePayload, SkillForm>({
    create: (payload) => skillApi.create(payload),
    update: (id, payload) => skillApi.update(id, payload),
    remove: (id) => skillApi.remove(id),
    reload,
    initForm: () => ({ name: "", version: "1.0.0", description: "", content: "", enabled: true }),
    toEditForm: (s) => ({
      name: s.name,
      version: "1.0.0",
      description: s.description ?? "",
      content: "",
      enabled: s.enabled,
    }),
    toCreatePayload: (f) => f as unknown as AiSkillPayload,
    toUpdatePayload: (f) => f as unknown as AiSkillUpdatePayload,
  });

  const filtered = skills.filter((s) => {
    if (filterName && !s.name.includes(filterName)) return false;
    if (filterEnabled === "yes" && !s.enabled) return false;
    if (filterEnabled === "no" && s.enabled) return false;
    return true;
  });

  return {
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
  };
}
