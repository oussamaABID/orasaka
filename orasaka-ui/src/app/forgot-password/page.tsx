"use client";

import * as React from "react";
import Link from "next/link";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useTranslation } from "@/core/context/LocaleContext";
import { useRouter } from "next/navigation";
import { Cpu, Shield, Layers, Plug, ArrowLeft, Mail, Loader2 } from "lucide-react";

/** Feature bullets for the marketing hero. */
const HERO_FEATURES = [
  { icon: Shield, key: "heroFeature1" as const },
  { icon: Layers, key: "heroFeature2" as const },
  { icon: Plug, key: "heroFeature3" as const },
];

/**
 * ForgotPasswordPage — split layout with marketing hero (left) and
 * email reset form (right). Calls POST /api/v1/auth/forgot.
 */
export default function ForgotPasswordPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuth();
  const { t } = useTranslation();

  const [email, setEmail] = React.useState("");
  const [isSubmitting, setIsSubmitting] = React.useState(false);
  const [error, setError] = React.useState("");
  const [success, setSuccess] = React.useState(false);

  React.useEffect(() => {
    if (isAuthenticated) router.push("/");
  }, [isAuthenticated, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim()) return;
    setIsSubmitting(true);
    setError("");
    setSuccess(false);

    try {
      const res = await fetch("/api/v1/auth/forgot", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
      });
      if (res.ok) {
        setSuccess(true);
      } else {
        const data = await res.text();
        setError(data || t.auth.forgotError || "Failed to send reset email.");
      }
    } catch {
      setError(t.auth.forgotError || "An unexpected error occurred.");
    } finally {
      setIsSubmitting(false);
    }
  };

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

      {/* ── Right: Forgot password form ─────────────────────────── */}
      <section className="login-form-panel">
        <Card className="w-full max-w-md p-0">
          <CardHeader className="space-y-1 text-center pb-2">
            <CardTitle className="text-2xl font-bold tracking-tight">
              {t.auth.forgotTitle || "Reset Your Password"}
            </CardTitle>
            <CardDescription className="text-[var(--text-secondary)]">
              {t.auth.forgotSubtitle ||
                "Enter your email and we'll send you a reset link."}
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4">
            {error && (
              <div
                role="alert"
                className="rounded-lg bg-red-500/5 border border-red-500/10 px-3 py-2 text-sm text-red-600 dark:text-red-400"
              >
                {error}
              </div>
            )}

            {success ? (
              <div
                role="status"
                className="rounded-lg bg-emerald-500/5 border border-emerald-500/10 px-4 py-4 space-y-2"
              >
                <p className="text-sm font-medium text-emerald-600 dark:text-emerald-400">
                  {t.auth.forgotSuccess ||
                    "Check your email for a password reset link."}
                </p>
                <p className="text-xs text-[var(--text-secondary)]">
                  {t.auth.forgotSuccessDetail ||
                    "If you don't see it, check your spam folder."}
                </p>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-4">
                <div className="space-y-2">
                  <label
                    htmlFor="forgot-email"
                    className="text-sm font-medium text-[var(--text-primary)]"
                  >
                    {t.auth.emailLabel || "Email"}
                  </label>
                  <div className="relative">
                    <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[var(--text-muted)]" />
                    <input
                      id="forgot-email"
                      type="email"
                      required
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="you@example.com"
                      className="w-full pl-10 pr-4 py-2.5 rounded-[var(--radius-md)] bg-[var(--surface-2)] border border-[var(--border-default)] text-sm text-[var(--text-primary)] placeholder:text-[var(--text-muted)] focus:border-[var(--accent)] focus:ring-2 focus:ring-[var(--accent-soft)] transition-all duration-200"
                    />
                  </div>
                </div>
                <Button
                  id="btn-forgot-submit"
                  type="submit"
                  disabled={isSubmitting || !email.trim()}
                  className="w-full h-10 rounded-[var(--radius-md)] font-medium text-sm bg-[var(--accent)] hover:bg-[var(--accent-hover)] text-white transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isSubmitting ? (
                    <Loader2 className="h-4 w-4 animate-spin mr-2" />
                  ) : null}
                  {t.auth.forgotSubmit || "Send Reset Link"}
                </Button>
              </form>
            )}
          </CardContent>

          <CardFooter className="justify-center pb-4">
            <Link
              href="/login"
              className="inline-flex items-center gap-1.5 text-xs font-medium text-[var(--text-muted)] hover:text-[var(--accent)] transition-colors duration-150"
            >
              <ArrowLeft className="h-3 w-3" />
              {t.auth.backToLogin || "Back to Login"}
            </Link>
          </CardFooter>
        </Card>
      </section>
    </main>
  );
}
