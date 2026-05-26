"use client";

import * as React from "react";
import { getSession } from "next-auth/react";
import { Plus } from "lucide-react";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/Card";
import { useTenant } from "@/features/tenant/context/TenantContext";
import { useTranslation } from "@/core/context/LocaleContext";
import {
  PROVIDERS,
  ProviderCard,
  type ProviderState,
} from "./AiProvidersParts";

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
