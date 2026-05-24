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
        <span className="text-zinc-400 text-sm animate-pulse">
          {t.chat.loadingMessages}
        </span>
      </div>
    );
  }

  if (!messages || messages.length === 0) {
    return (
      <div className="h-full flex items-center justify-center text-zinc-400 text-sm">
        {t.chat.noActiveConversation}
      </div>
    );
  }

  return (
    <>
      {messages.map((msg) => (
        <ChatMessage key={msg.id} message={msg} />
      ))}
      {(isGenerating || isImagePending || isSpeechPending) && (
        <div className="flex items-start gap-3">
          <div className="w-8 h-8 rounded-xl bg-zinc-100 dark:bg-zinc-900 border flex items-center justify-center text-xs font-semibold text-zinc-500">
            {t.chat.ai}
          </div>
          <div className="bg-zinc-100 dark:bg-zinc-900 border rounded-2xl px-4 py-3 flex items-center gap-1.5 h-10">
            <span
              className="w-2 h-2 bg-zinc-400 rounded-full animate-bounce"
              style={{ animationDelay: "0ms" }}
            />
            <span
              className="w-2 h-2 bg-zinc-400 rounded-full animate-bounce"
              style={{ animationDelay: "150ms" }}
            />
            <span
              className="w-2 h-2 bg-zinc-400 rounded-full animate-bounce"
              style={{ animationDelay: "300ms" }}
            />
          </div>
        </div>
      )}
      <div ref={messagesEndRef} />
      {error && (
        <div className="p-4 bg-red-50 text-red-600 rounded-xl text-sm">
          {t.chat.connectionError}
        </div>
      )}
    </>
  );
};
