import React from "react";
import { format } from "date-fns";
import { ChatMessage } from "@/features/chat-session/types/chat.types";

interface Props {
  message: ChatMessage;
}

export const MessageBubble: React.FC<Props> = ({ message }) => {
  const isUser = message.role === "user";

  return (
    <article
      className={`flex w-full mb-4 ${isUser ? "justify-end" : "justify-start"}`}
    >
      <div
        className={`flex max-w-[75%] gap-3 items-start ${isUser ? "flex-row-reverse" : "flex-row"}`}
      >
        {/* Avatar Neutre */}
        <div
          className={`h-8 w-8 rounded-md flex items-center justify-center text-xs font-medium border select-none flex-shrink-0 ${
            isUser
              ? "bg-zinc-100 dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800 text-zinc-900 dark:text-zinc-100"
              : "bg-zinc-50 dark:bg-zinc-950 border-zinc-200 dark:border-zinc-900 text-zinc-700 dark:text-zinc-300"
          }`}
        >
          {isUser ? "U" : "AI"}
        </div>

        {/* Bulle de texte */}
        <div
          className={`p-4 rounded-xl border text-sm leading-relaxed text-zinc-900 dark:text-zinc-100 ${
            isUser
              ? "bg-zinc-100 dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800 rounded-tr-none"
              : "bg-zinc-50 dark:bg-zinc-950 border-zinc-200 dark:border-zinc-900 rounded-tl-none"
          }`}
        >
          <p className="whitespace-pre-wrap">{message.content}</p>
          <span className="text-[10px] text-zinc-400 dark:text-zinc-500 block mt-2 text-right">
            {format(new Date(message.timestamp), "HH:mm")}
          </span>
        </div>
      </div>
    </article>
  );
};
