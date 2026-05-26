"use client";

import React from "react";
import Link from "next/link";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { useAuth } from "@/core/hooks/useAuth";
import { useRouter } from "next/navigation";
import { useTranslation } from "@/core/context/LocaleContext";
import { useOperationGraph } from "@/features/playground/hooks/useOperationGraph";
import { OperationGraphCard } from "@/features/playground/components/OperationGraphCard";
import {
  Cpu,
  Loader2,
  Video,
  Image,
  Mic,
  Eye,
  Code2,
  MessageSquare,
  Sparkles,
  Search,
  Wrench,
  ArrowRight,
} from "lucide-react";

/** Category definition for the Feature Hub layout. */
interface FeatureCategory {
  id: string;
  label: string;
  description: string;
  icon: React.ElementType;
  accentColor: string;
  features: FeatureTile[];
}

interface FeatureTile {
  label: string;
  description: string;
  href: string;
  icon: React.ElementType;
}

export default function PlaygroundPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading } = useAuth();
  const { t } = useTranslation();

  React.useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isLoading, isAuthenticated, router]);

  const { isLoading: isLoadingGraph } = useOperationGraph(isAuthenticated);

  const categories: FeatureCategory[] = [
    {
      id: "creative",
      label: t.playgroundHeader.creativeCategory || "Creative Studio",
      description:
        t.playgroundHeader.creativeDesc ||
        "Generate videos, images, and speech from text prompts using AI models.",
      icon: Sparkles,
      accentColor: "text-amber-500",
      features: [
        {
          label: t.sidebar.generateVideo || "Generate Video",
          description:
            t.playgroundHeader.generateVideoDesc ||
            "Create AI-powered video clips from text descriptions.",
          href: "/playground/video/generate",
          icon: Video,
        },
        {
          label: t.sidebar.generateImage || "Generate Image",
          description:
            t.playgroundHeader.generateImageDesc ||
            "Produce photorealistic or stylized images from prompts.",
          href: "/playground/image/generate",
          icon: Image,
        },
        {
          label: t.sidebar.speechSynthesis || "Text-to-Speech",
          description:
            t.playgroundHeader.speechDesc ||
            "Convert text into natural-sounding speech audio.",
          href: "/playground/speech/synthesis",
          icon: Mic,
        },
      ],
    },
    {
      id: "analysis",
      label: t.playgroundHeader.analysisCategory || "Analysis Lab",
      description:
        t.playgroundHeader.analysisDesc ||
        "Analyze media content — video, images, and audio — with AI-powered insights.",
      icon: Search,
      accentColor: "text-emerald-500",
      features: [
        {
          label: t.sidebar.analyzeVideo || "Analyze Video",
          description:
            t.playgroundHeader.analyzeVideoDesc ||
            "Extract insights, transcripts, and key frames from video.",
          href: "/playground/video/analyze",
          icon: Video,
        },
        {
          label: t.sidebar.visionAnalysis || "Vision Analysis",
          description:
            t.playgroundHeader.visionDesc ||
            "Analyze images for objects, text, and scene understanding.",
          href: "/playground/vision/analyze",
          icon: Eye,
        },
        {
          label: t.sidebar.analyzeAudio || "Analyze Audio",
          description:
            t.playgroundHeader.audioDesc ||
            "Transcribe and analyze audio content with AI.",
          href: "/playground/audio/analyze",
          icon: Mic,
        },
      ],
    },
    {
      id: "developer",
      label: t.playgroundHeader.developerCategory || "Developer Tools",
      description:
        t.playgroundHeader.developerDesc ||
        "Code scaffolding, conversational AI, and knowledge retrieval tools.",
      icon: Wrench,
      accentColor: "text-blue-500",
      features: [
        {
          label: t.sidebar.featureToCode || "Code Scaffold",
          description:
            t.playgroundHeader.codeDesc ||
            "Generate boilerplate code from feature descriptions.",
          href: "/playground/code/scaffold",
          icon: Code2,
        },
        {
          label: t.sidebar.textChat || "Text Chat",
          description:
            t.playgroundHeader.textChatDesc ||
            "Chat with AI models in a dedicated text interface.",
          href: "/playground/text/chat",
          icon: MessageSquare,
        },
      ],
    },
  ];

  if (isLoading || !isAuthenticated) return null;

  return (
    <section className="flex h-screen w-screen overflow-hidden bg-[var(--surface-0)] transition-colors duration-200">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden min-w-0">
        <Header />
        <main className="flex-1 overflow-y-auto p-6 text-[var(--text-primary)] scrollbar-thin ambient-grid">
          <div className="max-w-6xl mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-3 duration-300">
            {/* Page header */}
            <header className="flex flex-col gap-1">
              <h1 className="fluid-2xl font-bold tracking-tight flex items-center gap-2.5">
                <Cpu className="h-6 w-6 text-[var(--accent)]" />
                {t.playgroundHeader.title}
              </h1>
              <p className="text-[var(--text-secondary)] text-sm max-w-2xl">
                {t.playgroundHeader.subtitle}
              </p>
            </header>

            {/* Operation Graph (always on top) */}
            {isLoadingGraph ? (
              <div className="flex items-center justify-center p-12">
                <Loader2 className="h-8 w-8 animate-spin text-amber-500" />
              </div>
            ) : (
              <OperationGraphCard />
            )}

            {/* Feature Hub — categorized sections */}
            <div className="space-y-10 stagger-children">
              {categories.map((cat) => (
                <section key={cat.id} className="space-y-4">
                  {/* Category header */}
                  <div className="flex items-center gap-3">
                    <div
                      className={`p-2 rounded-[var(--radius-md)] bg-[var(--surface-2)] ${cat.accentColor}`}
                    >
                      <cat.icon className="h-5 w-5" />
                    </div>
                    <div>
                      <h2 className="text-lg font-semibold tracking-tight text-[var(--text-primary)]">
                        {cat.label}
                      </h2>
                      <p className="text-xs text-[var(--text-secondary)]">
                        {cat.description}
                      </p>
                    </div>
                  </div>

                  {/* Feature tiles grid */}
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {cat.features.map((feature) => (
                      <Link
                        key={feature.href}
                        href={feature.href}
                        className="glass-card group relative flex flex-col gap-3 p-[var(--space-card)] rounded-[var(--radius-lg)] border border-[var(--border-subtle)] hover:border-[var(--accent)] hover:shadow-[var(--shadow-glow)] hover-lift transition-[border-color,box-shadow,transform] duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent)] no-underline"
                      >
                        <div className="flex items-center justify-between">
                          <div
                            className={`p-2 rounded-[var(--radius-md)] bg-[var(--accent-soft)] ${cat.accentColor}`}
                          >
                            <feature.icon className="h-4 w-4" />
                          </div>
                          <ArrowRight className="h-4 w-4 text-[var(--text-muted)] group-hover:text-[var(--accent)] group-hover:translate-x-0.5 transition-all duration-200" />
                        </div>
                        <div className="space-y-1">
                          <h3 className="text-sm font-semibold text-[var(--text-primary)]">
                            {feature.label}
                          </h3>
                          <p className="text-xs text-[var(--text-secondary)] leading-relaxed">
                            {feature.description}
                          </p>
                        </div>
                      </Link>
                    ))}
                  </div>
                </section>
              ))}
            </div>
          </div>
        </main>
      </div>
    </section>
  );
}
