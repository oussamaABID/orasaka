"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { useTranslation } from "@/core/context/LocaleContext";
import type { CatalogModel } from "./CategoryCard";
import { CategoryCard } from "./CategoryCard";
import { ModelDialog } from "./ModelDialog";
import { MetricsGrid } from "@/features/dashboard/components/MetricsGrid";
import { PipelineConfigCard } from "./PipelineConfigCard";
import { AdminToolbar } from "./AdminToolbar";
import { FeatureToggleCard } from "./FeatureToggleCard";
import { useThreadManagement } from "@/features/chat-session/hooks/useThreadManagement";
import { useTenant } from "@/features/tenant/context/TenantContext";
import { useAdminModels } from "./useAdminModels";
import {
  CATEGORY_VALUES,
  MODEL_CATEGORY,
} from "@/core/constants/capability.constants";

/** Admin-specific categories extending MODEL_CATEGORY with theme/code. */
const CATEGORIES = [...CATEGORY_VALUES, "theme", "code"] as const;

export default function AdminDashboardPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading, user } = useAuth();
  const { t } = useTranslation();
  const { threads } = useThreadManagement();
  const { accentClasses } = useTenant();
  const userId = user?.id || user?.email || "anonymous";

  const {
    models,
    loadingModels,
    errorMessage,
    setErrorMessage,
    providers,
    features,
    loadModels,
    handleSave,
    handleDelete,
    handleToggleFeature,
    saving,
    fetchWithAuth,
  } = useAdminModels();

  const [activeSessions, setActiveSessions] = React.useState(0);

  const derivedMetrics = React.useMemo(() => {
    if (globalThis.window === undefined || !isAuthenticated) {
      return { tokensUsed: 0, memoryNodes: 0 };
    }

    let totalMessagesCount = 0;
    let totalCharacters = 0;

    if (threads && Array.isArray(threads)) {
      threads.forEach((thread: { conversationId: string }) => {
        const storedMessagesStr = localStorage.getItem(
          `orasaka_messages_${userId}_${thread.conversationId}`,
        );
        if (storedMessagesStr) {
          try {
            const messages = JSON.parse(storedMessagesStr);
            if (Array.isArray(messages)) {
              totalMessagesCount += messages.length;
              messages.forEach((msg: { content?: string }) => {
                if (msg.content) {
                  totalCharacters += msg.content.length;
                }
              });
            }
          } catch {
            // Silently handle parse failures for corrupted localStorage entries
          }
        }
      });
    }

    return {
      tokensUsed: Math.round(totalCharacters / 4),
      memoryNodes: totalMessagesCount,
    };
  }, [isAuthenticated, threads, userId]);

  const metrics = React.useMemo(
    () => ({ activeSessions, ...derivedMetrics }),
    [activeSessions, derivedMetrics],
  );

  const [isModalOpen, setIsModalOpen] = React.useState(false);
  const [formModel, setFormModel] = React.useState<CatalogModel>({
    modelName: "",
    modelLabel: "",
    category: MODEL_CATEGORY.SPEECH,
    options: "",
    isDefault: false,
  });
  const [formMode, setFormMode] = React.useState<"create" | "edit">("create");

  React.useEffect(() => {
    if (
      globalThis.window === undefined ||
      !isAuthenticated ||
      user?.role !== "admin"
    )
      return;

    const pollConnections = async () => {
      try {
        const res = await fetchWithAuth(
          "/api/v1/admin/jobs/active-connections",
        );
        if (res.ok) {
          const count = await res.json();
          setActiveSessions(count);
        }
      } catch (err) {
        console.error("Failed to fetch active connections:", err);
      }
    };

    pollConnections();
    const interval = setInterval(pollConnections, 5000);
    return () => clearInterval(interval);
  }, [isAuthenticated, user, fetchWithAuth]);

  React.useEffect(() => {
    if (!isLoading) {
      if (!isAuthenticated) router.push("/login");
      else if (user?.role === "admin") loadModels();
      else router.push("/");
    }
  }, [isLoading, isAuthenticated, user, router, loadModels]);

  const openModal = (mode: "create" | "edit", model?: CatalogModel) => {
    setFormModel(
      model
        ? {
            id: model.id,
            modelName: model.modelName,
            modelLabel: model.modelLabel,
            category: model.category,
            options: model.options || "",
            isDefault: !!model.isDefault,
            providerName: model.providerName || "ollama",
          }
        : {
            modelName: "",
            modelLabel: "",
            category: MODEL_CATEGORY.SPEECH,
            options: "",
            isDefault: false,
            providerName: "ollama",
          },
    );
    setFormMode(mode);
    setErrorMessage(null);
    setIsModalOpen(true);
  };

  const onSubmit = async (e: React.SubmitEvent<HTMLFormElement>) => {
    e.preventDefault();
    const success = await handleSave(formModel, formMode);
    if (success) setIsModalOpen(false);
  };

  if (isLoading || !isAuthenticated || user?.role !== "admin") {
    return (
      <section className="flex min-h-screen items-center justify-center bg-background">
        <span className="text-zinc-400 dark:text-zinc-500 text-sm animate-pulse">
          {t.admin.loadingCredentials}
        </span>
      </section>
    );
  }

  return (
    <section className="flex h-screen overflow-hidden bg-background transition-colors duration-200">
      <Sidebar />

      <section className="flex flex-col flex-1 overflow-hidden">
        <Header />

        <main className="flex-1 overflow-auto p-6 scrollbar-thin ambient-grid">
          <section className="mx-auto max-w-5xl space-y-6 animate-in fade-in slide-in-from-bottom-3 duration-300">
            <AdminToolbar
              loadingModels={loadingModels}
              errorMessage={errorMessage}
              onRefresh={loadModels}
              onAddModel={() => openModal("create")}
              onClearError={() => setErrorMessage(null)}
              t={t}
            />

            <MetricsGrid
              metrics={metrics}
              accentClasses={accentClasses}
              t={t}
            />

            <PipelineConfigCard fetchWithAuth={fetchWithAuth} />

            {loadingModels && models.length === 0 ? (
              <div className="text-center py-20 text-zinc-400 animate-pulse text-sm">
                {t.admin.loadingModels}
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {CATEGORIES.map((cat) => (
                  <CategoryCard
                    key={cat}
                    category={cat}
                    models={models.filter((m) => m.category === cat)}
                    onEdit={(m) => openModal("edit", m)}
                    onDelete={handleDelete}
                  />
                ))}
              </div>
            )}

            <FeatureToggleCard
              features={features}
              onToggle={handleToggleFeature}
              t={t}
            />
          </section>
        </main>
      </section>

      <ModelDialog
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        formModel={formModel}
        setFormModel={setFormModel}
        formMode={formMode}
        saving={saving}
        onSubmit={onSubmit}
        errorMessage={errorMessage}
        setErrorMessage={setErrorMessage}
        providers={providers}
      />
    </section>
  );
}
