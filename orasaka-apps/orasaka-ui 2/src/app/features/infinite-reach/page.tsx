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
    title: "Infinite Reach",
    subtitle: "Outbound reverse tunnel, local SQLite job store & sandboxed automation.",
    backToLogin: "Back to login",
    intro:
      "To execute tasks on your local environment (like editing code files, compiling, running scripts, or querying DBs), Orasaka utilizes a secure reverse-tunneling protocol combined with transactional SQLite persistence on the local host. You get 100% security with no open inbound firewall ports.",
    columns: [
      {
        title: "Zero Inbound Firewall Ports",
        desc: "The CLI agent initiates an outbound Server-Sent Events (SSE) tunnel. Your local machine stays safely hidden behind NAT with zero open ports exposed to the network.",
      },
      {
        title: "SQLite Transactional Job Store",
        desc: "Every command is immediately saved to the CLI's embedded SQLite database (~/.orasaka-tasks.db) as 'PENDING'. If the tunnel drops, the state is persisted and reconciled automatically upon reconnecting.",
      },
      {
        title: "Sandboxed Local Consent",
        desc: "No script runs silently. The local agent prompts you with an interactive [Y/n] consent check. Once approved, the command runs inside a sandboxed directory timeout-bounded to 300 seconds.",
      },
    ],
    schemaTitle: "Orasaka Transactional Agent Topology",
    schemaDesc: "Outbound-only SSE tunnel writing to an embedded SQLite store before local sandbox execution.",
    sduiTitle: "Server-Driven Dynamic UI (SDUI)",
    sduiSubtitle: "Deploy dynamic layouts and interactive approval screens instantly, no client rebuilds required.",
    sduiCard1Title: "Dynamic Template Registry",
    sduiCard1Desc: "The Gateway manages structural JSON-defined layouts. Templates are pushed down the SSE stream to dynamically render fields, status widgets, and output interfaces.",
    sduiCard2Title: "Interactive Consent Cards",
    sduiCard2Desc: "When the CLI agent dispatches file transfers, code reviews, or Slack posts, the frontend displays beautiful dynamic consent forms custom-built for that specific task.",
  },
  fr: {
    title: "Portée Infinie",
    subtitle: "Tunnel inverse sortant, stockage des tâches SQLite local et automatisation isolée.",
    backToLogin: "Retour à la connexion",
    intro:
      "Pour exécuter des tâches sur votre machine locale (comme modifier des fichiers, compiler du code ou interroger des bases de données), Orasaka utilise un protocole de tunnel inverse sécurisé couplé à une base SQLite transactionnelle. La sécurité est totale, sans aucun port réseau ouvert.",
    columns: [
      {
        title: "Zéro Port Ouvert (Pare-Feu)",
        desc: "L'agent CLI initie une connexion SSE sortante. Votre machine reste hermétique derrière son NAT/pare-feu, sans aucun point d'accès réseau exposé aux intrusions.",
      },
      {
        title: "Persistance SQLite Locale",
        desc: "Chaque commande reçue est enregistrée dans une base SQLite embarquée (~/.orasaka-tasks.db) à l'état 'PENDING'. En cas de coupure réseau, la tâche est conservée puis synchronisée à la reconnexion.",
      },
      {
        title: "Consentement & Sandbox Locale",
        desc: "Aucune commande ne tourne en cachette. L'agent local vous demande validation via [Y/n]. Après votre accord, le script s'exécute localement dans un environnement surveillé et limité à 300 secondes.",
      },
    ],
    schemaTitle: "Topologie Transactionnelle de l'Agent",
    schemaDesc: "Tunnel SSE sortant enregistrant les tâches dans SQLite avant exécution locale sécurisée.",
    sduiTitle: "Server-Driven Dynamic UI (SDUI)",
    sduiSubtitle: "Déployez des interfaces dynamiques et des écrans d'approbation instantanément, sans recompilation.",
    sduiCard1Title: "Registre de Modèles Dynamiques",
    sduiCard1Desc: "La passerelle gère les structures de mise en page définies en JSON. Les modèles sont envoyés dans le flux SSE pour générer des formulaires et des widgets interactifs à la volée.",
    sduiCard2Title: "Cartes d'Approbation Interactives",
    sduiCard2Desc: "Lorsque l'agent local prépare une modification de fichier ou un dispatch Slack, l'interface affiche un formulaire d'approbation riche et dynamique, adapté sur mesure à la tâche.",
  },
};

export default function InfiniteReachFeaturePage() {
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
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-cyan-500/10 text-cyan-500 border border-cyan-500/20">
          <span className="w-1.5 h-1.5 rounded-full bg-cyan-500 animate-pulse" />
          Model Context Protocol (MCP) & Local Automation
        </span>
        <h1 className="text-4xl font-extrabold tracking-tight text-[var(--text-primary)] md:text-5xl lg:text-6xl bg-gradient-to-r from-[var(--text-primary)] to-cyan-500 bg-clip-text text-transparent pb-1">
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
          <div className="absolute inset-0 bg-radial-gradient from-cyan-500/5 to-transparent opacity-40 pointer-events-none" />
          <h3 className="text-sm font-bold text-[var(--text-primary)] mb-1 flex items-center gap-2">
            {c.schemaTitle}
          </h3>
          <p className="text-[11px] text-[var(--text-muted)] mb-6 text-center">
            {c.schemaDesc}
          </p>

          <svg
            className="w-full max-w-2xl h-auto"
            viewBox="0 0 600 220"
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
              @keyframes pulseLine {
                0%, 100% { opacity: 0.25; }
                50% { opacity: 0.65; }
              }
              .svg-node {
                transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
                cursor: pointer;
              }
              .svg-node:hover {
                filter: drop-shadow(0 0 8px var(--accent-soft));
              }
              .svg-node:hover rect {
                fill: var(--surface-3) !important;
                stroke: var(--accent) !important;
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
                animation: flow 1s linear infinite;
              }
              .flow-sync {
                stroke-dasharray: 5 5;
                animation: flowReverse 1.5s linear infinite;
              }
              .firewall-line {
                animation: pulseFirewall 2.5s ease-in-out infinite;
              }
              @keyframes pulseFirewall {
                0%, 100% { opacity: 0.25; }
                50% { opacity: 0.65; }
              }
            `}</style>

            {/* Grid overlay */}
            <defs>
              <pattern id="grid-pattern-infinite" width="16" height="16" patternUnits="userSpaceOnUse">
                <path d="M 16 0 L 0 0 0 16" fill="none" stroke="rgba(255,255,255,0.03)" strokeWidth="1" />
              </pattern>
              <marker id="arrow-infinite" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse">
                <path d="M 0 0 L 10 5 L 0 10 z" fill="var(--accent)" />
              </marker>
              <marker id="arrow-red" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse">
                <path d="M 0 0 L 10 5 L 0 10 z" fill="#ef4444" />
              </marker>
            </defs>
            <rect width="100%" height="100%" fill="url(#grid-pattern-infinite)" className="rounded-lg" />

            {/* Firewall barrier Line */}
            <line x1="180" y1="20" x2="180" y2="200" stroke="#ef4444" strokeWidth="2" strokeDasharray="4 2" className="firewall-line" />
            <text x="174" y="32" fill="#ef4444" fontSize="8" fontWeight="bold" textAnchor="end" className="firewall-line">Local Firewall</text>

            {/* Local Host Boundary box */}
            <rect x="210" y="15" width="375" height="190" rx="12" stroke="var(--accent)" strokeWidth="1" strokeOpacity="0.15" fill="rgba(255,255,255,0.005)" />
            <text x="575" y="28" fill="var(--text-muted)" fontSize="7" fontWeight="bold" textAnchor="end" letterSpacing="0.05em">LOCAL HOST BOUNDARY (SAFE SANDBOX)</text>

            {/* Connection Lines - Solid Background */}
            <path d="M 140 120 C 180 120, 180 65, 230 65" stroke="var(--border-subtle)" strokeWidth="1.5" fill="none" />
            <line x1="350" y1="65" x2="430" y2="65" stroke="var(--border-subtle)" strokeWidth="1.5" />
            <path d="M 290 95 C 290 120, 310 145, 340 145" stroke="var(--border-subtle)" strokeWidth="1.5" fill="none" />
            <path d="M 460 145 C 490 145, 490 95, 490 95" stroke="var(--border-subtle)" strokeWidth="1.5" fill="none" />

            {/* Connection Lines - Animated Flows */}
            {/* 1. Gateway to CLI (Job Dispatch) */}
            <path d="M 140 120 C 180 120, 180 65, 230 65" stroke="var(--accent)" strokeWidth="2" fill="none" className="flow-dispatch" markerEnd="url(#arrow-infinite)" />
            
            {/* 2. CLI to SQLite (Persist payload) */}
            <line x1="350" y1="65" x2="430" y2="65" stroke="var(--accent)" strokeWidth="2" className="flow-persist" markerEnd="url(#arrow-infinite)" />
            
            {/* 3. CLI to Sandbox Executor (Command dispatch upon [Y/n] consent) */}
            <path d="M 290 95 C 290 120, 310 145, 340 145" stroke="var(--accent)" strokeWidth="2" fill="none" className="flow-exec" markerEnd="url(#arrow-infinite)" />
            
            {/* 4. Sandbox Executor to SQLite (Record logs & exit code) */}
            <path d="M 460 145 C 490 145, 490 95, 490 95" stroke="var(--accent)" strokeWidth="2" fill="none" className="flow-persist" markerEnd="url(#arrow-infinite)" />

            {/* Nodes */}
            {/* Orasaka Gateway */}
            <g className="svg-node" style={{ transformOrigin: "85px 120px" }}>
              <rect x="30" y="90" width="110" height="60" rx="8" fill="var(--surface-2)" stroke="var(--border-subtle)" strokeWidth="1" />
              <text x="85" y="118" fill="var(--text-primary)" fontSize="10" fontWeight="bold" textAnchor="middle">Orasaka Gateway</text>
              <text x="85" y="132" fill="var(--text-muted)" fontSize="8" textAnchor="middle">BFF Cloud Instance</text>
            </g>

            {/* CLI Agent Listener */}
            <g className="svg-node" style={{ transformOrigin: "290px 65px" }}>
              <rect x="230" y="38" width="120" height="54" rx="8" fill="var(--surface-3)" stroke="var(--accent)" strokeWidth="1.5" />
              <text x="290" y="60" fill="var(--text-primary)" fontSize="10" fontWeight="bold" textAnchor="middle">CLI Agent</text>
              <text x="290" y="72" fill="var(--accent)" fontSize="8" fontWeight="bold" textAnchor="middle">`orasaka listen`</text>
              <text x="290" y="83" fill="var(--text-muted)" fontSize="7" textAnchor="middle">reverse SSE tunnel</text>
            </g>

            {/* Local SQLite DB */}
            <g className="svg-node" style={{ transformOrigin: "490px 65px" }}>
              <rect x="430" y="38" width="120" height="54" rx="8" fill="var(--surface-2)" stroke="var(--border-subtle)" strokeWidth="1" />
              <text x="490" y="58" fill="var(--text-primary)" fontSize="10" fontWeight="bold" textAnchor="middle">SQLite DB</text>
              <text x="490" y="70" fill="var(--text-muted)" fontSize="7" textAnchor="middle">~/.orasaka-tasks.db</text>
              <text x="490" y="81" fill="emerald-500" fontSize="7" fontWeight="bold" textAnchor="middle">resilient offline queue</text>
            </g>

            {/* Local Sandbox Executor */}
            <g className="svg-node" style={{ transformOrigin: "390px 167px" }}>
              <rect x="320" y="140" width="140" height="54" rx="8" fill="var(--surface-2)" stroke="var(--border-subtle)" strokeWidth="1" />
              <text x="390" y="160" fill="var(--text-primary)" fontSize="10" fontWeight="bold" textAnchor="middle">Sandbox Executor</text>
              <text x="390" y="172" fill="var(--text-muted)" fontSize="7" textAnchor="middle">Local scripts & bash commands</text>
              <text x="390" y="183" fill="amber-500" fontSize="7" fontWeight="bold" textAnchor="middle">timeout-bounded (300s)</text>
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

        {/* Server-Driven Dynamic UI (SDUI) Section */}
        <section className="border-t border-[var(--border-subtle)] pt-10 space-y-6">
          <div className="text-center max-w-2xl mx-auto space-y-3">
            <h2 className="text-2xl font-bold tracking-tight text-[var(--text-primary)] md:text-3xl bg-gradient-to-r from-[var(--text-primary)] to-cyan-400 bg-clip-text text-transparent">
              {c.sduiTitle}
            </h2>
            <p className="text-sm text-[var(--text-secondary)] leading-relaxed">
              {c.sduiSubtitle}
            </p>
          </div>

          <div className="grid gap-6 md:grid-cols-2 pt-2">
            <div className="p-6 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-1)]/30 backdrop-blur-sm hover:border-[var(--border-strong)] transition-colors duration-200">
              <h3 className="text-sm font-bold text-[var(--text-primary)] mb-2 flex items-center gap-2">
                {c.sduiCard1Title}
              </h3>
              <p className="text-xs text-[var(--text-secondary)] leading-relaxed">
                {c.sduiCard1Desc}
              </p>
            </div>

            <div className="p-6 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-1)]/30 backdrop-blur-sm hover:border-[var(--border-strong)] transition-colors duration-200">
              <h3 className="text-sm font-bold text-[var(--text-primary)] mb-2 flex items-center gap-2">
                {c.sduiCard2Title}
              </h3>
              <p className="text-xs text-[var(--text-secondary)] leading-relaxed">
                {c.sduiCard2Desc}
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
