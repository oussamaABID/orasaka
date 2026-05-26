"use client";

import * as React from "react";
import { format } from "date-fns";
import { fr, enUS } from "date-fns/locale";
import { useRouter } from "next/navigation";
import { useAuth } from "@/core/hooks/useAuth";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { useTenant } from "@/core/context/TenantContext";
import { useTranslation } from "@/core/context/LocaleContext";
import { useChatStream } from "@/features/chat-session/hooks/useChatStream";
import { useToast } from "@/core/context/ToastContext";
import { Skeleton } from "@/components/ui/Skeleton";

import { InterceptorPipeline } from "@/features/dashboard/components/InterceptorPipeline";
import { MetricsGrid } from "@/features/dashboard/components/MetricsGrid";
import { SystemHealthHud } from "@/features/dashboard/components/SystemHealthHud";
import { QuickActions } from "@/features/dashboard/components/QuickActions";
import { RecentSessions } from "@/features/dashboard/components/RecentSessions";

/**
 * HomePage — Command Center Dashboard.
 * 3-section layout: Pipeline → Metrics+Health → Actions+Sessions.
 *
 * @returns The rendered React element for the dashboard view.
 */
export default function HomePage() {
  const router = useRouter();
  const { isAuthenticated, isLoading, user } = useAuth();
  const { accentClasses } = useTenant();
  const { locale, t } = useTranslation();
  const { addToast } = useToast();

  // Use useChatStream to fetch threads list and access createThread helper
  const { threads, createThread } = useChatStream("dashboard");

  React.useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isLoading, isAuthenticated, router]);

  const handleStartNewChat = async () => {
    try {
      const newThread = await createThread();
      router.push(`/chat?conversationId=${newThread.conversationId}`);
      addToast(t.dashboard.startNewChat, "success");
    } catch (e) {
      console.error("Failed to start new chat:", e);
      addToast(t.errors.generic, "error");
    }
  };

  const formatDate = (timestamp: number) => {
    try {
      return format(timestamp, "MMM d, HH:mm", {
        locale: locale === "fr" ? fr : enUS,
      });
    } catch {
      return "";
    }
  };

  // Time-of-day greeting emoji
  const getGreetingEmoji = (): string => {
    const hour = new Date().getHours();
    if (hour < 12) return "☀️";
    if (hour < 18) return "🌤️";
    return "🌙";
  };

  if (isLoading || !isAuthenticated) {
    return (
      <section className="flex min-h-screen items-center justify-center bg-[var(--surface-0)] ambient-grid">
        <div className="w-full max-w-6xl px-6 space-y-8">
          {/* Skeleton header */}
          <div className="space-y-2">
            <Skeleton variant="text" width="40%" height="2rem" />
            <Skeleton variant="text" width="60%" />
          </div>
          {/* Skeleton pipeline */}
          <Skeleton variant="rect" height="5rem" />
          {/* Skeleton grid */}
          <section className="grid gap-6 md:grid-cols-3">
            <Skeleton variant="rect" height="8rem" />
            <Skeleton variant="rect" height="8rem" />
            <Skeleton variant="rect" height="8rem" />
          </section>
        </div>
      </section>
    );
  }

  // Get most recent 3 threads to show in activity panel
  const recentThreads = [...(threads || [])]
    .sort((a, b) => b.updatedAt - a.updatedAt)
    .slice(0, 3);

  // Mock metrics — in production, fetched from /api/metrics
  const metrics = {
    activeSessions: threads?.length || 0,
    tokensUsed: 12450,
    memoryNodes: 847,
  };

  return (
    <section className="flex h-screen overflow-hidden bg-[var(--surface-0)] transition-colors duration-200">
      <Sidebar />

      <div className="flex flex-1 flex-col overflow-hidden">
        <Header />

        <main className="flex-1 overflow-auto p-6 scrollbar-thin ambient-grid">
          <div className="mx-auto max-w-6xl space-y-6 stagger-children">
            {/* ── Section 1: Welcome Header ──────────────────── */}
            <header className="flex items-baseline justify-between">
              <div className="space-y-1">
                <h2 className="fluid-2xl font-bold tracking-tight text-[var(--text-primary)]">
                  {getGreetingEmoji()}{" "}
                  {t.dashboard.welcome},{" "}
                  <span className="text-[var(--accent)]">
                    {user?.name || "Admin"}
                  </span>
                </h2>
                <p className="text-[var(--text-secondary)] fluid-sm">
                  {t.dashboard.overview}
                </p>
              </div>
              <kbd className="hidden lg:flex items-center gap-1 px-2 py-1 rounded-md bg-[var(--surface-2)] border border-[var(--border-subtle)] text-[10px] font-mono text-[var(--text-muted)]">
                ⌘K
              </kbd>
            </header>

            {/* ── Section 2: Interceptor Pipeline ────────────── */}
            <InterceptorPipeline />

            {/* ── Section 3: Metrics + System Health ─────────── */}
            <div className="grid gap-6 lg:grid-cols-4">
              <section className="lg:col-span-3">
                <MetricsGrid
                  metrics={metrics}
                  accentClasses={accentClasses}
                  t={t}
                />
              </section>
              <section className="lg:col-span-1">
                <SystemHealthHud />
              </section>
            </div>

            {/* ── Section 4: Quick Actions + Recent Sessions ── */}
            <div className="grid gap-6 md:grid-cols-3">
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
    </section>
  );
}
