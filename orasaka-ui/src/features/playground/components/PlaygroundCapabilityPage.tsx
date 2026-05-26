"use client";

import React from "react";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useRouter } from "next/navigation";
import { Cpu, Loader2 } from "lucide-react";
import { useTranslation } from "@/core/context/LocaleContext";
import { useOperationGraph } from "@/features/playground/hooks/useOperationGraph";
import { PlaygroundNodeCard } from "@/features/playground/components/PlaygroundNodeCard";

interface PlaygroundCapabilityPageProps {
  /** Operation graph node ID (e.g. "orasaka.core.chat.text") */
  nodeId: string;
  /** Translation key accessor returning the page title */
  titleAccessor: (t: ReturnType<typeof useTranslation>["t"]) => string;
}

/**
 * Shared layout for all playground capability pages.
 * Eliminates duplication across text/chat, image/generate, video/generate, etc.
 */
export function PlaygroundCapabilityPage({
  nodeId,
  titleAccessor,
}: Readonly<PlaygroundCapabilityPageProps>) {
  const router = useRouter();
  const { isAuthenticated, isLoading } = useAuth();
  const { t } = useTranslation();

  React.useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isLoading, isAuthenticated, router]);

  const {
    nodes,
    isLoading: isLoadingGraph,
    invalidate,
  } = useOperationGraph(isAuthenticated);

  if (isLoading || !isAuthenticated) return null;

  const node = nodes.find((n) => n.id === nodeId);
  const title = titleAccessor(t);

  return (
    <section className="flex h-screen w-screen overflow-hidden bg-[var(--surface-0)]">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden min-w-0">
        <Header />
        <main className="flex-1 overflow-y-auto p-6 text-[var(--text-primary)] ambient-grid">
          <div className="max-w-3xl mx-auto space-y-8">
            <header className="flex flex-col gap-1">
              <h1 className="text-2xl font-bold tracking-tight flex items-center gap-2.5">
                <Cpu className="h-6 w-6 text-[var(--accent)]" />
                {title}
              </h1>
              <p className="text-[var(--text-secondary)] text-sm">
                {t.playgroundHeader.subtitle}
              </p>
            </header>

            {(() => {
              if (isLoadingGraph) {
                return (
                  <div className="flex items-center justify-center p-12">
                    <Loader2 className="h-8 w-8 animate-spin text-[var(--accent)]" />
                  </div>
                );
              }
              if (node) {
                return (
                  <PlaygroundNodeCard node={node} onExecuted={invalidate} />
                );
              }
              return (
                <article className="flex flex-col items-center justify-center p-16 border border-dashed border-[var(--border-default)] rounded-xl bg-[var(--surface-1)] space-y-4">
                  <div className="p-4 rounded-full bg-[var(--surface-2)] border border-[var(--border-subtle)] text-[var(--text-muted)]">
                    <Cpu className="h-8 w-8" />
                  </div>
                  <div className="text-center space-y-2 max-w-md">
                    <p className="font-semibold text-[var(--text-primary)] text-base">
                      {title}
                    </p>
                    <p className="text-sm text-[var(--text-secondary)] leading-relaxed">
                      {t.playgroundHeader.noActiveCapabilitiesDesc}
                    </p>
                  </div>
                </article>
              );
            })()}
          </div>
        </main>
      </div>
    </section>
  );
}
