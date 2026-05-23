import * as React from "react";
import { useMutation } from "@tanstack/react-query";
import { signIn } from "next-auth/react";
import { useRouter } from "next/navigation";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";

// ─── Supported UI languages ────────────────────────────────────────────────
const SUPPORTED_LANGUAGES = [
  { code: "en", label: "English" },
  { code: "fr", label: "Français" },
] as const;

// ─── Styles ────────────────────────────────────────────────────────
const fieldLabelClass =
  "text-sm font-medium leading-none text-zinc-700 dark:text-zinc-300 peer-disabled:cursor-not-allowed peer-disabled:opacity-70";

const selectClass =
  "w-full rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm " +
  "text-zinc-900 shadow-sm ring-offset-white transition-colors " +
  "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-950 " +
  "dark:border-zinc-800 dark:bg-zinc-950 dark:text-zinc-50 dark:ring-offset-zinc-950 " +
  "dark:focus-visible:ring-zinc-300";

/**
 * Properties required by the {@link RegisterForm} component.
 */
interface RegisterFormProps {
  /** Callback triggered when registration succeeds. */
  onSuccess: (msg: string) => void;
  /** Callback triggered when registration fails. */
  onError: (msg: string) => void;
}

/**
 * RegisterForm component renders the user registration credentials inputs,
 * validates values on the client side, and submits to the Next.js BFF endpoint.
 *
 * @param props - Component React properties.
 * @param props.onSuccess - Callback triggered when registration succeeds.
 * @param props.onError - Callback triggered when registration fails.
 * @returns The React functional component representing the registration form.
 */
export function RegisterForm({ onSuccess, onError }: RegisterFormProps) {
  const router = useRouter();

  const [email, setEmail] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [username, setUsername] = React.useState("");
  const [language, setLanguage] = React.useState("en");
  const [confirmPassword, setConfirmPassword] = React.useState("");

  const registerMutation = useMutation<
    unknown,
    { status: number; error?: string },
    Record<string, string>
  >({
    mutationFn: async (payload: Record<string, string>) => {
      const res = await fetch("/api/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      const data = await res.json();
      if (!res.ok) {
        throw { status: res.status, error: data.error };
      }
      return data;
    },
    onSuccess: async (_, variables) => {
      onSuccess("Account created! Signing you in…");
      // Auto-login after successful registration
      const signInRes = await signIn("credentials", {
        email: variables.email,
        password: variables.password,
        redirect: false,
      });
      if (signInRes?.ok) {
        router.push("/");
      } else {
        // Fallback — redirect to login so user can sign in manually
        onSuccess("Account created! Redirecting to login…");
        setTimeout(() => router.push("/login"), 1500);
      }
    },
    onError: (err) => {
      if (err.status === 409) {
        onError(err.error || "An account with this email already exists.");
      } else if (err.error) {
        onError(err.error);
      } else {
        onError("Registration failed. Please try again.");
      }
    },
  });

  const handleRegister = (e: React.FormEvent) => {
    e.preventDefault();
    onError("");
    onSuccess("");

    // Client-side guards
    if (username.trim().length < 3) {
      onError("Username must be at least 3 characters.");
      return;
    }
    if (password.length < 8) {
      onError("Password must be at least 8 characters.");
      return;
    }
    if (password !== confirmPassword) {
      onError("Passwords do not match.");
      return;
    }

    registerMutation.mutate({
      username: username.trim(),
      email,
      password,
      language,
    });
  };

  return (
    <form id="form-register" onSubmit={handleRegister} className="space-y-4">
      <div className="space-y-2">
        <label htmlFor="reg-username" className={fieldLabelClass}>
          Username <span className="text-red-500">*</span>
        </label>
        <Input
          id="reg-username"
          type="text"
          placeholder="your_name"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
          minLength={3}
          autoComplete="username"
        />
      </div>
      <div className="space-y-2">
        <label htmlFor="reg-email" className={fieldLabelClass}>
          Email <span className="text-red-500">*</span>
        </label>
        <Input
          id="reg-email"
          type="email"
          placeholder="you@orasaka.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          autoComplete="email"
        />
      </div>
      <div className="space-y-2">
        <label htmlFor="reg-password" className={fieldLabelClass}>
          Password <span className="text-red-500">*</span>
        </label>
        <Input
          id="reg-password"
          type="password"
          placeholder="At least 8 characters"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          minLength={8}
          autoComplete="new-password"
        />
      </div>
      <div className="space-y-2">
        <label htmlFor="reg-confirm-password" className={fieldLabelClass}>
          Confirm Password <span className="text-red-500">*</span>
        </label>
        <Input
          id="reg-confirm-password"
          type="password"
          placeholder="Repeat your password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          required
          autoComplete="new-password"
        />
      </div>
      <div className="space-y-2">
        <label htmlFor="reg-language" className={fieldLabelClass}>
          Preferred Language
        </label>
        <select
          id="reg-language"
          value={language}
          onChange={(e) => setLanguage(e.target.value)}
          className={selectClass}
        >
          {SUPPORTED_LANGUAGES.map((lang) => (
            <option key={lang.code} value={lang.code}>
              {lang.label}
            </option>
          ))}
        </select>
      </div>
      <Button
        id="btn-register-submit"
        type="submit"
        className="w-full"
        disabled={registerMutation.isPending}
      >
        {registerMutation.isPending ? "Creating account…" : "Create Account"}
      </Button>
      <p className="text-center text-xs text-zinc-500 dark:text-zinc-400">
        By registering, you accept the platform terms of use.
      </p>
    </form>
  );
}
