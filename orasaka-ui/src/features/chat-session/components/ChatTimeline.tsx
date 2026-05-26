import React from "react";
import { ChatMessage as ChatMessageType } from "@/features/chat-session/types/chat.types";
import { ChatMessage } from "./ChatMessage";
import { useTranslation } from "@/core/context/LocaleContext";

interface Props {
  messages: ChatMessageType[];
  isLoadingMessages: boolean;
  isGenerating: boolean;
  isImagePending: boolean;
  isSpeechPending: boolean;
  error: Error | null;
  messagesEndRef: React.RefObject<HTMLDivElement | null>;
}

export const ChatTimeline: React.FC<Props> = ({
  messages,
  isLoadingMessages,
  isGenerating,
  isImagePending,
  isSpeechPending,
  error,
  messagesEndRef,
}) => {
  const { t } = useTranslation();

  if (isLoadingMessages) {
    return (
      <div className="h-full flex items-center justify-center">
        <span className="text-[var(--text-muted)] text-[13px] animate-pulse">
          {t.chat.loadingMessages}
        </span>
      </div>
    );
  }

  if (!messages || messages.length === 0) {
    return (
      <div className="h-full flex items-center justify-center text-[var(--text-muted)] text-[13px]">
        {t.chat.noActiveConversation}
      </div>
    );
  }

  return (
    <>
      {messages.map((msg, idx) => (
        <ChatMessage key={msg.id} message={msg} index={idx} />
      ))}
      {(isGenerating || isImagePending || isSpeechPending) && (
        <div className="flex items-start gap-3 animate-in fade-in slide-in-from-left-3 duration-300">
          <figure className="w-9 h-9 rounded-2xl bg-[var(--surface-2)] border border-[var(--border-subtle)] flex items-center justify-center text-[11px] font-bold text-[var(--text-secondary)]">
            {t.chat.ai}
          </figure>
          <section className="bg-[var(--surface-1)] border border-[var(--border-subtle)] rounded-2xl rounded-tl-md px-5 py-3 flex items-center gap-2 h-11">
            <span
              className="w-2 h-2 bg-[var(--accent)] rounded-full animate-bounce"
              style={{ animationDelay: "0ms" }}
            />
            <span
              className="w-2 h-2 bg-[var(--accent)] rounded-full animate-bounce"
              style={{ animationDelay: "150ms" }}
            />
            <span
              className="w-2 h-2 bg-[var(--accent)] rounded-full animate-bounce"
              style={{ animationDelay: "300ms" }}
            />
          </section>
        </div>
      )}
      <div ref={messagesEndRef} />
      {error && (
        <div className="p-4 bg-red-500/5 text-red-600 dark:text-red-400 rounded-xl text-[13px] border border-red-500/20">
          {t.chat.connectionError}
        </div>
      )}
    </>
  );
};
