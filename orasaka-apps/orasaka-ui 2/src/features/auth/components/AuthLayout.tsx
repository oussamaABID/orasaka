/* eslint-disable */
"use client";

import React from "react";
import Link from "next/link";
import { SentinelMascot } from "@/features/auth/components/SentinelMascot";
import { useTranslation } from "@/core/context/LocaleContext";
import type { Locale } from "@/core/context/translations.types";

/** Feature bullet metadata for the marketing hero */
const HERO_FEATURES: { index: string; key: "heroFeature1" | "heroFeature2" | "heroFeature3" }[] = [
  { index: "01", key: "heroFeature1" },
  { index: "02", key: "heroFeature2" },
  { index: "03", key: "heroFeature3" },
];

/** Available locales for the discreet footer switcher */
const LOCALES: { code: Locale; label: string }[] = [
  { code: "en", label: "EN" },
  { code: "fr", label: "FR" },
];

interface AuthLayoutProps {
  children: React.ReactNode;
}

/**
 * Shared split-screen layout for authentication pages (login, register, forgot-password).
 * Left panel: marketing hero with branding, impactful headline, and feature bullets.
 * Right panel: form content provided via children, with discreet footer (legal + language).
 */
export function AuthLayout({ children }: Readonly<AuthLayoutProps>) {
  const { t, locale, setLocale } = useTranslation();

  return (
    <main className="login-split">
      {/* ── Left: Marketing hero ───────────────────────────────────── */}
      <section
        className="login-hero gradient-mesh ambient-grid"
        aria-label="Product overview"
      >
        <div className="login-hero-content">
          <div className="login-hero-brand">
            <img
              src="/logo.svg"
              alt="Orasaka Logo"
              width={32}
              height={32}
              className="w-8 h-8"
            />
            <span className="text-xl font-bold tracking-tight text-[var(--text-primary)]">
              Orasaka
            </span>
          </div>

          <h1 className="login-hero-headline">{t.auth.heroHeadline}</h1>

          <p className="login-hero-tagline">
            {t.auth.heroTagline}
          </p>

          <ul className="login-hero-features">
            {HERO_FEATURES.map(({ index, key }) => {
              const text = t.auth[key];
              const parts = text.split(" — ");
              const title = parts[0];
              const desc = parts[1];
              const route = key === "heroFeature1" ? "absolute-privacy" : key === "heroFeature2" ? "unified-engine" : "infinite-reach";

              return (
                <li key={key} className="w-full">
                  <Link
                    href={`/features/${route}`}
                    className="login-hero-feature group"
                  >
                    <span className="login-hero-feature-index">
                      {index}
                    </span>
                    <div className="login-hero-feature-content">
                      <span className="login-hero-feature-title">
                        {title}
                      </span>
                      {desc && (
                        <span className="login-hero-feature-desc">
                          {desc}
                        </span>
                      )}
                    </div>
                  </Link>
                </li>
              );
            })}
          </ul>

          {/* ── Sentinel AI Mascot ─────────────────────────── */}
          <SentinelMascot />
        </div>

        <p className="login-hero-footer">
          © {new Date().getFullYear()} Orasaka · MIT License - by{" "}
          <a
            href="https://www.krizaka.com/"
            target="_blank"
            rel="noopener noreferrer"
            className="hover:text-[var(--accent)] transition-colors duration-150 underline underline-offset-2"
          >
            krizaka
          </a>
        </p>
      </section>

      {/* ── Right: Form panel ──────────────────────────────────────── */}
      <section className="login-form-panel ambient-grid">
        {children}

        {/* Discreet footer: legal links + language switcher */}
        <footer className="auth-legal-footer" aria-label={t.auth.langSwitchLabel}>
          <Link href="/privacy" className="auth-legal-link">
            {t.auth.legalPrivacy}
          </Link>
          <Link href="/terms" className="auth-legal-link">
            {t.auth.legalTerms}
          </Link>
          <Link href="/contact" className="auth-legal-link">
            {t.auth.legalContact}
          </Link>

          <span className="auth-footer-separator" aria-hidden="true" />

          <nav className="auth-lang-switcher-inline" aria-label={t.auth.langSwitchLabel}>
            {LOCALES.map(({ code, label }, i) => (
              <React.Fragment key={code}>
                {i > 0 && <span className="auth-lang-divider" aria-hidden="true">/</span>}
                <button
                  type="button"
                  className="auth-lang-link"
                  data-active={locale === code}
                  onClick={() => setLocale(code)}
                  aria-current={locale === code ? "true" : undefined}
                >
                  {label}
                </button>
              </React.Fragment>
            ))}
          </nav>
        </footer>
      </section>
    </main>
  );
}
