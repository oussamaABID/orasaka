import * as React from "react";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/icon";

interface Thread {
  conversationId: string;
  title: string;
  updatedAt: number;
}

interface RecentSessionsProps {
  recentThreads: Thread[];
  onStartNewChat: () => void;
  onResumeSession: (conversationId: string) => void;
  formatDate: (timestamp: number) => string;
  t: {
    dashboard: {
      recentActivity: string;
      noRecentSessions: string;
      startNewChat: string;
      resumeSession: string;
    };
  };
}

/**
 * Renders the recent chat sessions on the user dashboard — Calm Obsidian 2028.
 * Clean card list with design-token colors, hover-lift interaction, and
 * skeleton-ready empty state.
 *
 * @param props - The component properties.
 * @param props.recentThreads - List of thread metadata representing recent chat history.
 * @param props.onStartNewChat - Callback triggered to initiate a new conversation.
 * @param props.onResumeSession - Callback triggered to load an existing conversation thread.
 * @param props.formatDate - Utility function to format timestamp values into human-readable strings.
 * @param props.t - The localized translations dictionary.
 * @returns React element representing the recent sessions list or empty state.
 */
export function RecentSessions({
  recentThreads,
  onStartNewChat,
  onResumeSession,
  formatDate,
  t,
}: Readonly<RecentSessionsProps>) {
  return (
    <div className="md:col-span-3 space-y-4">
      <h3 className="hud-label px-1">{t.dashboard.recentActivity}</h3>

      <Card className="p-[var(--space-card)] overflow-hidden">
        {recentThreads.length === 0 ? (
          <div className="text-center py-12">
            <div className="mx-auto mb-4 w-12 h-12 rounded-[var(--radius-lg)] bg-[var(--surface-2)] flex items-center justify-center">
              <Icon name="chat" size={20} className="text-[var(--text-muted)]" />
            </div>
            <p className="fluid-sm text-[var(--text-secondary)] mb-4">
              {t.dashboard.noRecentSessions}
            </p>
            <Button onClick={onStartNewChat} size="sm">
              {t.dashboard.startNewChat}
            </Button>
          </div>
        ) : (
          <ul className="divide-y divide-[var(--border-subtle)] -mx-1">
            {recentThreads.map((thread) => (
              <li key={thread.conversationId}>
                <button
                  type="button"
                  onClick={() => onResumeSession(thread.conversationId)}
                  className="flex w-full items-center justify-between py-3.5 px-3 rounded-[var(--radius-md)] cursor-pointer transition-all duration-200 hover:bg-[var(--surface-2)] hover:shadow-[var(--shadow-xs)] group text-left"
                >
                <div className="flex items-center min-w-0 mr-4">
                  <div className="w-9 h-9 rounded-[var(--radius-md)] bg-[var(--surface-2)] flex items-center justify-center mr-3.5 text-[var(--text-muted)] group-hover:text-[var(--accent)] transition-colors duration-200 border border-[var(--border-subtle)]">
                    <Icon name="chat" size={16} />
                  </div>
                  <div className="min-w-0">
                    <p className="fluid-sm font-semibold text-[var(--text-primary)] truncate max-w-[200px] sm:max-w-[320px]">
                      {thread.title}
                    </p>
                    <span className="fluid-xs text-[var(--text-muted)] font-mono tracking-wider flex items-center mt-0.5">
                      <Icon name="history" size={12} className="mr-1" />
                      {formatDate(thread.updatedAt)}
                    </span>
                  </div>
                </div>

                <div className="flex items-center fluid-xs text-[var(--text-muted)] font-medium group-hover:text-[var(--accent)] transition-colors duration-200">
                  <span className="mr-1.5 hidden sm:inline-block font-semibold tracking-wide uppercase">
                    {t.dashboard.resumeSession}
                  </span>
                  <Icon name="arrowRight" size={14} className="transform group-hover:translate-x-0.5 transition-transform duration-200" />
                </div>
                </button>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}
