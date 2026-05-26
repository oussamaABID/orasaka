/* eslint-disable no-restricted-syntax */
import React from "react";
import ReactMarkdown from "react-markdown";
import { format } from "date-fns";
import { ChatMessage as ChatMessageType } from "@/features/chat-session/types/chat.types";
import { useTranslation } from "@/core/context/LocaleContext";

interface Props {
  message: ChatMessageType;
  index?: number;
}

const MarkdownH1 = ({ children }: { children?: React.ReactNode }) => (
  <h1 className="text-base font-semibold mt-3 mb-1 text-[var(--text-primary)]">
    {children}
  </h1>
);

const MarkdownH2 = ({ children }: { children?: React.ReactNode }) => (
  <h2 className="text-[13px] font-semibold mt-3 mb-1 text-[var(--text-primary)]">
    {children}
  </h2>
);

const MarkdownH3 = ({ children }: { children?: React.ReactNode }) => (
  <h3 className="text-[12px] font-semibold mt-2 mb-1 text-[var(--text-primary)]">
    {children}
  </h3>
);

const MarkdownP = ({ children }: { children?: React.ReactNode }) => (
  <p className="mb-2 last:mb-0 leading-relaxed break-words whitespace-pre-wrap">
    {children}
  </p>
);

const markdownComponents = {
  h1: MarkdownH1,
  h2: MarkdownH2,
  h3: MarkdownH3,
  p: MarkdownP,
  ul: ({ children }: { children?: React.ReactNode }) => (
    <ul className="list-disc pl-5 mb-2 space-y-1">{children}</ul>
  ),
  ol: ({ children }: { children?: React.ReactNode }) => (
    <ol className="list-decimal pl-5 mb-2 space-y-1">{children}</ol>
  ),
  li: ({ children }: { children?: React.ReactNode }) => <li className="mb-0.5">{children}</li>,
  strong: ({ children }: { children?: React.ReactNode }) => (
    <strong className="font-semibold text-[var(--text-primary)]">
      {children}
    </strong>
  ),
  em: ({ children }: { children?: React.ReactNode }) => <em className="italic">{children}</em>,
  a: ({ href, children }: { href?: string; children?: React.ReactNode }) => (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      className="text-[var(--accent)] hover:underline break-all"
    >
      {children}
    </a>
  ),
  pre: ({ children }: { children?: React.ReactNode }) => (
    <pre className="overflow-x-auto rounded-xl bg-[var(--surface-2)] p-3 my-2 font-mono text-[11px] text-[var(--text-primary)] border border-[var(--border-subtle)] shadow-inner">
      {children}
    </pre>
  ),
  code: ({ className, children }: { className?: string; children?: React.ReactNode }) => {
    const isInline = !className;
    return isInline ? (
      <code className="bg-[var(--surface-3)] rounded px-1 py-0.5 font-mono text-[11px] text-[var(--text-primary)]">
        {children}
      </code>
    ) : (
      <code className={className}>{children}</code>
    );
  },
  blockquote: ({ children }: { children?: React.ReactNode }) => (
    <blockquote className="border-l-2 border-[var(--border-default)] pl-3 py-0.5 italic my-2 text-[var(--text-secondary)]">
      {children}
    </blockquote>
  ),
};

const ContentText: React.FC<Readonly<{ content: string }>> = ({ content }) => (
  <div className="max-w-none text-[var(--text-primary)]">
    <ReactMarkdown components={markdownComponents}>
      {content}
    </ReactMarkdown>
  </div>
);

const ContentImage: React.FC<{ content: string }> = ({ content }) => (
  <div className="mt-1 rounded-xl overflow-hidden border border-[var(--border-subtle)] bg-[var(--surface-2)] max-w-sm shadow-sm transition-all duration-200 hover:scale-[1.01]">
    {/* eslint-disable-next-line @next/next/no-img-element */}
    <img
      src={content}
      alt="Generated Asset"
      className="w-full object-contain max-h-[300px]"
    />
  </div>
);

const ContentAudio: React.FC<{ content: string }> = ({ content }) => (
  <div className="mt-1 flex items-center justify-center p-2 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-2)] min-w-[240px] shadow-sm">
    <audio src={content} controls className="w-full focus:outline-none">
      <track kind="captions" />
    </audio>
  </div>
);

export const ChatMessage: React.FC<Props> = ({ message, index = 0 }) => {
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
    <article
      className={`flex w-full mb-4 ${isUser ? "justify-end" : "justify-start"} animate-in fade-in duration-300 ${isUser ? "slide-in-from-right-3" : "slide-in-from-left-3"}`}
      style={{
        animationDelay: `${Math.min(index * 50, 300)}ms`,
        animationFillMode: "backwards",
      }}
    >
      <div
        className={`flex max-w-[75%] gap-3 items-start ${isUser ? "flex-row-reverse" : "flex-row"}`}
      >
        <figure
          className={`h-9 w-9 rounded-2xl flex items-center justify-center text-[11px] font-bold border select-none flex-shrink-0 transition-colors duration-200 ${
            isUser
              ? "bg-[var(--accent-soft)] border-[var(--accent)]/20 text-[var(--accent)]"
              : "bg-[var(--surface-2)] border-[var(--border-subtle)] text-[var(--text-secondary)]"
          }`}
        >
          {isUser ? t.chat.user[0] : t.chat.ai}
        </figure>

        <section
          className={`p-4 rounded-2xl border text-[13px] leading-[1.7] text-[var(--text-primary)] transition-shadow duration-200 ${
            isUser
              ? "bg-[var(--accent-soft)] border-[var(--accent)]/10 rounded-tr-md"
              : "bg-[var(--surface-1)] border-[var(--border-subtle)] rounded-tl-md"
          }`}
        >
          {renderContent()}
          <span className="text-[10px] text-[var(--text-muted)] block mt-2 text-right select-none">
            {format(message.timestamp, "HH:mm")}
          </span>
        </section>
      </div>
    </article>
  );
};
