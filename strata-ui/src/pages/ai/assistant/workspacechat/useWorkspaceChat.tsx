// SPDX-License-Identifier: Apache-2.0
// State and handlers of WorkspaceChat, split out of WorkspaceChat.tsx.

import { useEffect, useRef, useState, type ChangeEvent } from "react";
import { modelApi, type AiChatAttachment } from "@/shared/api/ai";
import type { AiModelDto } from "@/shared/types";
import { fileApi as systemFileApi } from "@/shared/api/system";
import { useAiChat } from "@/shared/hooks/useAiChat";
import { failureMessage } from "@/shared/hooks/useCrud";
import { useI18n } from "@/shared/i18n";
import { toast } from "@/shared/pageKit/Toast";

export function useWorkspaceChat() {
  const { t } = useI18n();

  const [model, setModel] = useState("");

  const [models, setModels] = useState<AiModelDto[]>([]);

  const [sessionSearch, setSessionSearch] = useState("");

  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null);

  useEffect(() => {
    modelApi
      .list()
      .then((ms) => {
        setModels(ms);
        const first = ms.find((m) => m.enabled && m.type === "chat");
        if (first) setModel(first.id);
      })
      .catch(() => {});
  }, []);

  const [attachments, setAttachments] = useState<AiChatAttachment[]>([]);

  const [uploading, setUploading] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const [deleteTargetId, setDeleteTargetId] = useState<string | null>(null);

  const [clearing, setClearing] = useState(false);

  const {
    activeId,
    busy,
    canCreate,
    createSession,
    deleteSession,
    input,
    loading,
    messages,
    selectSession,
    send,
    sessions,
    setInput,
  } = useAiChat({ sessionTitlePrefix: t("nav.workspace") });

  const visibleSessions = sessions.filter((s) => !sessionSearch || s.title.includes(sessionSearch));

  const handlePickFiles = async (event: ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? []);
    event.target.value = "";
    if (files.length === 0) return;
    setUploading(true);
    try {
      const uploaded: AiChatAttachment[] = [];
      for (const file of files) {
        const dto = await systemFileApi.upload(file);
        uploaded.push({
          bucket: dto.bucket,
          key: dto.key,
          contentType: dto.contentType ?? file.type,
          fileName: file.name,
          size: dto.size,
        });
      }
      setAttachments((prev) => [...prev, ...uploaded]);
    } catch (error) {
      toast.error(failureMessage(error));
    } finally {
      setUploading(false);
    }
  };

  const handleSend = async () => {
    const ok = await send(attachments, "");
    if (ok) setAttachments([]);
  };

  const clearAllSessions = async () => {
    setMenuAnchor(null);
    setClearing(true);
    try {
      for (const s of sessions) {
        try {
          await deleteSession(s.id);
        } catch {
          // ignore individual failures
        }
      }
      await createSession(model || null);
    } finally {
      setClearing(false);
    }
  };

  return {
    t,
    model,
    setModel,
    models,
    setModels,
    sessionSearch,
    setSessionSearch,
    menuAnchor,
    setMenuAnchor,
    attachments,
    setAttachments,
    uploading,
    setUploading,
    fileInputRef,
    deleteTargetId,
    setDeleteTargetId,
    clearing,
    setClearing,
    activeId,
    busy,
    canCreate,
    createSession,
    deleteSession,
    input,
    loading,
    messages,
    selectSession,
    send,
    sessions,
    setInput,
    visibleSessions,
    handlePickFiles,
    handleSend,
    clearAllSessions,
  };
}
