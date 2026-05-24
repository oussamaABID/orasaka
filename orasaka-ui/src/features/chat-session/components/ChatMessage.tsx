import React from "react";
import ReactMarkdown from "react-markdown";
import { ChatMessage as ChatMessageType } from "../types/chat.types";
import { useTranslation } from "@/core/context/LocaleContext";

interface Props {
  message: ChatMessageType;
}

const ContentText: React.FC<{ content: string }> = ({ content }) => (
  <div className="max-w-none text-zinc-900 dark:text-zinc-100">
    <ReactMarkdown
      components={{
        h1: ({ children }) => (
          <h1 className="text-base font-semibold mt-3 mb-1 text-zinc-900 dark:text-zinc-50">
            {children}
          </h1>
        ),
        h2: ({ children }) => (
          <h2 className="text-sm font-semibold mt-3 mb-1 text-zinc-900 dark:text-zinc-50">
            {children}
          </h2>
        ),
        h3: ({ children }) => (
          <h3 className="text-xs font-semibold mt-2 mb-1 text-zinc-900 dark:text-zinc-50">
            {children}
          </h3>
        ),
        p: ({ children }) => (
          <p className="mb-2 last:mb-0 leading-relaxed break-words whitespace-pre-wrap">
            {children}
          </p>
        ),
        ul: ({ children }) => (
          <ul className="list-disc pl-5 mb-2 space-y-1">{children}</ul>
        ),
        ol: ({ children }) => (
          <ol className="list-decimal pl-5 mb-2 space-y-1">{children}</ol>
        ),
        li: ({ children }) => <li className="mb-0.5">{children}</li>,
        strong: ({ children }) => (
          <strong className="font-semibold text-zinc-950 dark:text-zinc-50">
            {children}
          </strong>
        ),
        em: ({ children }) => <em className="italic">{children}</em>,
        a: ({ href, children }) => (
          <a
            href={href}
            target="_blank"
            rel="noopener noreferrer"
            className="text-cyan-600 dark:text-cyan-400 hover:underline break-all"
          >
            {children}
          </a>
        ),
        pre: ({ children }) => (
          <pre className="overflow-x-auto rounded-lg bg-zinc-950 p-3 my-2 font-mono text-xs text-zinc-50 border border-zinc-800/60 shadow-inner">
            {children}
          </pre>
        ),
        code: ({ className, children }) => {
          const isInline = !className;
          return isInline ? (
            <code className="bg-zinc-200/60 dark:bg-zinc-800/80 rounded px-1 py-0.5 font-mono text-[11px] text-zinc-900 dark:text-zinc-100">
              {children}
            </code>
          ) : (
            <code className={className}>{children}</code>
          );
        },
        blockquote: ({ children }) => (
          <blockquote className="border-l-2 border-zinc-300 dark:border-zinc-700 pl-3 py-0.5 italic my-2 text-zinc-500 dark:text-zinc-400">
            {children}
          </blockquote>
        ),
      }}
    >
      {content}
    </ReactMarkdown>
  </div>
);

const ContentImage: React.FC<{ content: string }> = ({ content }) => (
  <div className="mt-1 rounded-lg overflow-hidden border border-zinc-200/50 dark:border-zinc-800/50 bg-zinc-100/30 dark:bg-zinc-900/30 max-w-sm shadow-sm transition-all duration-200 hover:scale-[1.01]">
    {/* eslint-disable-next-line @next/next/no-img-element */}
    <img
      src={content}
      alt="Generated Asset"
      className="w-full object-contain max-h-[300px]"
    />
  </div>
);

const ContentAudio: React.FC<{ content: string }> = ({ content }) => (
  <div className="mt-1 flex items-center justify-center p-2 rounded-lg border border-zinc-200/50 dark:border-zinc-800/50 bg-zinc-100/30 dark:bg-zinc-900/30 min-w-[240px] shadow-sm">
    <audio src={content} controls className="w-full focus:outline-none" />
  </div>
);

export const ChatMessage: React.FC<Props> = ({ message }) => {
  const isUser = message.role === "user";
  const { t } = useTranslation();

  const renderContent = () => {
    switch (message.kind) {
      case "image":
        return <ContentImage content={message.content} />;
      case "audio":
        return <ContentAudio content={message.content} />;
      case "text":
      default:
        return <ContentText content={message.content} />;
    }
  };

  return (
    <div
      className={`flex w-full mb-4 ${isUser ? "justify-end" : "justify-start"}`}
    >
      <div
        className={`flex max-w-[75%] gap-3 items-start ${isUser ? "flex-row-reverse" : "flex-row"}`}
      >
        <div
          className={`h-8 w-8 rounded-xl flex items-center justify-center text-xs font-semibold border select-none flex-shrink-0 transition-all duration-200 hover:scale-105 ${
            isUser
              ? "bg-zinc-100/80 dark:bg-zinc-900/65 border-zinc-200/80 dark:border-zinc-800/60 text-zinc-900 dark:text-zinc-100 shadow-sm"
              : "bg-zinc-50/80 dark:bg-zinc-900/40 border-zinc-200/80 dark:border-zinc-800/60 text-zinc-700 dark:text-zinc-300 shadow-sm"
          }`}
        >
          {isUser ? t.chat.user[0] : t.chat.ai}
        </div>

        <div
          className={`p-4 rounded-2xl border text-sm leading-relaxed text-zinc-900 dark:text-zinc-100 shadow-sm backdrop-blur-sm transition-all duration-200 hover:shadow-md ${
            isUser
              ? "bg-zinc-100/80 dark:bg-zinc-900/65 border-zinc-200/80 dark:border-zinc-800/60 rounded-tr-sm hover:bg-zinc-100/90 dark:hover:bg-zinc-900/75"
              : "bg-zinc-50/90 dark:bg-zinc-900/35 border-zinc-200/80 dark:border-zinc-800/50 rounded-tl-sm hover:bg-zinc-50/95 dark:hover:bg-zinc-900/45"
          }`}
        >
          {renderContent()}
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
