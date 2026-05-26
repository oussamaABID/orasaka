"use client";

import React from "react";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { useAuth } from "@/core/hooks/useAuth";
import { useRouter } from "next/navigation";
import { Cpu } from "lucide-react";
import { useTranslation } from "@/core/context/LocaleContext";
import { VideoAnalysisCard } from "@/features/playground/components/VideoAnalysisCard";

export default function VideoAnalyzePage() {
  const router = useRouter();
  const { isAuthenticated, isLoading } = useAuth();
  const { t } = useTranslation();

  React.useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isLoading, isAuthenticated, router]);

  if (isLoading || !isAuthenticated) return null;

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
                {t.sidebar.analyzeVideo}
              </h1>
              <p className="text-[var(--text-secondary)] text-sm">
                {t.playgroundHeader.subtitle}
              </p>
            </header>

            <VideoAnalysisCard />
          </div>
        </main>
      </div>
    </section>
  );
}
