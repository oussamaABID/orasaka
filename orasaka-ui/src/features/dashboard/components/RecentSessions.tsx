import * as React from "react";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { MessageSquare, Clock, ArrowRight } from "lucide-react";

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
 * Renders the recent chat sessions on the user dashboard.
 * Displays a list of active threads or an empty state prompt to start a new chat.
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
}: RecentSessionsProps) {
  return (
    <div className="md:col-span-3 space-y-4">
      <h3 className="text-lg font-bold text-zinc-800 dark:text-zinc-200">
        {t.dashboard.recentActivity}
      </h3>

      <Card className="p-4">
        {recentThreads.length === 0 ? (
          <div className="text-center py-12">
            <MessageSquare className="h-8 w-8 text-zinc-300 dark:text-zinc-700 mx-auto mb-3" />
            <p className="text-sm text-zinc-500 dark:text-zinc-400">
              {t.dashboard.noRecentSessions}
            </p>
            <Button onClick={onStartNewChat} className="mt-4" size="sm">
              {t.dashboard.startNewChat}
            </Button>
          </div>
        ) : (
          <div className="divide-y divide-zinc-100 dark:divide-zinc-800/50">
            {recentThreads.map((thread) => (
              <div
                key={thread.conversationId}
                onClick={() => onResumeSession(thread.conversationId)}
                className="flex items-center justify-between py-3.5 hover:bg-zinc-50/50 dark:hover:bg-zinc-900/20 px-2 rounded-xl cursor-pointer transition-all duration-150 group"
              >
                <div className="flex items-center min-w-0 mr-4">
                  <div className="w-8 h-8 rounded-xl bg-zinc-100 dark:bg-zinc-800 flex items-center justify-center mr-3 text-zinc-500 group-hover:text-zinc-900 dark:group-hover:text-zinc-350 transition-colors">
                    <MessageSquare className="h-4 w-4" />
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-zinc-800 dark:text-zinc-200 truncate max-w-[200px] sm:max-w-[320px]">
                      {thread.title}
                    </p>
                    <span className="text-xs text-zinc-400 dark:text-zinc-500 font-mono flex items-center mt-0.5">
                      <Clock className="h-3 w-3 mr-1" />
                      {formatDate(thread.updatedAt)}
                    </span>
                  </div>
                </div>

                <div className="flex items-center text-xs text-zinc-400 dark:text-zinc-500 font-medium group-hover:text-zinc-900 dark:group-hover:text-zinc-300 transition-colors">
                  <span className="mr-1.5 hidden sm:inline-block">
                    {t.dashboard.resumeSession}
                  </span>
                  <ArrowRight className="h-3.5 w-3.5 transform group-hover:translate-x-0.5 transition-transform" />
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}
