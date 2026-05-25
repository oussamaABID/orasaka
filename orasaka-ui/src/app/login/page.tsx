"use client";

import * as React from "react";
import { signIn } from "next-auth/react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { useAuth } from "@/features/auth/hooks/useAuth";

// ─── Reusable field wrapper ─────────────────────────────────────────────────
const fieldLabelClass =
  "text-sm font-medium leading-none text-zinc-700 dark:text-zinc-300 peer-disabled:cursor-not-allowed peer-disabled:opacity-70";

// ─── Component ─────────────────────────────────────────────────────────────
/**
 * LoginPage component that handles user login.
 *
 * @returns The user login interface.
 */
export default function LoginPage() {
  const router = useRouter();
  const { isAuthenticated, loginWithGithub, loginWithGoogle } = useAuth();

  // Form fields
  const [email, setEmail] = React.useState("");
  const [password, setPassword] = React.useState("");

  // UI state
  const [error, setError] = React.useState("");
  const [isLoading, setIsLoading] = React.useState(false);

  React.useEffect(() => {
    if (isAuthenticated) router.push("/");
  }, [isAuthenticated, router]);

  // ── Login ──────────────────────────────────────────────────────────────
  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError("");

    try {
      const res = await signIn("credentials", {
        email,
        password,
        redirect: false,
      });

      if (res?.error) {
        setError("Invalid email or password. Please check your credentials.");
      } else if (res?.ok) {
        router.push("/");
      }
    } catch {
      setError("An unexpected error occurred. Please try again.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-zinc-50 dark:bg-zinc-950 p-4">
      <Card className="w-full max-w-md">
        {/* ── Header ─────────────────────────────────────────────────── */}
        <CardHeader className="space-y-1 text-center pb-2">
          <CardTitle className="text-2xl font-bold tracking-tight">
            Orasaka
          </CardTitle>
          <CardDescription className="text-zinc-500 dark:text-zinc-400">
            Sign in to your workspace
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-4">
          {/* ── Feedback banners ─────────────────────────────────────── */}
          {error && (
            <div
              role="alert"
              className="rounded-md bg-red-50 border border-red-200 px-3 py-2 text-sm text-red-700 dark:bg-red-950/30 dark:border-red-800 dark:text-red-400"
            >
              {error}
            </div>
          )}

          {/* ── LOGIN form ───────────────────────────────────────────── */}
          <form id="form-login" onSubmit={handleLogin} className="space-y-4">
            <div className="space-y-2">
              <label htmlFor="login-email" className={fieldLabelClass}>
                Email
              </label>
              <Input
                id="login-email"
                type="email"
                placeholder="you@orasaka.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
              />
            </div>
            <div className="space-y-2">
              <label htmlFor="login-password" className={fieldLabelClass}>
                Password
              </label>
              <Input
                id="login-password"
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="current-password"
              />
            </div>
            <Button
              id="btn-login-submit"
              type="submit"
              className="w-full"
              disabled={isLoading}
            >
              {isLoading ? "Signing in…" : "Sign In"}
            </Button>
          </form>

          {/* ── OAuth divider ─────────────────────────────────────────── */}
          <div
            className="relative"
            role="separator"
            aria-orientation="horizontal"
          >
            <hr className="border-t border-zinc-200 dark:border-zinc-800" />
            <span className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 bg-white px-2 text-xs uppercase text-zinc-400 dark:bg-zinc-950">
              Or continue with
            </span>
          </div>

          <nav
            className="grid grid-cols-2 gap-4"
            aria-label="Social login options"
          >
            <Button id="btn-github" variant="outline" onClick={loginWithGithub}>
              GitHub
            </Button>
            <Button id="btn-google" variant="outline" onClick={loginWithGoogle}>
              Google
            </Button>
          </nav>
        </CardContent>

        <CardFooter className="justify-center pb-4">
          <p className="text-xs text-zinc-400">
            No account?{" "}
            <Link
              href="/register"
              className="font-medium text-zinc-700 underline underline-offset-2 hover:text-zinc-900 dark:text-zinc-300 dark:hover:text-zinc-100"
            >
              Register
            </Link>
          </p>
        </CardFooter>
      </Card>
    </main>
  );
}
