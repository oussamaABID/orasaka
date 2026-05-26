import * as React from "react";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { useRegister } from "@/features/auth/hooks/useRegister";
import { useTranslation } from "@/core/context/LocaleContext";

// ─── Supported UI languages ────────────────────────────────────────────────
const SUPPORTED_LANGUAGES = [
  { code: "en", label: "English" },
  { code: "fr", label: "Français" },
] as const;

// ─── Styles ────────────────────────────────────────────────────────
const fieldLabelClass =
  "text-sm font-medium leading-none text-zinc-700 dark:text-zinc-300 peer-disabled:cursor-not-allowed peer-disabled:opacity-70";

const selectClass =
  "w-full rounded-xl border border-input-border bg-input-bg px-3 py-2 text-sm " +
  "text-input-text shadow-sm backdrop-blur-sm transition-all duration-200 " +
  "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-300 " +
  "dark:focus-visible:ring-zinc-800 focus-visible:border-transparent " +
  "disabled:cursor-not-allowed disabled:opacity-50";

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
 */
export function RegisterForm({ onSuccess, onError }: Readonly<RegisterFormProps>) {
  const [email, setEmail] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [username, setUsername] = React.useState("");
  const [language, setLanguage] = React.useState("en");
  const [confirmPassword, setConfirmPassword] = React.useState("");

  const { t } = useTranslation();
  const { register, isPending } = useRegister({ onSuccess, onError });

  const handleRegister = (e: React.SubmitEvent<HTMLFormElement>) => {
    e.preventDefault();
    onError("");
    onSuccess("");

    // Client-side guards
    if (username.trim().length < 3) {
      onError(t.auth.usernameMinLength);
      return;
    }
    if (password.length < 8) {
      onError(t.auth.passwordMinLength);
      return;
    }
    if (password !== confirmPassword) {
      onError(t.auth.passwordsDoNotMatch);
      return;
    }

    register({
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
          {t.auth.usernameLabel} <span className="text-red-500">*</span>
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
          {t.auth.emailLabel} <span className="text-red-500">*</span>
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
          {t.auth.passwordLabel} <span className="text-red-500">*</span>
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
          {t.auth.confirmPasswordLabel} <span className="text-red-500">*</span>
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
          {t.auth.preferredLanguageLabel}
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
        disabled={isPending}
      >
        {isPending ? t.auth.creatingAccountBtn : t.auth.createAccountBtn}
      </Button>
      <p className="text-center text-xs text-zinc-500 dark:text-zinc-400">
        {t.auth.termsNotice}
      </p>
    </form>
  );
}
