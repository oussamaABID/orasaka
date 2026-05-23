"use client";

import * as React from "react";
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
import { Button } from "@/components/ui/Button";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { RegisterForm } from "@/features/auth/components/RegisterForm";

/**
 * RegisterPage component that handles user registration layout and OAuth options.
 * Wraps the credentials RegisterForm inside a Card structure.
 *
 * @returns The user registration interface.
 */
export default function RegisterPage() {
  const router = useRouter();
  const { isAuthenticated, loginWithGithub, loginWithGoogle } = useAuth();

  const [error, setError] = React.useState("");
  const [success, setSuccess] = React.useState("");

  React.useEffect(() => {
    if (isAuthenticated) router.push("/");
  }, [isAuthenticated, router]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-zinc-50 dark:bg-zinc-950 p-4">
      <Card className="w-full max-w-md">
        {/* Header */}
        <CardHeader className="space-y-1 text-center pb-2">
          <CardTitle className="text-2xl font-bold tracking-tight">
            Orasaka
          </CardTitle>
          <CardDescription className="text-zinc-500 dark:text-zinc-400">
            Create your Orasaka account
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-4">
          {/* Feedback banners */}
          {error && (
            <div
              role="alert"
              className="rounded-md bg-red-50 border border-red-200 px-3 py-2 text-sm text-red-700 dark:bg-red-950/30 dark:border-red-800 dark:text-red-400"
            >
              {error}
            </div>
          )}
          {success && (
            <div
              role="status"
              className="rounded-md bg-green-50 border border-green-200 px-3 py-2 text-sm text-green-700 dark:bg-green-950/30 dark:border-green-800 dark:text-green-400"
            >
              {success}
            </div>
          )}

          {/* Credentials registration form */}
          <RegisterForm onSuccess={setSuccess} onError={setError} />

          {/* OAuth divider */}
          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <span className="w-full border-t border-zinc-200 dark:border-zinc-800" />
            </div>
            <div className="relative flex justify-center text-xs uppercase">
              <span className="bg-white px-2 text-zinc-400 dark:bg-zinc-950">
                Or continue with
              </span>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Button id="btn-github" variant="outline" onClick={loginWithGithub}>
              GitHub
            </Button>
            <Button id="btn-google" variant="outline" onClick={loginWithGoogle}>
              Google
            </Button>
          </div>
        </CardContent>

        <CardFooter className="justify-center pb-4">
          <p className="text-xs text-zinc-400">
            Already have an account?{" "}
            <Link
              href="/login"
              className="font-medium text-zinc-700 underline underline-offset-2 hover:text-zinc-900 dark:text-zinc-300 dark:hover:text-zinc-100"
            >
              Sign In
            </Link>
          </p>
        </CardFooter>
      </Card>
    </div>
  );
}
