"use client";

/**
 * @file verify/page.tsx
 * @description Account activation and verification page.
 * Processes the verification token via the BFF GraphQL proxy and renders corresponding status screens.
 */

import * as React from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { Suspense } from "react";
import { useMutation } from "@tanstack/react-query";
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

function VerifyPageContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const { locale } = useTranslation();
  const tokenParam = searchParams.get("token");

  const [token, setToken] = React.useState("");
  const [errorMsg, setErrorMsg] = React.useState<string | null>(null);

  // TanStack Mutation for verifying token via BFF GraphQL endpoint
  const verifyMutation = useMutation({
    mutationFn: async (tokenToVerify: string) => {
      const response = await fetch("/api/graphql", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          query: `
            mutation VerifyEmail($token: String!) {
              verifyEmail(token: $token)
            }
          `,
          variables: { token: tokenToVerify.trim() },
        }),
      });

      if (!response.ok) {
        throw new Error(
          locale === "fr"
            ? "Le jeton est invalide ou expiré."
            : "Invalid or expired verification token.",
        );
      }

      const resBody = await response.json();
      if (resBody.errors && resBody.errors.length > 0) {
        throw new Error(resBody.errors[0].message);
      }

      if (!resBody.data?.verifyEmail) {
        throw new Error(
          locale === "fr"
            ? "Le jeton est invalide ou expiré."
            : "Invalid or expired verification token.",
        );
      }

      return resBody.data.verifyEmail;
    },
    onSuccess: () => {
      setErrorMsg(null);
    },
    onError: (err: Error) => {
      setErrorMsg(err.message || "An error occurred.");
    },
  });

  // If token parameter exists in URL, trigger verification automatically
  React.useEffect(() => {
    if (tokenParam) {
      Promise.resolve().then(() => {
        setToken(tokenParam);
        verifyMutation.mutate(tokenParam);
      });
    }
  }, [tokenParam, verifyMutation]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (token.trim()) {
      verifyMutation.mutate(token);
    }
  };

  // Resolve status state from mutation flags
  const status = verifyMutation.isPending
    ? "loading"
    : verifyMutation.isSuccess
      ? "success"
      : verifyMutation.isError
        ? "error"
        : "idle";

  return (
    <div className="w-full max-w-md">
      {status === "loading" && (
        <Card className="border-zinc-200/80 bg-white/70 shadow-2xl dark:border-zinc-800/60 dark:bg-zinc-950/60 backdrop-blur-xl">
          <CardContent className="flex flex-col items-center justify-center pt-12 pb-12 space-y-4">
            <Loader2 className="w-12 h-12 animate-spin text-emerald-500" />
            <p className="text-zinc-600 dark:text-zinc-300 font-medium">
              {locale === "fr"
                ? "Vérification de votre compte..."
                : "Verifying your account..."}
            </p>
          </CardContent>
        </Card>
      )}

      {status === "success" && (
        <Card className="border-emerald-500/20 bg-emerald-500/5 shadow-2xl dark:border-emerald-500/25 dark:bg-emerald-950/20 backdrop-blur-xl">
          <CardHeader className="text-center">
            <CheckCircle2 className="w-12 h-12 text-emerald-500 mx-auto mb-4" />
            <CardTitle className="text-2xl font-bold tracking-tight text-emerald-600 dark:text-emerald-400">
              {locale === "fr" ? "Compte Activé !" : "Account Verified!"}
            </CardTitle>
            <CardDescription className="text-zinc-600 dark:text-zinc-400">
              {locale === "fr"
                ? "Votre compte a été vérifié avec succès. Vous pouvez maintenant vous connecter."
                : "Your account has been successfully verified. You can now log in."}
            </CardDescription>
          </CardHeader>
          <CardFooter>
            <Button
              onClick={() => router.push("/login")}
              className="w-full bg-emerald-600 hover:bg-emerald-500 text-white flex items-center justify-center space-x-2"
            >
              <span>{locale === "fr" ? "Se Connecter" : "Go to Login"}</span>
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
              {locale === "fr"
                ? "Échec de la Vérification"
                : "Verification Failed"}
            </CardTitle>
            <CardDescription className="text-zinc-600 dark:text-zinc-400">
              {errorMsg}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-700 dark:text-zinc-300">
                  {locale === "fr"
                    ? "Entrez le jeton manuellement :"
                    : "Enter verification token manually:"}
                </label>
                <Input
                  type="text"
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                  placeholder="e.g. your-activation-token"
                  className="w-full bg-white/50 dark:bg-zinc-900/30"
                />
              </div>
              <Button
                type="submit"
                className="w-full bg-zinc-900 text-zinc-50 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-200"
              >
                {locale === "fr" ? "Essayer à nouveau" : "Retry Verification"}
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      {status === "idle" && (
        <Card className="border-zinc-200/80 bg-white/70 shadow-2xl dark:border-zinc-800/60 dark:bg-zinc-950/60 backdrop-blur-xl">
          <CardHeader className="text-center">
            <CardTitle className="text-2xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
              {locale === "fr" ? "Activation du Compte" : "Account Activation"}
            </CardTitle>
            <CardDescription className="text-zinc-500 dark:text-zinc-400">
              {locale === "fr"
                ? "Veuillez saisir le jeton reçu par email pour activer votre compte."
                : "Please enter the verification token received via email to activate your account."}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <label className="text-sm font-medium text-zinc-700 dark:text-zinc-300">
                  {locale === "fr"
                    ? "Jeton de vérification"
                    : "Verification Token"}
                </label>
                <Input
                  type="text"
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                  placeholder="e.g. your-activation-token"
                  className="w-full bg-white/50 dark:bg-zinc-900/30"
                  required
                />
              </div>
              <Button
                type="submit"
                className="w-full bg-emerald-600 hover:bg-emerald-500 text-white"
              >
                {locale === "fr" ? "Activer le Compte" : "Activate Account"}
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
        <div className="flex min-h-screen items-center justify-center bg-zinc-50 dark:bg-zinc-950">
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
