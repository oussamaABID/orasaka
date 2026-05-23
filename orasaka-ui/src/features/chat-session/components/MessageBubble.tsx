import React from "react";
import { ChatMessage } from "../types/chat.types";
import { useTranslation } from "@/core/context/LocaleContext";

interface Props {
  message: ChatMessage;
}

/**
 * MessageBubble component displaying a single message card with role-specific avatar.
 *
 * @param props Component properties containing the chat message record.
 * @returns The MessageBubble react node.
 */
export const MessageBubble: React.FC<Props> = ({ message }) => {
  const isUser = message.role === "user";
  const { t } = useTranslation();

  return (
    <div
      className={`flex w-full mb-4 ${isUser ? "justify-end" : "justify-start"}`}
    >
      <div
        className={`flex max-w-[75%] gap-3 items-start ${isUser ? "flex-row-reverse" : "flex-row"}`}
      >
        {/* Avatar Neutre */}
        <div
          className={`h-8 w-8 rounded-xl flex items-center justify-center text-xs font-semibold border select-none flex-shrink-0 transition-all duration-200 hover:scale-105 ${
            isUser
              ? "bg-zinc-100/80 dark:bg-zinc-900/65 border-zinc-200/80 dark:border-zinc-800/60 text-zinc-900 dark:text-zinc-100 shadow-sm"
              : "bg-zinc-50/80 dark:bg-zinc-900/40 border-zinc-200/80 dark:border-zinc-800/60 text-zinc-700 dark:text-zinc-300 shadow-sm"
          }`}
        >
          {isUser ? t.chat.user[0] : t.chat.ai}
        </div>

        {/* Bulle de texte */}
        <div
          className={`p-4 rounded-2xl border text-sm leading-relaxed text-zinc-900 dark:text-zinc-100 shadow-sm backdrop-blur-sm transition-all duration-200 hover:shadow-md ${
            isUser
              ? "bg-zinc-100/80 dark:bg-zinc-900/65 border-zinc-200/80 dark:border-zinc-800/60 rounded-tr-sm hover:bg-zinc-100/90 dark:hover:bg-zinc-900/75"
              : "bg-zinc-50/90 dark:bg-zinc-900/35 border-zinc-200/80 dark:border-zinc-800/50 rounded-tl-sm hover:bg-zinc-50/95 dark:hover:bg-zinc-900/45"
          }`}
        >
          <p className="whitespace-pre-wrap">{message.content}</p>
          <span className="text-[10px] text-zinc-400 dark:text-zinc-500 block mt-2 text-right">
            {new Date(message.timestamp).toLocaleTimeString([], {
              hour: "2-digit",
              minute: "2-digit",
            })}
          </span>
        </div>
      </div>
    </div>
  );
};
