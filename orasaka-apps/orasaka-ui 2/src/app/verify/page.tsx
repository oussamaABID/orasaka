"use client";

/**
 * @file verify/page.tsx
 * @description Account activation and verification page.
 * Processes the verification token via the BFF GraphQL proxy and renders corresponding status screens.
 */

import * as React from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { Suspense } from "react";
import { Button } from "@/components/ui/Button";
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
  CardFooter,
} from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { CheckCircle2, XCircle, Loader2, ArrowRight } from "lucide-react";
import { useTranslation } from "@/core/context/LocaleContext";
import { useVerifyEmail } from "@/features/auth/hooks/useVerifyEmail";

function VerifyPageContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const { t } = useTranslation();
  const tokenParam = searchParams.get("token");

  const [token, setToken] = React.useState("");
  const { verify, status, errorMsg } = useVerifyEmail();

  // If token parameter exists in URL, trigger verification automatically
  React.useEffect(() => {
    if (tokenParam) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setToken(tokenParam);
      verify(tokenParam);
    }
  }, [tokenParam, verify]);

  const handleSubmit = (e: React.SubmitEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (token.trim()) {
      verify(token);
    }
  };

  return (
    <div className="w-full max-w-md">
      {status === "loading" && (
        <Card className="">
          <CardContent className="flex flex-col items-center justify-center pt-12 pb-12 space-y-4">
            <Loader2 className="w-12 h-12 animate-spin text-emerald-500" />
            <p className="text-zinc-600 dark:text-zinc-300 font-medium">
              {t.verify.loadingMessage}
            </p>
          </CardContent>
        </Card>
      )}

      {status === "success" && (
        <Card className="border-emerald-500/20 bg-emerald-500/5 shadow-2xl dark:border-emerald-500/25 dark:bg-emerald-950/20 backdrop-blur-xl">
          <CardHeader className="text-center">
            <CheckCircle2 className="w-12 h-12 text-emerald-500 mx-auto mb-4" />
            <CardTitle className="text-2xl font-bold tracking-tight text-emerald-600 dark:text-emerald-400">
              {t.verify.successTitle}
            </CardTitle>
            <CardDescription className="text-zinc-600 dark:text-zinc-400">
              {t.verify.successDescription}
            </CardDescription>
          </CardHeader>
          <CardFooter>
            <Button
              onClick={() => router.push("/login")}
              className="w-full bg-emerald-600 hover:bg-emerald-500 text-white flex items-center justify-center space-x-2"
            >
              <span>{t.verify.goToLogin}</span>
              <ArrowRight className="w-4 h-4" />
            </Button>
          </CardFooter>
        </Card>
      )}

      {status === "error" && (
        <Card className="border-red-500/20 bg-red-500/5 shadow-2xl dark:border-red-500/25 dark:bg-red-950/20 backdrop-blur-xl">
          <CardHeader className="text-center">
            <XCircle className="w-12 h-12 text-red-500 mx-auto mb-4" />
            <CardTitle className="text-2xl font-bold tracking-tight text-red-600 dark:text-red-400">
              {t.verify.errorTitle}
            </CardTitle>
            <CardDescription className="text-zinc-600 dark:text-zinc-400">
              {errorMsg}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-700 dark:text-zinc-300">
                  {t.verify.enterTokenManually}
                </label>
                <Input
                  type="text"
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                  placeholder={t.verify.tokenPlaceholder}
                  className="w-full bg-card-bg/50 backdrop-blur-sm"
                />
              </div>
              <Button
                type="submit"
                className="w-full bg-zinc-900 text-zinc-50 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-200"
              >
                {t.verify.retryVerification}
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      {status === "idle" && (
        <Card className="">
          <CardHeader className="text-center">
            <CardTitle className="text-2xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
              {t.verify.idleTitle}
            </CardTitle>
            <CardDescription className="text-zinc-500 dark:text-zinc-400">
              {t.verify.idleDescription}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-700 dark:text-zinc-300">
                  {t.verify.tokenLabel}
                </label>
                <Input
                  type="text"
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                  placeholder={t.verify.tokenPlaceholder}
                  className="w-full bg-card-bg/50 backdrop-blur-sm"
                  required
                />
              </div>
              <Button
                type="submit"
                className="w-full bg-emerald-600 hover:bg-emerald-500 text-white"
              >
                {t.verify.activateAccount}
              </Button>
            </form>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

/**
 * VerifyPage component that wraps VerifyPageContent in a Suspense boundary.
 *
 * @returns The Suspense wrapped account verification page.
 */
export default function VerifyPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center bg-background">
          <Loader2 className="w-8 h-8 animate-spin text-emerald-500" />
        </div>
      }
    >
      <main className="flex min-h-screen flex-col items-center justify-center p-6 bg-gradient-to-tr from-zinc-50 via-zinc-100 to-zinc-200 dark:from-zinc-950 dark:via-zinc-900 dark:to-zinc-950 transition-colors duration-300">
        <VerifyPageContent />
      </main>
    </Suspense>
  );
}
