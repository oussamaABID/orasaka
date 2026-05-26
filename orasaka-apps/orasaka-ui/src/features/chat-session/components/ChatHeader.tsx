import React, { useState, useEffect } from "react";

interface ChatHeaderProps {
  activeConversationId: string;
  threadTitle: string;
  onOpenDrawer: () => void;
  onRename: (title: string) => void;
  t: {
    chat: {
      sessionTitle: string;
      id: string;
    };
  };
}

/**
 * Chat session header — displays thread title (inline-editable) and conversation ID.
 * Calm Obsidian 2026 — solid surface, no backdrop-blur.
 */
export function ChatHeader({
  activeConversationId,
  threadTitle,
  onOpenDrawer,
  onRename,
  t,
}: Readonly<ChatHeaderProps>) {
  const [isEditing, setIsEditing] = useState(false);
  const [editTitle, setEditTitle] = useState(threadTitle);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setEditTitle(threadTitle);
  }, [threadTitle]);

  const handleSave = () => {
    setIsEditing(false);
    if (editTitle.trim() && editTitle !== threadTitle) {
      onRename(editTitle.trim());
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      handleSave();
    } else if (e.key === "Escape") {
      setEditTitle(threadTitle);
      setIsEditing(false);
    }
  };

  return (
    <header className="p-4 border-b border-[var(--border-subtle)] bg-[var(--surface-1)] flex items-center justify-between">
      <div className="flex items-center gap-3 w-full">
        <button
          type="button"
          onClick={onOpenDrawer}
          className="p-2 rounded-lg md:hidden border border-[var(--border-default)] transition-colors"
        >
          <svg
            className="w-5 h-5"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5"
            />
          </svg>
        </button>
        <section className="flex flex-col flex-1 min-w-0">
          <section className="flex items-center gap-2">
            {isEditing ? (
              <input
                type="text"
                id="rename-session-input"
                value={editTitle}
                onChange={(e) => setEditTitle(e.target.value)}
                onBlur={handleSave}
                onKeyDown={handleKeyDown}
                autoFocus
                className="text-sm font-semibold text-[var(--text-primary)] bg-[var(--surface-2)] border border-[var(--border-default)] rounded px-2 py-0.5 focus:outline-none focus:ring-1 focus:ring-[var(--accent)] max-w-md w-full"
              />
            ) : (
              <>
                <h1 className="text-sm font-semibold text-[var(--text-primary)] truncate">
                  {threadTitle || t.chat.sessionTitle}
                </h1>
                <button
                  type="button"
                  id="btn-edit-session-title"
                  onClick={() => setIsEditing(true)}
                  className="p-1 text-[var(--text-muted)] hover:text-[var(--text-primary)] transition-colors"
                  title="Rename Session"
                >
                  <svg
                    className="w-4 h-4"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    strokeWidth={2}
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L6.832 19.82a4.5 4.5 0 01-1.897 1.13l-2.685.8.8-2.685a4.5 4.5 0 011.13-1.897L16.863 4.487zm0 0L19.5 7.125"
                    />
                  </svg>
                </button>
              </>
            )}
          </section>
          <span className="text-xs text-[var(--text-muted)] mt-0.5 font-mono">
            {t.chat.id}: {activeConversationId}
          </span>
        </section>
      </div>
    </header>
  );
}
