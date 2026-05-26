"use client";

import * as React from "react";
import { getSession } from "next-auth/react";
import {
  Eye,
  EyeOff,
  Check,
  AlertCircle,
  Loader2,
  Plus,
  Trash2,
} from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/Card";
import { useTenant } from "@/features/tenant/context/TenantContext";
import { useTranslation } from "@/core/context/LocaleContext";

interface ProviderConfig {
  id: string;
  name: string;
  icon: React.ReactNode;
  color: string;
  gradient: string;
  placeholder: string;
}

/* Inline SVG provider logos — small, distinctive, and themed */
const GeminiIcon = () => (
  <svg viewBox="0 0 24 24" className="w-5 h-5" fill="currentColor">
    <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" />
  </svg>
);

const ClaudeIcon = () => (
  <svg viewBox="0 0 24 24" className="w-5 h-5" fill="currentColor">
    <circle cx="12" cy="12" r="9" opacity="0.15" />
    <circle cx="12" cy="12" r="4.5" />
  </svg>
);

const OpenAIIcon = () => (
  <svg
    viewBox="0 0 24 24"
    className="w-5 h-5"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
  >
    <circle cx="12" cy="12" r="9" />
    <circle cx="12" cy="12" r="3" />
    <line x1="12" y1="3" x2="12" y2="9" />
    <line x1="12" y1="15" x2="12" y2="21" />
  </svg>
);

const MistralIcon = () => (
  <svg viewBox="0 0 24 24" className="w-5 h-5" fill="currentColor">
    <path d="M12 3L21 18H3L12 3Z" opacity="0.2" />
    <path d="M12 6L18.5 17H5.5L12 6Z" />
  </svg>
);

const GroqIcon = () => (
  <svg viewBox="0 0 24 24" className="w-5 h-5" fill="currentColor">
    <path d="M13 2L3 14h7l-1 8l10-12h-7l1-8z" />
  </svg>
);

const OllamaIcon = () => (
  <svg viewBox="0 0 24 24" className="w-5 h-5" fill="currentColor">
    <path
      d="M12 4C8 4 5 7 5 10c0 2 1 3 2 4l-1 5h12l-1-5c1-1 2-2 2-4 0-3-3-6-7-6z"
      opacity="0.2"
    />
    <circle cx="9.5" cy="10" r="1.5" />
    <circle cx="14.5" cy="10" r="1.5" />
    <path
      d="M9 14c0 0 1.5 2 3 2s3-2 3-2"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
    />
  </svg>
);

const PROVIDERS: ProviderConfig[] = [
  {
    id: "gemini",
    name: "Google Gemini",
    icon: <GeminiIcon />,
    color: "text-blue-500",
    gradient: "from-blue-500/10 to-indigo-500/10",
    placeholder: "AIza...",
  },
  {
    id: "claude",
    name: "Anthropic Claude",
    icon: <ClaudeIcon />,
    color: "text-orange-500",
    gradient: "from-orange-500/10 to-amber-500/10",
    placeholder: "sk-ant-...",
  },
  {
    id: "openai",
    name: "OpenAI",
    icon: <OpenAIIcon />,
    color: "text-emerald-500",
    gradient: "from-emerald-500/10 to-teal-500/10",
    placeholder: "sk-...",
  },
  {
    id: "mistral",
    name: "Mistral AI",
    icon: <MistralIcon />,
    color: "text-orange-400",
    gradient: "from-orange-400/10 to-red-500/10",
    placeholder: "...",
  },
  {
    id: "groq",
    name: "Groq",
    icon: <GroqIcon />,
    color: "text-purple-500",
    gradient: "from-purple-500/10 to-violet-500/10",
    placeholder: "gsk_...",
  },
  {
    id: "ollama",
    name: "Ollama (Local)",
    icon: <OllamaIcon />,
    color: "text-zinc-500",
    gradient: "from-zinc-500/10 to-slate-500/10",
    placeholder: "http://localhost:11434",
  },
];

interface ProviderState {
  key: string;
  isRevealed: boolean;
  isTesting: boolean;
  testResult: "success" | "error" | null;
  isSaved: boolean;
}

function ProviderCard({
  provider,
  state,
  onKeyChange,
  onToggleReveal,
  onTest,
  onSave,
  onDelete,
  disabled,
}: Readonly<{
  provider: ProviderConfig;
  state: ProviderState;
  onKeyChange: (key: string) => void;
  onToggleReveal: () => void;
  onTest: () => void;
  onSave: () => void;
  onDelete: () => void;
  disabled: boolean;
}>) {
  const { t } = useTranslation();
  const hasKey = state.key.length > 0;
  const isConnected = state.isSaved && hasKey;

  return (
    <article
      className={`relative rounded-xl border transition-all duration-300 overflow-hidden group ${
        isConnected
          ? "border-emerald-500/30 bg-emerald-500/[0.02] shadow-[0_0_16px_rgba(16,185,129,0.06)]"
          : "border-[var(--border-subtle)] hover:border-[var(--border-default)]"
      }`}
    >
      {/* Gradient header */}
      <figure
        className={`h-1 bg-gradient-to-r ${provider.gradient} transition-opacity duration-300 ${
          isConnected ? "opacity-100" : "opacity-0 group-hover:opacity-60"
        }`}
      />

      <section className="p-4 space-y-3">
        {/* Provider info row */}
        <header className="flex items-center justify-between">
          <section className="flex items-center gap-2.5">
            <span className={`${provider.color}`}>{provider.icon}</span>
            <section>
              <h4 className="text-sm font-semibold text-[var(--text-primary)]">
                {provider.name}
              </h4>
              <span
                className={`text-[10px] font-medium ${
                  isConnected
                    ? "text-emerald-600 dark:text-emerald-400"
                    : "text-[var(--text-muted)]"
                }`}
              >
                {isConnected
                  ? `✓ ${t.providers.connected}`
                  : t.providers.notConfigured}
              </span>
            </section>
          </section>

          {isConnected && (
            <button
              onClick={onDelete}
              disabled={disabled}
              className="p-1.5 rounded-lg text-[var(--text-muted)] hover:text-red-500 hover:bg-red-500/5 transition-all duration-200 opacity-0 group-hover:opacity-100"
              aria-label={t.providers.deleteProvider}
            >
              <Trash2 className="w-3.5 h-3.5" />
            </button>
          )}
        </header>

        {/* API Key input */}
        <section className="relative">
          <Input
            type={state.isRevealed ? "text" : "password"}
            value={state.key}
            onChange={(e) => onKeyChange(e.target.value)}
            placeholder={provider.placeholder}
            disabled={disabled}
            className="pr-20 bg-[var(--surface-2)] border-[var(--border-subtle)] text-sm font-mono text-[var(--text-primary)] rounded-lg"
          />
          <section className="absolute right-2 top-1/2 -translate-y-1/2 flex items-center gap-1">
            <button
              type="button"
              onClick={onToggleReveal}
              className="p-1 rounded text-[var(--text-muted)] hover:text-[var(--text-primary)] transition-colors"
              aria-label={
                state.isRevealed ? t.providers.keyHide : t.providers.keyReveal
              }
            >
              {state.isRevealed ? (
                <EyeOff className="w-3.5 h-3.5" />
              ) : (
                <Eye className="w-3.5 h-3.5" />
              )}
            </button>
          </section>
        </section>

        {/* Action buttons */}
        <section className="flex items-center gap-2">
          <Button
            type="button"
            onClick={onSave}
            disabled={disabled || !hasKey}
            className="text-[11px] font-semibold rounded-lg px-3 py-1.5 h-7 bg-[var(--accent)] hover:bg-[var(--accent-hover)] text-white"
          >
            {t.settings.saveCredentials}
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={onTest}
            disabled={disabled || !hasKey || state.isTesting}
            className="text-[11px] font-semibold rounded-lg px-3 py-1.5 h-7 border-[var(--border-default)]"
          >
            {state.isTesting ? (
              <Loader2 className="w-3 h-3 animate-spin mr-1" />
            ) : null}
            {state.isTesting ? t.providers.testing : t.providers.testConnection}
          </Button>
        </section>

        {/* Test result feedback */}
        {state.testResult && (
          <aside
            className={`flex items-center gap-1.5 text-[11px] font-medium px-2 py-1.5 rounded-lg animate-in fade-in slide-in-from-bottom-1 duration-200 ${
              state.testResult === "success"
                ? "text-emerald-600 dark:text-emerald-400 bg-emerald-500/5"
                : "text-red-600 dark:text-red-400 bg-red-500/5"
            }`}
          >
            {state.testResult === "success" ? (
              <Check className="w-3 h-3" />
            ) : (
              <AlertCircle className="w-3 h-3" />
            )}
            {state.testResult === "success"
              ? t.providers.testSuccess
              : t.providers.testFailed}
          </aside>
        )}
      </section>
    </article>
  );
}

export function AiProvidersSection() {
  const { accentClasses } = useTenant();
  const { t } = useTranslation();

  const [states, setStates] = React.useState<Record<string, ProviderState>>(
    () =>
      Object.fromEntries(
        PROVIDERS.map((p) => [
          p.id,
          {
            key: "",
            isRevealed: false,
            isTesting: false,
            testResult: null,
            isSaved: false,
          },
        ]),
      ),
  );
  const [isSaving, setIsSaving] = React.useState(false);

  // Load existing credentials on mount
  React.useEffect(() => {
    const loadCredentials = async () => {
      try {
        const session = await getSession();
        const headers: Record<string, string> = {
          "Content-Type": "application/json",
          ...(session?.user?.id
            ? { Authorization: `Bearer ${session.user.id}` }
            : {}),
        };
        const res = await fetch("/api/v1/credentials", { headers });
        if (res.ok) {
          const data = await res.json();
          setStates((prev) => {
            const updated = { ...prev };
            if (data.geminiApiKey)
              updated.gemini = {
                ...updated.gemini,
                key: data.geminiApiKey,
                isSaved: true,
              };
            if (data.anthropicApiKey)
              updated.claude = {
                ...updated.claude,
                key: data.anthropicApiKey,
                isSaved: true,
              };
            if (data.openaiApiKey)
              updated.openai = {
                ...updated.openai,
                key: data.openaiApiKey,
                isSaved: true,
              };
            return updated;
          });
        }
      } catch {
        // Silently fail — credentials endpoint may not exist yet
      }
    };
    loadCredentials();
  }, []);

  const updateState = (id: string, partial: Partial<ProviderState>) => {
    setStates((prev) => ({
      ...prev,
      [id]: { ...prev[id], ...partial },
    }));
  };

  const handleSave = async (providerId: string) => {
    setIsSaving(true);
    try {
      const session = await getSession();
      const headers: Record<string, string> = {
        "Content-Type": "application/json",
        ...(session?.user?.id
          ? { Authorization: `Bearer ${session.user.id}` }
          : {}),
      };
      const keyMap: Record<string, string> = {
        gemini: "geminiApiKey",
        claude: "anthropicApiKey",
        openai: "openaiApiKey",
      };
      const bodyKey = keyMap[providerId] || `${providerId}ApiKey`;
      const res = await fetch("/api/v1/credentials", {
        method: "PUT",
        headers,
        body: JSON.stringify({ [bodyKey]: states[providerId].key }),
      });
      if (res.ok) {
        updateState(providerId, { isSaved: true });
      }
    } catch {
      // Error handling
    } finally {
      setIsSaving(false);
    }
  };

  const handleTest = async (providerId: string) => {
    updateState(providerId, { isTesting: true, testResult: null });
    // Simulate connection test (real test would hit a backend endpoint)
    await new Promise((resolve) => setTimeout(resolve, 1500));
    const hasKey = states[providerId].key.length > 3;
    updateState(providerId, {
      isTesting: false,
      testResult: hasKey ? "success" : "error",
    });
    // Clear result after 3s
    setTimeout(() => {
      updateState(providerId, { testResult: null });
    }, 3000);
  };

  const handleDelete = (providerId: string) => {
    updateState(providerId, {
      key: "",
      isSaved: false,
      testResult: null,
      isRevealed: false,
    });
  };

  return (
    <section className="space-y-4">
      <Card className="border-[var(--border-subtle)] bg-[var(--surface-1)] shadow-sm">
        <CardHeader>
          <CardTitle className="text-base font-semibold text-[var(--text-primary)] flex items-center gap-2">
            <span
              className={`inline-flex items-center justify-center w-7 h-7 rounded-lg bg-gradient-to-br ${accentClasses.accentGradient} text-white text-xs font-bold`}
            >
              ⚡
            </span>
            {t.providers.title}
          </CardTitle>
          <CardDescription className="text-[var(--text-muted)]">
            {t.providers.subtitle}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {PROVIDERS.map((provider) => (
              <ProviderCard
                key={provider.id}
                provider={provider}
                state={states[provider.id]}
                onKeyChange={(key) =>
                  updateState(provider.id, { key, isSaved: false })
                }
                onToggleReveal={() =>
                  updateState(provider.id, {
                    isRevealed: !states[provider.id].isRevealed,
                  })
                }
                onTest={() => handleTest(provider.id)}
                onSave={() => handleSave(provider.id)}
                onDelete={() => handleDelete(provider.id)}
                disabled={isSaving}
              />
            ))}

            {/* Add custom provider card */}
            <button
              type="button"
              className="rounded-xl border-2 border-dashed border-[var(--border-subtle)] p-6 flex flex-col items-center justify-center gap-2 text-[var(--text-muted)] hover:text-[var(--text-secondary)] hover:border-[var(--border-default)] hover:bg-[var(--surface-2)] transition-all duration-200 cursor-pointer"
            >
              <Plus className="w-5 h-5" />
              <span className="text-xs font-medium">
                {t.providers.addCustom}
              </span>
            </button>
          </div>
        </CardContent>
      </Card>
    </section>
  );
}
