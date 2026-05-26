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
    title: "Unified Engine",
    subtitle: "Orasaka cognitive pipeline resolving Interceptors, RAG & Tool chains.",
    backToLogin: "Back to login",
    intro:
      "Executing multi-modal AI requests requires more than raw model calls. Orasaka routes every prompt through a secure, structured Spring AI pipeline—resolving active interceptors, querying local PGVector stores, and attaching dynamic tools before routing to the optimal hardware engine.",
    columns: [
      {
        title: "Cognitive Interceptors",
        desc: "Prompts flow through a type-safe Spring AI chain: UserContextResolver, SystemContextInjector, LanguageAlignmentInterceptor, and MemoryInterceptor run sequentially to inject environment variables, resolve security policies, align language outputs, and restore conversation history.",
      },
      {
        title: "PGVector RAG Pipeline",
        desc: "The RagInterceptor automatically executes semantic search on the local PGVector database. It retrieves relevant text embeddings and appends private domain knowledge to the prompt context before routing.",
      },
      {
        title: "Dynamic MCP Tools",
        desc: "The ToolInterceptor attaches runtime schemas from the Model Context Protocol (MCP) registry on-demand. Matches active intents with Slack, Jira, database, or sandboxed filesystem tools.",
      },
    ],
    schemaTitle: "Orasaka Cognitive Pipeline & Modality Routing",
    schemaDesc: "Gateway requests passing through interceptors, RAG, and tool registries before execution.",
  },
  fr: {
    title: "Moteur Unifié",
    subtitle: "Orasaka orchestrateur de pipeline résolvant intercepteurs, RAG et outils.",
    backToLogin: "Retour à la connexion",
    intro:
      "L'exécution de requêtes IA multi-modales nécessite plus qu'un simple appel modèle. Orasaka structure ses flux via un pipeline Spring AI : il résout les intercepteurs, interroge les bases vectorielles PGVector locales et attache des outils dynamiques avant d'activer le bon moteur.",
    columns: [
      {
        title: "Intercepteurs Cognitifs",
        desc: "Les prompts traversent une chaîne d'intercepteurs typés (Spring AI) : UserContextResolver, SystemContextInjector, LanguageAlignmentInterceptor, et MemoryInterceptor s'exécutent en séquence pour injecter l'environnement, valider la sécurité, et charger la mémoire.",
      },
      {
        title: "Pipeline RAG PGVector",
        desc: "Le RagInterceptor exécute une recherche sémantique sur la base PGVector locale. Il récupère les correspondances vectorielles pertinentes pour enrichir le contexte du prompt avant l'envoi au modèle.",
      },
      {
        title: "Outils Dynamiques MCP",
        desc: "Le ToolInterceptor attache à la volée les schémas d'outils issus du registre Model Context Protocol (MCP). Il connecte l'agent aux API Slack, Jira, bases de données ou scripts locaux sécurisés.",
      },
    ],
    schemaTitle: "Pipeline Cognitif Orasaka & Routage Modalities",
    schemaDesc: "Flux gateway traversant les intercepteurs, le RAG et les outils avant dispatch multi-modal.",
  },
};

export default function UnifiedEngineFeaturePage() {
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
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-500 border border-amber-500/20">
          <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse" />
          Multi-Modal Pipeline Orchestration
        </span>
        <h1 className="text-4xl font-extrabold tracking-tight text-[var(--text-primary)] md:text-5xl lg:text-6xl bg-gradient-to-r from-[var(--text-primary)] to-amber-500 bg-clip-text text-transparent pb-1">
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
          <div className="absolute inset-0 bg-radial-gradient from-amber-500/5 to-transparent opacity-40 pointer-events-none" />
          <h3 className="text-sm font-bold text-[var(--text-primary)] mb-1 flex items-center gap-2">
            {c.schemaTitle}
          </h3>
          <p className="text-[11px] text-[var(--text-muted)] mb-6 text-center">
            {c.schemaDesc}
          </p>

          <svg
            className="w-full max-w-2xl h-auto"
            viewBox="0 0 600 240"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <style>{`
              @keyframes flow {
                to {
                  stroke-dashoffset: -20;
                }
              }
              @keyframes flowReverse {
                to {
                  stroke-dashoffset: 20;
                }
              }
              @keyframes pulseCircle {
                0%, 100% { transform: scale(1); filter: drop-shadow(0 0 2px var(--accent-soft)); }
                50% { transform: scale(1.04); filter: drop-shadow(0 0 6px var(--accent-soft)); }
              }
              .svg-node {
                transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
                cursor: pointer;
              }
              .svg-node:hover rect {
                fill: var(--surface-3) !important;
                stroke: var(--accent) !important;
                filter: drop-shadow(0 0 8px var(--accent-soft));
              }
              .svg-node:hover circle {
                fill: var(--surface-3) !important;
                stroke: var(--accent) !important;
                filter: drop-shadow(0 0 8px var(--accent-soft));
              }
              .svg-node-center {
                transform-origin: 415px 115px;
                animation: pulseCircle 3s ease-in-out infinite;
                transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
                cursor: pointer;
              }
              .svg-node-center:hover {
                filter: drop-shadow(0 0 12px var(--accent));
              }
              .flow-dispatch {
                stroke-dasharray: 5 5;
                animation: flow 1.5s linear infinite;
              }
              .flow-persist {
                stroke-dasharray: 4 4;
                animation: flow 1.2s linear infinite;
              }
              .flow-exec {
                stroke-dasharray: 5 5;
                animation: flow 1.5s linear infinite;
              }
            `}</style>

            {/* Grid background inside SVG */}
            <defs>
              <pattern id="grid-pattern-engine" width="16" height="16" patternUnits="userSpaceOnUse">
                <path d="M 16 0 L 0 0 0 16" fill="none" stroke="rgba(255,255,255,0.03)" strokeWidth="1" />
              </pattern>
              <marker id="arrow-engine" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse">
                <path d="M 0 0 L 10 5 L 0 10 z" fill="var(--accent)" />
              </marker>
            </defs>
            <rect width="100%" height="100%" fill="url(#grid-pattern-engine)" className="rounded-lg" />

            {/* Background solid lines */}
            <path d="M 110 115 C 130 115, 120 59, 142 59" stroke="var(--border-subtle)" strokeWidth="1.5" fill="none" />
            <line x1="237" y1="80" x2="237" y2="92" stroke="var(--border-subtle)" strokeWidth="1.5" />
            <line x1="237" y1="134" x2="237" y2="146" stroke="var(--border-subtle)" strokeWidth="1.5" />
            <path d="M 332 167 C 355 167, 355 115, 375 115" stroke="var(--border-subtle)" strokeWidth="1.5" fill="none" />
            <path d="M 455 105 L 507 48" stroke="var(--border-subtle)" strokeWidth="1.5" />
            <path d="M 455 112 L 505 92" stroke="var(--border-subtle)" strokeWidth="1.5" />
            <path d="M 455 122 L 505 142" stroke="var(--border-subtle)" strokeWidth="1.5" />
            <path d="M 455 129 L 507 182" stroke="var(--border-subtle)" strokeWidth="1.5" />

            {/* Active flowing animated lines */}
            <path d="M 110 115 C 130 115, 120 59, 142 59" stroke="var(--accent)" strokeWidth="2" fill="none" className="flow-dispatch" markerEnd="url(#arrow-engine)" />
            <line x1="237" y1="80" x2="237" y2="92" stroke="var(--accent)" strokeWidth="2" className="flow-persist" markerEnd="url(#arrow-engine)" />
            <line x1="237" y1="134" x2="237" y2="146" stroke="var(--accent)" strokeWidth="2" className="flow-persist" markerEnd="url(#arrow-engine)" />
            <path d="M 332 167 C 355 167, 355 115, 375 115" stroke="var(--accent)" strokeWidth="2" fill="none" className="flow-dispatch" markerEnd="url(#arrow-engine)" />
            <path d="M 455 105 L 507 48" stroke="var(--accent)" strokeWidth="2" className="flow-exec" markerEnd="url(#arrow-engine)" />
            <path d="M 455 112 L 505 92" stroke="var(--accent)" strokeWidth="2" className="flow-exec" markerEnd="url(#arrow-engine)" />
            <path d="M 455 122 L 505 142" stroke="var(--accent)" strokeWidth="2" className="flow-exec" markerEnd="url(#arrow-engine)" />
            <path d="M 455 129 L 507 182" stroke="var(--accent)" strokeWidth="2" className="flow-exec" markerEnd="url(#arrow-engine)" />

            {/* Inbound Gateway BFF */}
            <g className="svg-node" style={{ transformOrigin: "62px 115px" }}>
              <rect x="15" y="87" width="95" height="56" rx="8" fill="var(--surface-2)" stroke="var(--border-subtle)" strokeWidth="1" />
              <text x="62" y="112" fill="var(--text-primary)" fontSize="9" fontWeight="bold" textAnchor="middle">Gateway BFF</text>
              <text x="62" y="125" fill="var(--text-muted)" fontSize="7" textAnchor="middle">Inbound Request</text>
            </g>

            {/* Orasaka Core Pipeline Boundary */}
            <rect x="130" y="15" width="215" height="205" rx="12" stroke="var(--accent)" strokeWidth="1" strokeOpacity="0.15" fill="none" />
            <text x="237" y="28" fill="var(--text-muted)" fontSize="7" fontWeight="bold" textAnchor="middle" letterSpacing="0.05em">COGNITIVE ORCHESTRATOR</text>

            {/* Pipeline Step 1: Interceptors */}
            <g className="svg-node" style={{ transformOrigin: "237px 59px" }}>
              <rect x="142" y="38" width="190" height="42" rx="6" fill="var(--surface-2)" stroke="var(--border-subtle)" strokeWidth="1" />
              <text x="237" y="58" fill="var(--text-primary)" fontSize="9" fontWeight="bold" textAnchor="middle">Cognitive Interceptors</text>
              <text x="237" y="70" fill="var(--text-muted)" fontSize="7" textAnchor="middle">Context, Memory, Translation</text>
            </g>

            {/* Pipeline Step 2: RAG PGVector */}
            <g className="svg-node" style={{ transformOrigin: "237px 113px" }}>
              <rect x="142" y="92" width="190" height="42" rx="6" fill="var(--surface-2)" stroke="var(--border-subtle)" strokeWidth="1" />
              <text x="237" y="112" fill="var(--text-primary)" fontSize="9" fontWeight="bold" textAnchor="middle">Hybrid RAG (PGVector)</text>
              <text x="237" y="124" fill="var(--text-muted)" fontSize="7" textAnchor="middle">Semantic Knowledge Retrieval</text>
            </g>

            {/* Pipeline Step 3: Tool Callbacks */}
            <g className="svg-node" style={{ transformOrigin: "237px 167px" }}>
              <rect x="142" y="146" width="190" height="42" rx="6" fill="var(--surface-3)" stroke="var(--accent)" strokeWidth="1" />
              <text x="237" y="166" fill="var(--text-primary)" fontSize="9" fontWeight="bold" textAnchor="middle">MCP Tool Registry</text>
              <text x="237" y="178" fill="var(--accent)" fontSize="7" fontWeight="bold" textAnchor="middle">Slack, Jira, SQL, FS Tools</text>
            </g>

            {/* Central Gateway Facade */}
            <g className="svg-node-center">
              <rect x="375" y="85" width="80" height="60" rx="8" fill="var(--surface-3)" stroke="var(--accent)" strokeWidth="1.5" />
              <text x="415" y="112" fill="var(--text-primary)" fontSize="10" fontWeight="bold" textAnchor="middle">Unified</text>
              <text x="415" y="125" fill="var(--accent)" fontSize="10" fontWeight="bold" textAnchor="middle">Facade</text>
            </g>

            {/* Modalities (Surrounding Nodes) */}
            {/* TEXT */}
            <g className="svg-node" style={{ transformOrigin: "525px 40px" }}>
              <circle cx="525" cy="40" r="18" fill="var(--surface-2)" stroke="var(--border-default)" strokeWidth="1" />
              <text x="525" y="43" fill="var(--text-primary)" fontSize="7" fontWeight="bold" textAnchor="middle">TXT</text>
            </g>

            {/* CODE */}
            <g className="svg-node" style={{ transformOrigin: "525px 90px" }}>
              <circle cx="525" cy="90" r="18" fill="var(--surface-2)" stroke="var(--border-default)" strokeWidth="1" />
              <text x="525" y="93" fill="var(--text-primary)" fontSize="7" fontWeight="bold" textAnchor="middle">COD</text>
            </g>

            {/* MEDIA */}
            <g className="svg-node" style={{ transformOrigin: "525px 140px" }}>
              <circle cx="525" cy="140" r="18" fill="var(--surface-2)" stroke="var(--border-default)" strokeWidth="1" />
              <text x="525" y="143" fill="var(--text-primary)" fontSize="7" fontWeight="bold" textAnchor="middle">MED</text>
            </g>

            {/* VISION */}
            <g className="svg-node" style={{ transformOrigin: "525px 190px" }}>
              <circle cx="525" cy="190" r="18" fill="var(--surface-2)" stroke="var(--border-default)" strokeWidth="1" />
              <text x="525" y="193" fill="var(--text-primary)" fontSize="7" fontWeight="bold" textAnchor="middle">VIS</text>
            </g>
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

        {/* Detailed Interceptor Pipeline Steps */}
        <section className="border-t border-[var(--border-subtle)] pt-10 space-y-6">
          <div className="text-center max-w-2xl mx-auto space-y-3">
            <h2 className="text-2xl font-bold tracking-tight text-[var(--text-primary)] md:text-3xl bg-gradient-to-r from-[var(--text-primary)] to-amber-500 bg-clip-text text-transparent pb-1">
              Cognitive Interceptor execution sequence
            </h2>
            <p className="text-xs text-[var(--text-secondary)] leading-relaxed">
              Every request is dynamically processed through a 10-tier pipeline resolving security controls, memory injections, vector storage, and 3-tier self-correction validation.
            </p>
          </div>

          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3 pt-2 text-left">
            {[
              { order: "01", name: "UserContextResolver", desc: "Checks user profile details, active RBAC constraints, and rate-limiting tiers." },
              { order: "02", name: "SystemContextInjector", desc: "Injects environment signals, system variables, and currently active local tools." },
              { order: "03", name: "LanguageAlignmentInterceptor", desc: "Forces LLM internal reasoning in English while aligning output translation to user preferences." },
              { order: "04", name: "MemoryInterceptor", desc: "Restores context of the recent conversation history in a FIFO window from the local DB." },
              { order: "05", name: "RagInterceptor", desc: "Queries local PGVector store using hybrid lexical/dense search to inject context nodes." },
              { order: "06", name: "McpInterceptor", desc: "Resolves dynamic schemas and schemas mapping from the Model Context Protocol." },
              { order: "07", name: "Refiner & Router", desc: "Performs fuzzy query optimization and routes context to the optimal hardware engine." },
              { order: "08", name: "ToolInterceptor", desc: "Attaches runtime callback declarations for execute hooks matching user intent." },
              { order: "09", name: "CostShieldInterceptor", desc: "Monitors local unified memory usage, auto-shifting loads to cloud adapters if memory >85%." },
              { order: "10", name: "QuantumValidationAdvisor", desc: "Runs 3-tier self-correction: strict JSON validation, sandbox crash test, and semantic consensus debate." }
            ].map((step) => (
              <div key={step.order} className="p-4 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-1)]/30 backdrop-blur-sm hover:border-[var(--border-strong)] transition-all duration-200">
                <div className="flex items-center gap-2.5 mb-2">
                  <span className="font-mono text-[10px] font-bold text-amber-500 bg-amber-500/10 px-1.5 py-0.5 rounded">
                    {step.order}
                  </span>
                  <h4 className="text-xs font-bold text-[var(--text-primary)]">
                    {step.name}
                  </h4>
                </div>
                <p className="text-[10px] text-[var(--text-secondary)] leading-relaxed">
                  {step.desc}
                </p>
              </div>
            ))}
          </div>
        </section>

        {/* Quantum Validation Advisor Section */}
        <section className="border-t border-[var(--border-subtle)] pt-10 space-y-6">
          <div className="text-center max-w-2xl mx-auto space-y-3">
            <h2 className="text-2xl font-bold tracking-tight text-[var(--text-primary)] md:text-3xl bg-gradient-to-r from-[var(--text-primary)] to-amber-500 bg-clip-text text-transparent pb-1">
              Quantum Validation Advisor
            </h2>
            <p className="text-xs text-[var(--text-secondary)] leading-relaxed">
              Orasaka's unique, closed-loop self-correction architecture validates model outputs using three distinct, automated approaches before delivering payloads.
            </p>
          </div>

          <div className="grid gap-6 md:grid-cols-3 pt-2 text-left">
            <div className="p-5 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-1)]/30 backdrop-blur-sm hover:border-[var(--border-strong)] transition-all duration-200">
              <div className="font-mono text-[10px] font-bold text-amber-500 mb-2">TIER A</div>
              <h3 className="text-sm font-bold text-[var(--text-primary)] mb-2">Deterministic JSON Schema</h3>
              <p className="text-[11px] text-[var(--text-secondary)] leading-relaxed">
                Executes structural parses of JSON payloads using Jackson ObjectMapper at zero-token cost. Any malformed syntax triggers an instant, localized pipeline retry.
              </p>
            </div>

            <div className="p-5 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-1)]/30 backdrop-blur-sm hover:border-[var(--border-strong)] transition-all duration-200">
              <div className="font-mono text-[10px] font-bold text-amber-500 mb-2">TIER B</div>
              <h3 className="text-sm font-bold text-[var(--text-primary)] mb-2">MCP Sandbox Crash-Test</h3>
              <p className="text-[11px] text-[var(--text-secondary)] leading-relaxed">
                Extracts code snippets and compiles them inside an isolated Model Context Protocol (MCP) compilation sandbox, feeding syntax errors back to self-correct the model dynamically.
              </p>
            </div>

            <div className="p-5 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-1)]/30 backdrop-blur-sm hover:border-[var(--border-strong)] transition-all duration-200">
              <div className="font-mono text-[10px] font-bold text-amber-500 mb-2">TIER C</div>
              <h3 className="text-sm font-bold text-[var(--text-primary)] mb-2">Semantic Consensus Debate</h3>
              <p className="text-[11px] text-[var(--text-secondary)] leading-relaxed">
                Spawns independent Advocate and Critic agent personas at zero temperature to review response logic. The request is retried if the Critic detects alignment flaws.
              </p>
            </div>
          </div>
        </section>
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
