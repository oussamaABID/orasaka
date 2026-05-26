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
    title: "Privacy Policy",
    subtitle: "Your data stays on your machine. Always.",
    lastUpdated: "Last updated: June 2026",
    intro:
      "Orasaka is designed with a strict local-first architecture. We believe that your private intelligence should remain completely yours. This Privacy Policy details how data is managed within the Orasaka client interface.",
    sections: [
      {
        title: "1. Local Data Storage Invariant",
        text: "All database tables, chat history, system settings, model configuration states, and credentials are saved locally on your hardware. We do not collect, monitor, or sync your private data to any remote cloud servers.",
      },
      {
        title: "2. Zero Telemetry & Tracking",
        text: "There are no trackers, telemetry trackers, or third-party usage analytics built into Orasaka. All application code runs sandboxed, and we do not track which models you execute, what code you compile, or what prompts you write.",
      },
      {
        title: "3. Commercial Cloud Bridges",
        text: "If you explicitly configure and enable commercial cloud providers (such as OpenAI, Anthropic, or Gemini) in your local system settings, queries are sent directly to their endpoints. This usage is governed by their respective privacy terms. Orasaka does not intercept or log these requests.",
      },
      {
        title: "4. Local Filesystem Access",
        text: "Orasaka may read or write workspace files when executing coding tasks or tool workflows. All operations are performed locally using your terminal permissions and remain restricted to the paths you authorize.",
      },
    ],
    backToLogin: "Back to login",
  },
  fr: {
    title: "Politique de Confidentialité",
    subtitle: "Vos données restent sur votre machine. Toujours.",
    lastUpdated: "Dernière mise à jour : Juin 2026",
    intro:
      "Orasaka est conçu avec une architecture locale stricte. Nous croyons que votre intelligence privée doit rester entièrement vôtre. Cette politique détaille comment les données sont gérées dans l'interface client d'Orasaka.",
    sections: [
      {
        title: "1. Invariant de Stockage Local",
        text: "Toutes les tables de base de données, l'historique des discussions, les paramètres système, les configurations de modèle et vos clés d'API sont enregistrés localement sur votre matériel. Nous ne collectons ni ne synchronisons vos données privées sur aucun serveur distant.",
      },
      {
        title: "2. Zéro Télémétrie et Suivi",
        text: "Il n'y a aucun tracker, outil de télémétrie ou analyse d'utilisation tiers dans Orasaka. Tout le code de l'application s'exécute dans un bac à sable local, et nous ne suivons pas les modèles que vous utilisez, le code que vous compilez ou les invites que vous saisissez.",
      },
      {
        title: "3. Passerelles Cloud Commerciales",
        text: "Si vous configurez et activez explicitement des fournisseurs de cloud commerciaux (tels qu'OpenAI, Anthropic ou Gemini), les requêtes sont envoyées directement à leurs serveurs. Cet usage est régi par leurs conditions de confidentialité respectives. Orasaka n'intercepte ni n'enregistre ces requêtes.",
      },
      {
        title: "4. Accès au Système de Fichiers Local",
        text: "Orasaka peut lire ou écrire des fichiers dans votre espace de travail pour exécuter des tâches de code ou des flux de travail. Toutes les opérations s'effectuent localement en utilisant vos autorisations de terminal.",
      },
    ],
    backToLogin: "Retour à la connexion",
  },
};

export default function PrivacyPage() {
  const { t, locale, setLocale } = useTranslation();
  const c = CONTENT[locale] || CONTENT.en;

  return (
    <main className="min-h-screen flex flex-col items-center p-6 bg-[var(--surface-0)] ambient-grid w-full overflow-y-auto">
      {/* Navbar header */}
      <header className="w-full max-w-4xl flex items-center justify-between py-4 mb-6 border-b border-[var(--border-subtle)]">
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

      {/* Main Glass Card container */}
      <article className="w-full max-w-4xl glass-card rounded-2xl p-6 md:p-10 space-y-8 animate-fade-up shadow-2xl">
        <header className="space-y-2 border-b border-[var(--border-subtle)] pb-6">
          <h1 className="text-3xl font-extrabold tracking-tight text-[var(--text-primary)] md:text-4xl">
            {c.title}
          </h1>
          <p className="text-[var(--accent)] font-medium text-sm md:text-base">
            {c.subtitle}
          </p>
          <p className="text-xs text-[var(--text-muted)] pt-1">{c.lastUpdated}</p>
        </header>

        <p className="text-[var(--text-secondary)] text-sm md:text-base leading-relaxed">
          {c.intro}
        </p>

        <div className="grid gap-6 md:grid-cols-2 pt-2">
          {c.sections.map((section, idx) => (
            <section
              key={idx}
              className="p-5 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-1)] hover:border-[var(--border-strong)] transition-colors duration-200"
            >
              <h2 className="text-base font-bold text-[var(--text-primary)] mb-2">
                {section.title}
              </h2>
              <p className="text-xs text-[var(--text-secondary)] leading-relaxed">
                {section.text}
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
