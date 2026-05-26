"use client";

import * as React from "react";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { useTranslation } from "@/core/context/LocaleContext";

const labelClass = "text-sm font-medium text-foreground opacity-80";

interface CredentialItem {
  providerName: string;
  configured: boolean;
}

interface CredentialsSectionProps {
  fetchHeaders: () => Promise<Record<string, string>>;
}

/**
 * CredentialsSection manages the commercial AI provider API key lifecycle.
 *
 * Handles save, display masking, and deletion of per-provider credentials
 * (Gemini, Anthropic, OpenAI) through the BFF proxy endpoint.
 *
 * @param props.fetchHeaders - Async function returning authorization headers.
 */
export function CredentialsSection({ fetchHeaders }: CredentialsSectionProps) {
  const { t } = useTranslation();
  const [geminiKey, setGeminiKey] = React.useState("");
  const [anthropicKey, setAnthropicKey] = React.useState("");
  const [openaiKey, setOpenaiKey] = React.useState("");
  const [isSaving, setIsSaving] = React.useState(false);
  const [message, setMessage] = React.useState<string | null>(null);

  React.useEffect(() => {
    const loadCredentials = async () => {
      try {
        const headers = await fetchHeaders();
        const res = await fetch("/api/v1/user/credentials", { headers });
        if (res.ok) {
          const data: CredentialItem[] = await res.json();
          if (data.find((c) => c.providerName === "gemini")?.configured)
            setGeminiKey("••••••••••••••••");
          if (data.find((c) => c.providerName === "anthropic")?.configured)
            setAnthropicKey("••••••••••••••••");
          if (data.find((c) => c.providerName === "openai")?.configured)
            setOpenaiKey("••••••••••••••••");
        }
      } catch (err) {
        console.error("Failed to load credentials:", err);
      }
    };
    loadCredentials();
  }, [fetchHeaders]);

  const reloadCredentials = async () => {
    try {
      const headers = await fetchHeaders();
      const res = await fetch("/api/v1/user/credentials", { headers });
      if (res.ok) {
        const data: CredentialItem[] = await res.json();
        if (data.find((c) => c.providerName === "gemini")?.configured)
          setGeminiKey("••••••••••••••••");
        if (data.find((c) => c.providerName === "anthropic")?.configured)
          setAnthropicKey("••••••••••••••••");
        if (data.find((c) => c.providerName === "openai")?.configured)
          setOpenaiKey("••••••••••••••••");
      }
    } catch (err) {
      console.error("Failed to reload credentials:", err);
    }
  };

  const handleSave = async (providerName: string, apiKey: string) => {
    if (!apiKey || apiKey === "••••••••••••••••") return;
    setIsSaving(true);
    setMessage(null);
    try {
      const headers = await fetchHeaders();
      const res = await fetch("/api/v1/user/credentials", {
        method: "POST",
        headers,
        body: JSON.stringify({ providerName, apiKey }),
      });
      if (res.ok) {
        setMessage(t.settings.credentialsSaved);
        reloadCredentials();
      } else {
        throw new Error();
      }
    } catch {
      setMessage(t.errors.generic);
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async (providerName: string) => {
    setIsSaving(true);
    setMessage(null);
    try {
      const headers = await fetchHeaders();
      const res = await fetch(`/api/v1/user/credentials/${providerName}`, {
        method: "DELETE",
        headers,
      });
      if (res.ok) {
        if (providerName === "gemini") setGeminiKey("");
        else if (providerName === "anthropic") setAnthropicKey("");
        else if (providerName === "openai") setOpenaiKey("");
        setMessage(t.settings.credentialsSaved);
        reloadCredentials();
      } else {
        throw new Error();
      }
    } catch {
      setMessage(t.errors.generic);
    } finally {
      setIsSaving(false);
    }
  };

  const providers = [
    {
      name: "gemini",
      label: t.settings.geminiKey,
      value: geminiKey,
      set: setGeminiKey,
    },
    {
      name: "anthropic",
      label: t.settings.anthropicKey,
      value: anthropicKey,
      set: setAnthropicKey,
    },
    {
      name: "openai",
      label: t.settings.openaiKey,
      value: openaiKey,
      set: setOpenaiKey,
    },
  ] as const;

  return (
    <section className="space-y-4">
      <h3 className="text-sm font-semibold text-foreground">
        {t.settings.credentialsTitle}
      </h3>
      <p className="text-xs text-foreground opacity-70">
        {t.settings.credentialsDesc}
      </p>
      {message && (
        <div className="p-3 bg-emerald-50 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-900/40 rounded-xl text-xs font-semibold text-emerald-700 dark:text-emerald-400">
          {message}
        </div>
      )}
      <section className="space-y-4">
        {providers.map((p) => (
          <article key={p.name} className="flex flex-col gap-2">
            <label className={labelClass}>{p.label}</label>
            <section className="flex gap-2">
              <Input
                type="password"
                value={p.value}
                onChange={(e) => p.set(e.target.value)}
                placeholder={t.settings.credentialsPlaceholder}
                className="flex-1 bg-input-bg border-input-border text-input-text"
              />
              {p.value && p.value === "••••••••••••••••" ? (
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => handleDelete(p.name)}
                  disabled={isSaving}
                  className="text-red-500 border-red-500/20 hover:bg-red-50 dark:hover:bg-red-950/20"
                >
                  Clear
                </Button>
              ) : (
                <Button
                  type="button"
                  onClick={() => handleSave(p.name, p.value)}
                  disabled={isSaving || !p.value}
                >
                  Save
                </Button>
              )}
            </section>
          </article>
        ))}
      </section>
    </section>
  );
}
