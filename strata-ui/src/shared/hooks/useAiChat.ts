// SPDX-License-Identifier: Apache-2.0
import { useCallback, useEffect, useRef, useState } from "react";
import { aiChatApi, type AiChatAttachment } from "@/shared/api/ai";
import { useAuthStore } from "@/shared/auth/store";
import { failureMessage } from "@/shared/hooks/useCrud";
import { toast } from "@/shared/pageKit/Toast";
import type { AiChatMessageDto, AiChatSessionDto } from "@/shared/types";

interface UseAiChatOptions {
  sessionTitlePrefix: string;
}

export function useAiChat({ sessionTitlePrefix }: UseAiChatOptions) {
  const canCreate = useAuthStore((state) => state.hasAuthority("ai:chat:create"));
  const [sessions, setSessions] = useState<AiChatSessionDto[]>([]);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [messages, setMessages] = useState<AiChatMessageDto[]>([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const activeIdRef = useRef<string | null>(null);
  const messageRequestRef = useRef(0);

  useEffect(() => {
    activeIdRef.current = activeId;
  }, [activeId]);

  const loadMessages = useCallback(async (sessionId: string) => {
    const requestId = ++messageRequestRef.current;
    try {
      const nextMessages = await aiChatApi.listMessages(sessionId);
      if (requestId === messageRequestRef.current) {
        setMessages(nextMessages);
      }
    } catch {
      toast.error("toast.loadFailed");
    }
  }, []);

  const loadSessions = useCallback(
    async (selectId?: string | null) => {
      try {
        const nextSessions = await aiChatApi.listSessions();
        setSessions(nextSessions);
        const nextId = selectId ?? activeIdRef.current ?? nextSessions[0]?.id ?? null;
        activeIdRef.current = nextId;
        setActiveId(nextId);
        if (nextId) {
          await loadMessages(nextId);
        } else {
          messageRequestRef.current++;
          setMessages([]);
        }
      } catch {
        toast.error("toast.loadFailed");
      } finally {
        setLoading(false);
      }
    },
    [loadMessages],
  );

  useEffect(() => {
    void loadSessions();
  }, [loadSessions]);

  const selectSession = useCallback(
    async (sessionId: string) => {
      activeIdRef.current = sessionId;
      setActiveId(sessionId);
      await loadMessages(sessionId);
    },
    [loadMessages],
  );

  const createSession = useCallback(
    async (modelId?: string | null) => {
      if (!canCreate) return;
      setBusy(true);
      try {
        const session = await aiChatApi.createSession(
          `${sessionTitlePrefix} ${new Date().toLocaleString()}`,
          modelId ?? null,
        );
        await loadSessions(session.id);
      } catch (error) {
        toast.error(failureMessage(error));
      } finally {
        setBusy(false);
      }
    },
    [canCreate, loadSessions, sessionTitlePrefix],
  );

  const send = useCallback(
    async (
      attachments: AiChatAttachment[] = [],
      prefix = "",
      overrideContent?: string,
    ): Promise<boolean> => {
      const content = (prefix + (overrideContent ?? input)).trim();
      const sessionId = activeIdRef.current;
      if ((!content && attachments.length === 0) || !sessionId || !canCreate) return false;

      setBusy(true);
      setInput("");
      const optimistic: AiChatMessageDto = {
        id: `tmp-${Date.now()}`,
        sessionId,
        role: "user",
        content,
        createdAt: new Date().toISOString(),
      };
      setMessages((current) => [...current, optimistic]);
      // Add an empty assistant placeholder that fills in via stream
      const assistantId = `tmp-asst-${Date.now()}`;
      setMessages((current) => [
        ...current,
        {
          id: assistantId,
          sessionId,
          role: "assistant",
          content: "",
          createdAt: new Date().toISOString(),
        },
      ]);
      try {
        await aiChatApi.sendMessageStream(sessionId, content, (chunk) => {
          setMessages((current) =>
            current.map((m) => (m.id === assistantId ? { ...m, content: m.content + chunk } : m)),
          );
        });
        await loadMessages(sessionId);
        return true;
      } catch (error) {
        toast.error(failureMessage(error));
        setMessages((current) =>
          current.filter((m) => m.id !== optimistic.id && m.id !== assistantId),
        );
        setInput(content);
        return false;
      } finally {
        setBusy(false);
      }
    },
    [canCreate, input, loadMessages],
  );

  const deleteSession = useCallback(
    async (sessionId: string) => {
      try {
        await aiChatApi.deleteSession(sessionId);
        // 如果删除的是当前激活的会话，自动切换到下一个
        if (sessionId === activeIdRef.current) {
          const remaining = sessions.filter((s) => s.id !== sessionId);
          await loadSessions(remaining[0]?.id ?? null);
        } else {
          setSessions((current) => current.filter((s) => s.id !== sessionId));
        }
        toast.success("toast.deleteSuccess");
      } catch (error) {
        toast.error(failureMessage(error));
      }
    },
    [sessions, loadSessions],
  );

  return {
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
  };
}
