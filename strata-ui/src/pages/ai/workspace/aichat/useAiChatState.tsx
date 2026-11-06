// SPDX-License-Identifier: Apache-2.0
// State and handlers of AiChat, split out of AiChat.tsx.

import { useEffect, useMemo, useRef, useState } from "react";
import { useI18n } from "@/shared/i18n";
import { useAiChat } from "@/shared/hooks/useAiChat";
import { fileApi } from "@/shared/api/system";
import { aiChatApi, modelApi, type AiChatAttachment } from "@/shared/api/ai";
import type { AiModelDto, FileInfoDto } from "@/shared/types";
import { useAuthStore } from "@/shared/auth/store";
import { failureMessage } from "@/shared/hooks/useCrud";
import { toast } from "@/shared/pageKit/Toast";
import { error as loggerError } from "@/shared/logger";
import { SessionFilter } from "./part-1";

export function useAiChatState() {
  const { t } = useI18n();

  const currentUserId = useAuthStore((s) => s.user?.id ?? null);

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
  } = useAiChat({ sessionTitlePrefix: t("ai.workspace.sessions") });

  const messagesEndRef = useRef<HTMLDivElement>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const [uploadedFiles, setUploadedFiles] = useState<FileInfoDto[]>([]);

  const [uploading, setUploading] = useState(false);

  const [deleteTargetId, setDeleteTargetId] = useState<string | null>(null);

  const [search, setSearch] = useState("");

  const [filter, setFilter] = useState<SessionFilter>("all");

  const [editingId, setEditingId] = useState<string | null>(null);

  const [editTitle, setEditTitle] = useState("");

  const editInputRef = useRef<HTMLInputElement | null>(null);

  const [models, setModels] = useState<AiModelDto[]>([]);

  const [selectedModel, setSelectedModel] = useState<string>("");

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, activeId]);

  useEffect(() => {
    modelApi
      .list()
      .then((ms) => {
        setModels(ms);
        if (ms.length > 0 && !selectedModel) setSelectedModel(ms[0].id);
      })
      .catch(() => {});
  }, [selectedModel]);

  useEffect(() => {
    if (editingId && editInputRef.current) {
      editInputRef.current.focus();
      editInputRef.current.select();
    }
  }, [editingId]);

  const visibleSessions = useMemo(() => {
    const q = search.trim().toLowerCase();
    return sessions.filter((s) => {
      if (q && !s.title.toLowerCase().includes(q)) return false;
      if (filter === "mine") return currentUserId != null && s.userId === currentUserId;
      if (filter === "shared") return currentUserId == null || s.userId !== currentUserId;
      return true;
    });
  }, [sessions, search, filter, currentUserId]);

  async function commitRename() {
    if (!editingId) return;
    const title = editTitle.trim();
    const original = sessions.find((s) => s.id === editingId);
    if (title && title !== original?.title) {
      try {
        await aiChatApi.renameSession(editingId, title);
        toast.success("toast.saved");
      } catch (err) {
        toast.error(failureMessage(err, "toast.saveFailed"));
      }
    }
    setEditingId(null);
    setEditTitle("");
  }

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    setUploading(true);
    try {
      const newFiles: FileInfoDto[] = [];
      for (const file of Array.from(files)) {
        const result = await fileApi.upload(file, "ai-chat");
        newFiles.push(result);
      }
      setUploadedFiles((prev) => [...prev, ...newFiles]);
    } catch (error) {
      loggerError("ai chat upload failed", error);
    } finally {
      setUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  const removeUploadedFile = (index: number) => {
    setUploadedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSend = async () => {
    const attachments: AiChatAttachment[] = uploadedFiles.map((file) => ({
      bucket: file.bucket,
      key: file.key,
      contentType: file.contentType ?? null,
      size: file.size,
      fileName: file.key.split("/").pop() ?? file.key,
    }));
    const ok = await send(attachments);
    if (ok) {
      setUploadedFiles([]);
    }
  };

  const handleRetry = async () => {
    const lastUser = [...messages].reverse().find((m) => m.role === "user");
    if (!lastUser || busy) return;
    setInput(lastUser.content);
    await send([]);
  };

  return {
    t,
    currentUserId,
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
    messagesEndRef,
    fileInputRef,
    uploadedFiles,
    setUploadedFiles,
    uploading,
    setUploading,
    deleteTargetId,
    setDeleteTargetId,
    search,
    setSearch,
    filter,
    setFilter,
    editingId,
    setEditingId,
    editTitle,
    setEditTitle,
    editInputRef,
    models,
    setModels,
    selectedModel,
    setSelectedModel,
    visibleSessions,
    commitRename,
    handleFileUpload,
    removeUploadedFile,
    handleSend,
    handleRetry,
  };
}
