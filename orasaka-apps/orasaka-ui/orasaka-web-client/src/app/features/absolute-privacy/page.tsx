/* eslint-disable */
"use client";

import * as React from "react";
import Link from "next/link";
import { useTranslation } from "@/core/context/LocaleContext";
import type { Locale } from "@/core/context/translations.types";

const LOCALES: { code: Locale; label: string }[] = [
  { code: "en", label: "EN" },
  { code: "fr", label: "FR" },
];

const CONTENT = {
  en: {
    title: "Absolute Privacy",
    subtitle: "Your private intelligence stays on your machine, always.",
    backToLogin: "Back to login",
    intro:
      "Unlike conventional cloud AI platforms that log your conversations, train on your datasets, and track your telemetry, Orasaka works under a strict local-first invariant. We believe your cognitive data belongs exclusively to you.",
    columns: [
      {
        title: "100% Local Inference & Video Workers",
        desc: "All neural computations run on your local GPU/CPU (Apple Metal MPS or Vulkan) using Ollama. For heavy video generation, tasks are routed to a stateless local video-worker running on port 8188 with MPS/CUDA hardware acceleration. Your prompts and video outputs never leave your machine.",
      },
      {
        title: "Zero-Telemetry Architecture",
        desc: "Orasaka does not contain hidden tracking code, analytics trackers, or usage monitors. The application runs completely sandboxed within your local environment.",
      },
      {
        title: "Encrypted Local Storage",
        desc: "All session contexts, memory blocks, and database nodes are saved locally in standard PostgreSQL/SQLite instances. Sensitive API credentials are encrypted on-disk using AES-256 keys.",
      },
    ],
    schemaTitle: "Local Security Sandbox Boundary",
    schemaDesc: "Zero outbound network lines — data flows in a closed local loop.",
  },
  fr: {
    title: "Confidentialité Absolue",
    subtitle: "Votre intelligence privée reste sur votre machine, toujours.",
    backToLogin: "Retour à la connexion",
    intro:
      "Contrairement aux plateformes d'IA cloud traditionnelles qui enregistrent vos conversations, s'entraînent sur vos données et suivent votre télémétrie, Orasaka fonctionne selon un invariant local strict. Votre propriété cognitive n'appartient qu'à vous.",
    columns: [
      {
        title: "Inférence & Workers Vidéo Locaux",
        desc: "Tous les calculs neuronaux s'exécutent sur votre GPU/CPU local (Apple Metal MPS ou Vulkan) via Ollama. Pour le rendu vidéo, les tâches sont déléguées à un worker vidéo local sans état (port 8188 avec accélération matérielle MPS/CUDA). Vos fichiers et requêtes ne quittent jamais votre machine.",
      },
      {
        title: "Zéro Télémétrie",
        desc: "Orasaka n'embarque aucun code de suivi, outil analytique ou mouchard. L'application tourne entièrement isolée dans votre environnement local.",
      },
      {
        title: "Stockage Local Chiffré",
        desc: "Tous les contextes, blocs de mémoire et bases vectorielles sont enregistrés dans vos instances PostgreSQL/SQLite locales. Vos clés d'API sont chiffrées avec AES-256.",
      },
    ],
    schemaTitle: "Frontière du Bac à Sable de Sécurité",
    schemaDesc: "Zéro transmission sortante — les données circulent dans une boucle locale fermée.",
  },
};

export default function AbsolutePrivacyFeaturePage() {
  const { t, locale, setLocale } = useTranslation();
  const c = CONTENT[locale] || CONTENT.en;

  return (
    <main className="min-h-screen flex flex-col items-center p-6 bg-[var(--surface-0)] ambient-grid w-full overflow-y-auto">
      {/* Navbar header */}
      <header className="w-full max-w-4xl flex items-center justify-between py-4 mb-10 border-b border-[var(--border-subtle)]">
        <Link href="/login" className="flex items-center gap-2 hover:opacity-80 transition-opacity">
          <img
            src="/logo.svg"
            alt="Orasaka Logo"
            width={24}
            height={24}
            className="w-6 h-6"
          />
          <span className="text-lg font-bold tracking-tight text-[var(--text-primary)]">
            Orasaka
          </span>
        </Link>

        <Link
          href="/login"
          className="text-xs font-semibold px-3 py-1.5 rounded-lg border border-[var(--border-default)] hover:border-[var(--accent)] transition-colors hover:text-[var(--accent)] text-[var(--text-secondary)]"
        >
          {c.backToLogin}
        </Link>
      </header>

      {/* Hero Header Content */}
      <div className="w-full max-w-4xl text-center space-y-4 mb-12">
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-500 border border-emerald-500/20">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
          Local-First Invariant
        </span>
        <h1 className="text-4xl font-extrabold tracking-tight text-[var(--text-primary)] md:text-5xl lg:text-6xl bg-gradient-to-r from-[var(--text-primary)] to-emerald-500 bg-clip-text text-transparent pb-1">
          {c.title}
        </h1>
        <p className="text-lg text-[var(--text-secondary)] max-w-2xl mx-auto leading-relaxed">
          {c.subtitle}
        </p>
      </div>

      {/* Main Glass Card container */}
      <article className="w-full max-w-4xl glass-card rounded-2xl p-6 md:p-10 space-y-12 animate-fade-up shadow-2xl mb-8">
        
        {/* Transparent SVG Schema */}
        <section className="flex flex-col items-center justify-center p-6 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-1)]/45 backdrop-blur-sm relative overflow-hidden group">
          <div className="absolute inset-0 bg-radial-gradient from-emerald-500/5 to-transparent opacity-40 pointer-events-none" />
          <h3 className="text-sm font-bold text-[var(--text-primary)] mb-1 flex items-center gap-2">
            {c.schemaTitle}
          </h3>
          <p className="text-[11px] text-[var(--text-muted)] mb-6 text-center">
            {c.schemaDesc}
          </p>

          <svg
            className="w-full max-w-lg h-auto"
            viewBox="0 0 500 200"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <style>{`
              @keyframes flow {
                to {
                  stroke-dashoffset: -20;
                }
              }
              @keyframes borderFlow {
                to {
                  stroke-dashoffset: -40;
                }
              }
              @keyframes pulseRing {
                0%, 100% { transform: scale(1); opacity: 0.15; }
                50% { transform: scale(1.2); opacity: 0.45; }
              }
              @keyframes pulseCross {
                0%, 100% { transform: scale(1); opacity: 1; }
                50% { transform: scale(1.1); opacity: 0.8; }
              }
              .svg-node {
                transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
                cursor: pointer;
              }
              .svg-node:hover {
                fill: var(--surface-3) !important;
                stroke: var(--accent) !important;
                filter: drop-shadow(0 0 8px var(--accent-soft));
              }
              .flow-arrow-1 {
                stroke-dasharray: 6 6;
                animation: flow 1.5s linear infinite;
              }
              .flow-arrow-2 {
                stroke-dasharray: 6 6;
                animation: flow 1.0s linear infinite;
              }
              .sandbox-boundary {
                stroke-dasharray: 8 6;
                animation: borderFlow 3s linear infinite;
              }
              .pulse-cross-ring {
                transform-origin: 250px 170px;
                animation: pulseRing 2s ease-in-out infinite;
              }
              .pulse-cross {
                transform-origin: 250px 170px;
                animation: pulseCross 1.5s ease-in-out infinite;
              }
            `}</style>

            {/* SVG Grid Overlay */}
            <defs>
              <pattern id="grid-pattern" width="16" height="16" patternUnits="userSpaceOnUse">
                <path d="M 16 0 L 0 0 0 16" fill="none" stroke="rgba(255,255,255,0.03)" strokeWidth="1" />
              </pattern>
            </defs>
            <rect width="100%" height="100%" fill="url(#grid-pattern)" className="rounded-lg" />

            {/* Sandbox Boundary Ring */}
            <rect
              x="10"
              y="10"
              width="480"
              height="180"
              rx="16"
              stroke="url(#gradient-sandbox)"
              strokeWidth="2"
              className="sandbox-boundary"
            />
            
            <linearGradient id="gradient-sandbox" x1="0" y1="0" x2="500" y2="200" gradientUnits="userSpaceOnUse">
              <stop offset="0%" stopColor="#10b981" stopOpacity="0.4" />
              <stop offset="50%" stopColor="#047857" stopOpacity="0.15" />
              <stop offset="100%" stopColor="#10b981" stopOpacity="0.4" />
            </linearGradient>

            {/* Connecting Arrows - solid background lines */}
            <path d="M 140 100 L 200 100" stroke="var(--border-subtle)" strokeWidth="1.5" />
            <path d="M 300 100 L 360 100" stroke="var(--border-subtle)" strokeWidth="1.5" />
            <path d="M 300 100 C 330 100, 330 155, 360 155" stroke="var(--border-subtle)" strokeWidth="1.5" fill="none" />

            {/* Connecting Arrows - flowing animation overlays */}
            <path d="M 140 100 L 200 100" stroke="#10b981" strokeWidth="2" className="flow-arrow-1" markerEnd="url(#arrow)" />
            <path d="M 300 100 L 360 100" stroke="#10b981" strokeWidth="2" className="flow-arrow-2" markerEnd="url(#arrow)" />
            <path d="M 300 100 C 330 100, 330 155, 360 155" stroke="#10b981" strokeWidth="2" className="flow-arrow-1" markerEnd="url(#arrow)" fill="none" />

            {/* Nodes */}
            {/* 1. Client Node */}
            <g className="svg-node">
              <rect x="40" y="70" width="100" height="60" rx="8" fill="var(--surface-2)" stroke="var(--border-subtle)" strokeWidth="1" />
              <text x="90" y="100" fill="var(--text-primary)" fontSize="11" fontWeight="bold" textAnchor="middle">User Interface</text>
              <text x="90" y="116" fill="var(--text-muted)" fontSize="9" textAnchor="middle">Next.js UI / CLI</text>
            </g>

            {/* 2. Core Node */}
            <g className="svg-node">
              <rect x="200" y="70" width="100" height="60" rx="8" fill="var(--surface-3)" stroke="var(--accent)" strokeWidth="1.5" />
              <text x="250" y="98" fill="var(--text-primary)" fontSize="11" fontWeight="bold" textAnchor="middle">Orasaka Core</text>
              <text x="250" y="114" fill="var(--accent)" fontSize="9" fontWeight="semibold" textAnchor="middle">Spring AI runtime</text>
            </g>

            {/* 3. Inference Node */}
            <g className="svg-node">
              <rect x="360" y="70" width="100" height="60" rx="8" fill="var(--surface-2)" stroke="var(--border-subtle)" strokeWidth="1" />
              <text x="410" y="98" fill="var(--text-primary)" fontSize="11" fontWeight="bold" textAnchor="middle">Local Engine</text>
              <text x="410" y="114" fill="var(--text-muted)" fontSize="9" textAnchor="middle">Ollama / MPS</text>
            </g>

            {/* 4. Video Worker Node */}
            <g className="svg-node">
              <rect x="360" y="135" width="100" height="45" rx="8" fill="var(--surface-2)" stroke="var(--border-subtle)" strokeWidth="1" />
              <text x="410" y="152" fill="var(--text-primary)" fontSize="9" fontWeight="bold" textAnchor="middle">Video Worker</text>
              <text x="410" y="165" fill="var(--text-muted)" fontSize="7" textAnchor="middle">Port 8188 / MPS</text>
            </g>
            
            {/* Outgoing blocking cross */}
            <path d="M 250 130 L 250 170" stroke="#ef4444" strokeWidth="1.5" strokeDasharray="2 2" />
            <circle cx="250" cy="170" r="12" fill="rgba(239,68,68,0.15)" stroke="#ef4444" strokeWidth="1" className="pulse-cross-ring" />
            <text x="250" y="174" fill="#ef4444" fontSize="10" fontWeight="bold" textAnchor="middle" className="pulse-cross">✕</text>
            <text x="312" y="173" fill="var(--text-muted)" fontSize="9" fontWeight="medium">Blocked Cloud Sync</text>

            {/* Arrow Marker Definition */}
            <defs>
              <marker id="arrow" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
                <path d="M 0 0 L 10 5 L 0 10 z" fill="#10b981" />
              </marker>
            </defs>
          </svg>
        </section>

        {/* Informative text columns */}
        <p className="text-[var(--text-secondary)] text-base md:text-lg leading-relaxed text-center max-w-3xl mx-auto">
          {c.intro}
        </p>

        <div className="grid gap-6 md:grid-cols-3 pt-4">
          {c.columns.map((column, idx) => (
            <section
              key={idx}
              className="p-6 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-1)] hover:border-[var(--border-strong)] transition-all duration-200 hover:-translate-y-1 shadow-sm hover:shadow-md"
            >
              <h2 className="text-base font-bold text-[var(--text-primary)] mb-3">
                {column.title}
              </h2>
              <p className="text-xs text-[var(--text-secondary)] leading-relaxed">
                {column.desc}
              </p>
            </section>
          ))}
        </div>
      </article>

      {/* Public Footer */}
      <footer className="w-full max-w-4xl flex items-center justify-between py-6 mt-6 border-t border-[var(--border-subtle)] text-xs text-[var(--text-muted)]">
        <div className="flex gap-4">
          <Link href="/privacy" className="hover:text-[var(--accent)] transition-colors underline">
            {t.auth.legalPrivacy}
          </Link>
          <Link href="/terms" className="hover:text-[var(--accent)] transition-colors">
            {t.auth.legalTerms}
          </Link>
          <Link href="/contact" className="hover:text-[var(--accent)] transition-colors">
            {t.auth.legalContact}
          </Link>
        </div>

        <nav className="auth-lang-switcher-inline" aria-label={t.auth.langSwitchLabel}>
          {LOCALES.map(({ code, label }, i) => (
            <React.Fragment key={code}>
              {i > 0 && <span className="auth-lang-divider">/</span>}
              <button
                type="button"
                className="auth-lang-link"
                data-active={locale === code}
                onClick={() => setLocale(code)}
              >
                {label}
              </button>
            </React.Fragment>
          ))}
        </nav>
      </footer>
    </main>
  );
}
