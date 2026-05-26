/* eslint-disable */
"use client";

import * as React from "react";
import Link from "next/link";
import { Icon } from "@/components/ui/icon";
import { useTranslation } from "@/core/context/LocaleContext";
import type { Locale } from "@/core/context/translations.types";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { useToast } from "@/core/context/ToastContext";

const LOCALES: { code: Locale; label: string }[] = [
  { code: "en", label: "EN" },
  { code: "fr", label: "FR" },
];

const CONTENT = {
  en: {
    title: "Get in Touch",
    subtitle: "Have questions about local AI orchestration? We're here to help.",
    nameLabel: "Your Name",
    emailLabel: "Email Address",
    subjectLabel: "Subject",
    messageLabel: "Message",
    sendBtn: "Send Message",
    sendingBtn: "Sending message...",
    backToLogin: "Back to login",
    successMsg: "Message sent! We will get back to you shortly.",
    errorMsg: "Please fill in all fields correctly.",
  },
  fr: {
    title: "Contactez-nous",
    subtitle: "Des questions sur l'orchestration locale de l'IA ? Nous sommes là.",
    nameLabel: "Votre nom",
    emailLabel: "Adresse e-mail",
    subjectLabel: "Sujet",
    messageLabel: "Message",
    sendBtn: "Envoyer le message",
    sendingBtn: "Envoi en cours...",
    backToLogin: "Retour à la connexion",
    successMsg: "Message envoyé ! Nous vous répondrons sous peu.",
    errorMsg: "Veuillez remplir correctement tous les champs.",
  },
};

export default function ContactPage() {
  const { t, locale, setLocale } = useTranslation();
  const { addToast } = useToast();
  const c = CONTENT[locale] || CONTENT.en;

  const [name, setName] = React.useState("");
  const [email, setEmail] = React.useState("");
  const [subject, setSubject] = React.useState("");
  const [message, setMessage] = React.useState("");
  const [isSubmitting, setIsSubmitting] = React.useState(false);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!name || !email || !subject || !message) {
      addToast(c.errorMsg, "error");
      return;
    }

    setIsSubmitting(true);

    // Simulate sending message
    await new Promise((resolve) => setTimeout(resolve, 1500));

    setIsSubmitting(false);
    addToast(c.successMsg, "success");

    setName("");
    setEmail("");
    setSubject("");
    setMessage("");
  };

  return (
    <main className="min-h-screen flex flex-col items-center p-6 bg-[var(--surface-0)] ambient-grid w-full overflow-y-auto">
      {/* Navbar header */}
      <header className="w-full max-w-2xl flex items-center justify-between py-4 mb-6 border-b border-[var(--border-subtle)]">
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
      <article className="w-full max-w-2xl glass-card rounded-2xl p-6 md:p-10 space-y-6 animate-fade-up shadow-2xl">
        <header className="space-y-2 border-b border-[var(--border-subtle)] pb-4">
          <h1 className="text-3xl font-extrabold tracking-tight text-[var(--text-primary)]">
            {c.title}
          </h1>
          <p className="text-[var(--text-secondary)] text-sm leading-relaxed">
            {c.subtitle}
          </p>
        </header>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid gap-4 md:grid-cols-2">
            <div className="space-y-2">
              <label htmlFor="contact-name" className="text-sm font-medium text-[var(--text-secondary)]">
                {c.nameLabel}
              </label>
              <Input
                id="contact-name"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <label htmlFor="contact-email" className="text-sm font-medium text-[var(--text-secondary)]">
                {c.emailLabel}
              </label>
              <Input
                id="contact-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="space-y-2">
            <label htmlFor="contact-subject" className="text-sm font-medium text-[var(--text-secondary)]">
              {c.subjectLabel}
            </label>
            <Input
              id="contact-subject"
              type="text"
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              required
            />
          </div>

          <div className="space-y-2">
            <label htmlFor="contact-message" className="text-sm font-medium text-[var(--text-secondary)]">
              {c.messageLabel}
            </label>
            <textarea
              id="contact-message"
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              required
              rows={5}
              className="w-full rounded-lg border border-[var(--border-default)] bg-[var(--surface-2)] px-3 py-2 text-sm text-[var(--text-primary)] focus:border-[var(--accent)] focus:outline-none focus:ring-1 focus:ring-[var(--accent)] transition-all duration-150"
            />
          </div>

          <Button
            type="submit"
            className="w-full gap-2 mt-2"
            disabled={isSubmitting}
          >
            {isSubmitting && (
              <svg
                className="h-4 w-4 animate-spin-slow"
                viewBox="0 0 24 24"
                fill="none"
              >
                <circle
                  className="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  strokeWidth="4"
                />
                <path
                  className="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                />
              </svg>
            )}
            {isSubmitting ? c.sendingBtn : c.sendBtn}
          </Button>
        </form>
      </article>

      {/* Public Footer */}
      <footer className="w-full max-w-2xl flex items-center justify-between py-6 mt-6 border-t border-[var(--border-subtle)] text-xs text-[var(--text-muted)]">
        <div className="flex gap-4">
          <Link href="/privacy" className="hover:text-[var(--accent)] transition-colors">
            {t.auth.legalPrivacy}
          </Link>
          <Link href="/terms" className="hover:text-[var(--accent)] transition-colors">
            {t.auth.legalTerms}
          </Link>
          <Link href="/contact" className="hover:text-[var(--accent)] transition-colors underline">
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
