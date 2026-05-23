"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { useTenant } from "@/features/tenant/context/TenantContext";
import { useTranslation } from "@/core/context/LocaleContext";
import { useChatStream } from "@/features/chat-session/hooks/useChatStream";
import {
  ChatThread,
  ChatMessage,
} from "@/features/chat-session/types/chat.types";
import { MetricsGrid } from "@/features/dashboard/components/MetricsGrid";
import { QuickActions } from "@/features/dashboard/components/QuickActions";
import { RecentSessions } from "@/features/dashboard/components/RecentSessions";

/**
 * HomePage component rendering the dynamic user dashboard.
 * Uses extracted sub-components to stay modular and readable.
 *
 * @returns The rendered React element for the dashboard view.
 */
export default function HomePage() {
  const router = useRouter();
  const { isAuthenticated, isLoading, user } = useAuth();
  const { accentClasses } = useTenant();
  const { locale, t } = useTranslation();

  // Use useChatStream to fetch threads list and access createThread helper
  const { threads, createThread } = useChatStream("dashboard");

  const [metrics, setMetrics] = React.useState({
    activeSessions: 0,
    tokensUsed: 0,
    memoryNodes: 0,
  });

  React.useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isLoading, isAuthenticated, router]);

  // Aggregate metrics from localStorage threads and messages
  React.useEffect(() => {
    if (typeof window === "undefined" || !isAuthenticated) return;

    const threadsCount = threads?.length || 0;
    let totalMessagesCount = 0;
    let totalCharacters = 0;

    if (threads && Array.isArray(threads)) {
      threads.forEach((t: ChatThread) => {
        const storedMessagesStr = localStorage.getItem(
          `orasaka_messages_${t.conversationId}`,
        );
        if (storedMessagesStr) {
          try {
            const messages = JSON.parse(storedMessagesStr);
            if (Array.isArray(messages)) {
              totalMessagesCount += messages.length;
              messages.forEach((msg: ChatMessage) => {
                if (msg.content) {
                  totalCharacters += msg.content.length;
                }
              });
            }
          } catch (err) {
            console.error(
              "Failed to parse messages for thread",
              t.conversationId,
              err,
            );
          }
        }
      });
    }

    const tokensEstimate = Math.round(totalCharacters / 4);

    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMetrics({
      activeSessions: threadsCount,
      tokensUsed: tokensEstimate,
      memoryNodes: totalMessagesCount,
    });
  }, [isAuthenticated, threads]);

  const handleStartNewChat = () => {
    const newThread = createThread();
    router.push(`/chat?conversationId=${newThread.conversationId}`);
  };

  const formatDate = (timestamp: number) => {
    try {
      return new Intl.DateTimeFormat(locale === "fr" ? "fr-FR" : "en-US", {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      }).format(new Date(timestamp));
    } catch {
      return "";
    }
  };

  if (isLoading || !isAuthenticated) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-zinc-50 dark:bg-zinc-950">
        <span className="text-zinc-400 dark:text-zinc-500 text-sm animate-pulse">
          {t.dashboard.loading}
        </span>
      </div>
    );
  }

  // Get most recent 3 threads to show in activity panel
  const recentThreads = [...(threads || [])]
    .sort((a, b) => b.updatedAt - a.updatedAt)
    .slice(0, 3);

  return (
    <div className="flex h-screen overflow-hidden bg-zinc-50 dark:bg-zinc-950 transition-colors duration-200">
      <Sidebar />

      <div className="flex flex-1 flex-col overflow-hidden">
        <Header />

        <main className="flex-1 overflow-auto p-6 scrollbar-thin">
          <div className="mx-auto max-w-5xl space-y-8 animate-in fade-in slide-in-from-bottom-3 duration-300">
            {/* Page Header */}
            <div className="space-y-1">
              <h2 className="text-3xl font-extrabold tracking-tight text-zinc-900 dark:text-zinc-50">
                {t.dashboard.welcome}, {user?.name || "Admin"}
              </h2>
              <p className="text-zinc-500 dark:text-zinc-400 text-sm">
                {t.dashboard.overview}
              </p>
            </div>

            {/* Metrics Grid */}
            <MetricsGrid
              metrics={metrics}
              accentClasses={accentClasses}
              t={t}
            />

            {/* Split layout: Quick Actions & Recent Sessions */}
            <div className="grid gap-8 md:grid-cols-5">
              <QuickActions
                onStartNewChat={handleStartNewChat}
                onResumeProfile={() => router.push("/profile")}
                onConfigureSettings={() => router.push("/settings")}
                accentClasses={accentClasses}
                t={t}
              />
              <RecentSessions
                recentThreads={recentThreads}
                onStartNewChat={handleStartNewChat}
                onResumeSession={(id) =>
                  router.push(`/chat?conversationId=${id}`)
                }
                formatDate={formatDate}
                t={t}
              />
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
