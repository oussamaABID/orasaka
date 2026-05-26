"use client";

import { useState, useRef, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useJobStream } from "@/features/jobs/context/JobStreamContext";
import type { ChatMessage } from "@/features/chat-session/types/chat.types";
import type { BootstrapFeature } from "@/features/chat-session/components/ContextPlusMenu";
import { executeFeatureWithPrompt } from "@/features/chat-session/utils/executeFeature";
import { MediaApi } from "@/services/media.api";
import type { MediaKind } from "@/core/constants/capability.constants";

interface UseChatActionsProps {
  activeConversationId: string;
}

/**
 * Custom hook encapsulating chat action handlers: thread management,
 * file uploads, message sending, and feature node execution.
 * Extracts imperative logic from ChatWindow into a composable unit.
 */
export function useChatActions({ activeConversationId }: UseChatActionsProps) {
  const router = useRouter();
  const { user } = useAuth();
  const { setActiveConversationId, chatInput, setChatInput } = useJobStream();
  const queryClient = useQueryClient();

  const userId = user?.id || user?.email || "anonymous";

  const [selectedFeature, setSelectedFeature] =
    useState<BootstrapFeature | null>(null);
  const [attachment, setAttachment] = useState<{
    assetId: string;
    name: string;
  } | null>(null);
  const [isUploadingAttachment, setIsUploadingAttachment] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const addMessageToCache = useCallback(
    (content: string, kind: MediaKind) => {
      const cacheKey = ["chatMessages", userId, activeConversationId];
      queryClient.setQueryData<ChatMessage[]>(cacheKey, (old = []) => {
        const updated = [
          ...old,
          {
            id: `assistant-${Date.now()}`,
            role: "assistant" as const,
            content,
            timestamp: Date.now(),
            kind,
          },
        ];
        localStorage.setItem(
          `orasaka_messages_${userId}_${activeConversationId}`,
          JSON.stringify(updated),
        );
        return updated;
      });
    },
    [queryClient, userId, activeConversationId],
  );

  const nodeMutation = useMutation({
    mutationFn: executeFeatureWithPrompt,
    onSuccess: (res) => addMessageToCache(res.content, res.kind),
  });

  const handleSelectThread = useCallback(
    (id: string) => {
      setActiveConversationId(id);
      router.push(`/chat?conversationId=${id}`);
    },
    [setActiveConversationId, router],
  );

  const handleFileChange = useCallback(
    async (
      e: React.ChangeEvent<HTMLInputElement>,
      t: { errors: { fileUploadFailed: string } },
    ) => {
      const file = e.target.files?.[0];
      if (!file) return;
      setIsUploadingAttachment(true);
      try {
        const res = await MediaApi.uploadMedia(file);
        setAttachment({ assetId: res.assetId, name: file.name });
      } catch (err) {
        console.error("Failed to upload attachment:", err);
        alert(t.errors.fileUploadFailed);
      } finally {
        setIsUploadingAttachment(false);
        if (fileInputRef.current) fileInputRef.current.value = "";
      }
    },
    [],
  );

  const handleSend = useCallback(
    (
      e: React.SubmitEvent<HTMLFormElement>,
      sendMessage: (msg: string) => void,
      isSending: boolean,
      isGenerating: boolean,
    ) => {
      e.preventDefault();
      const trimmed = chatInput.trim();
      if (
        (!trimmed && !attachment && !selectedFeature) ||
        isSending ||
        isGenerating ||
        isUploadingAttachment
      )
        return;

      if (selectedFeature) {
        nodeMutation.mutate({
          feature: selectedFeature,
          prompt: trimmed || `Run ${selectedFeature.label}`,
          assetId: attachment?.assetId,
        });
        setSelectedFeature(null);
      } else {
        let finalPrompt = trimmed;
        if (attachment) {
          if (!finalPrompt) {
            finalPrompt = `Analyze attachment: ${attachment.name}`;
          }
          finalPrompt = `${finalPrompt} (assetId: ${attachment.assetId})`;
        }
        sendMessage(finalPrompt);
      }
      setChatInput("");
      setAttachment(null);
    },
    [
      chatInput,
      attachment,
      selectedFeature,
      isUploadingAttachment,
      nodeMutation,
      setChatInput,
    ],
  );

  const handleExecuteNode = useCallback(
    (
      feature: BootstrapFeature,
      isPlusMenuOpen: boolean,
      setIsPlusMenuOpen: (v: boolean) => void,
    ) => {
      if (!chatInput.trim()) {
        setSelectedFeature(feature);
        setIsPlusMenuOpen(false);
        return;
      }
      nodeMutation.mutate({
        feature,
        prompt: chatInput.trim(),
        assetId: attachment?.assetId,
      });
      setChatInput("");
      setAttachment(null);
      setIsPlusMenuOpen(false);
    },
    [chatInput, attachment, nodeMutation, setChatInput],
  );

  return {
    userId,
    selectedFeature,
    setSelectedFeature,
    attachment,
    setAttachment,
    isUploadingAttachment,
    fileInputRef,
    nodeMutation,
    handleSelectThread,
    handleFileChange,
    handleSend,
    handleExecuteNode,
  };
}
