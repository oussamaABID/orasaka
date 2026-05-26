"use client";

import React from "react";
import { Cpu, Shield, Layers, Plug } from "lucide-react";
import { useTranslation } from "@/core/context/LocaleContext";

/** Feature bullet metadata for the marketing hero */
const HERO_FEATURES = [
  { icon: Shield, key: "heroFeature1" as const },
  { icon: Layers, key: "heroFeature2" as const },
  { icon: Plug, key: "heroFeature3" as const },
];

interface AuthLayoutProps {
  children: React.ReactNode;
}

/**
 * Shared split-screen layout for authentication pages (login, register, forgot-password).
 * Left panel: marketing hero with branding and feature bullets.
 * Right panel: form content provided via children.
 */
export function AuthLayout({ children }: Readonly<AuthLayoutProps>) {
  const { t } = useTranslation();

  return (
    <main className="login-split">
      {/* ── Left: Marketing hero ───────────────────────────────────── */}
      <section
        className="login-hero gradient-mesh"
        aria-label="Product overview"
      >
        <div className="login-hero-content">
          <div className="login-hero-brand">
            <Cpu className="h-8 w-8 text-[var(--accent)]" />
            <span className="text-xl font-bold tracking-tight text-[var(--text-primary)]">
              Orasaka
            </span>
          </div>

          <h1 className="login-hero-headline">{t.auth.heroHeadline}</h1>

          <p className="text-base text-[var(--text-secondary)] leading-relaxed max-w-md">
            {t.auth.heroTagline}
          </p>

          <ul className="login-hero-features">
            {HERO_FEATURES.map(({ icon: Icon, key }) => (
              <li key={key} className="login-hero-feature">
                <span className="login-hero-feature-icon">
                  <Icon className="h-4 w-4" />
                </span>
                <span className="text-sm text-[var(--text-secondary)]">
                  {t.auth[key]}
                </span>
              </li>
            ))}
          </ul>
        </div>

        <p className="text-xs text-[var(--text-muted)] mt-auto">
          © {new Date().getFullYear()} Orasaka · MIT License
        </p>
      </section>

      {/* ── Right: Form panel ──────────────────────────────────────── */}
      <section className="login-form-panel">{children}</section>
    </main>
  );
}
