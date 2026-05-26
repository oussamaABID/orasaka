"use client";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/Card";
import { useTenant } from "@/features/tenant/context/TenantContext";
import { useTranslation } from "@/core/context/LocaleContext";
import { useProfile } from "@/features/profile/hooks/useProfile";
import { ProfileMcpSection } from "./ProfileMcpSection";
import { Settings, Shield, Mail, User, Hash } from "lucide-react";
import { useRouter } from "next/navigation";
import { CopyableField, useMcpServers } from "./ProfileViewParts";

/**
 * ProfileView component fetches and displays the current user's profile metadata,
 * subscription tier, authorities, and raw preferences JSON.
 */
export function ProfileView() {
  const { accentClasses } = useTenant();
  const { t } = useTranslation();
  const { profile, isLoading, error } = useProfile();
  const router = useRouter();

  const mcp = useMcpServers(!!profile, t);

  if (isLoading) {
    return (
      <section className="space-y-6 max-w-3xl">
        <section className="animate-pulse h-8 w-48 bg-[var(--surface-2)] rounded-lg" />
        <section className="animate-pulse h-64 bg-[var(--surface-2)] rounded-xl" />
      </section>
    );
  }

  if (error || !profile) {
    return (
      <section className="p-4 rounded-xl bg-red-500/5 text-red-600 dark:text-red-400 border border-red-500/20">
        <p className="font-semibold">{t.profile.failedLoad}</p>
        <p className="text-sm">
          {(error as Error)?.message || t.profile.unauthError}
        </p>
      </section>
    );
  }

  const tierName = (() => {
    const auths = profile.authorities || [];
    if (auths.includes("ROLE_ADMIN")) {
      return {
        label: t.profile.enterpriseTier,
        badgeClass: `bg-gradient-to-r ${accentClasses.accentGradient} text-white font-semibold shadow-sm`,
      };
    } else if (auths.includes("ROLE_USER")) {
      return {
        label: t.profile.premiumTier,
        badgeClass:
          "bg-[var(--surface-2)] text-[var(--text-primary)] font-medium border border-[var(--border-subtle)]",
      };
    }
    return {
      label: t.profile.freeTier,
      badgeClass: "bg-[var(--surface-2)] text-[var(--text-muted)]",
    };
  })();

  const initials = (profile.username || "U").slice(0, 2).toUpperCase();

  return (
    <section className="space-y-6 max-w-5xl mx-auto stagger-children animate-in fade-in slide-in-from-bottom-4 duration-300">
      {/* Hero header card */}
      <Card className="relative overflow-hidden border-[var(--border-subtle)] bg-[var(--surface-1)] shadow-sm">
        <figure
          className={`absolute top-0 left-0 right-0 h-20 bg-gradient-to-r ${accentClasses.accentGradient} opacity-10`}
        />
        <CardContent className="pt-8 pb-6">
          <section className="flex items-center gap-5">
            {/* Avatar */}
            <figure
              className={`w-16 h-16 rounded-2xl bg-gradient-to-br ${accentClasses.accentGradient} flex items-center justify-center text-white text-xl font-bold shadow-lg flex-shrink-0`}
            >
              {initials}
            </figure>
            <section className="flex-1 min-w-0">
              <h1 className="text-xl font-bold text-[var(--text-primary)] truncate">
                {profile.username}
              </h1>
              <p className="text-sm text-[var(--text-muted)] truncate">
                {profile.email}
              </p>
              <section className="flex items-center gap-2 mt-2">
                <span
                  className={`inline-flex items-center rounded-full px-3 py-1 text-[10px] ${tierName.badgeClass}`}
                >
                  {tierName.label}
                </span>
                {profile.authorities?.map((auth) => (
                  <span
                    key={auth}
                    className="inline-flex items-center rounded-md bg-[var(--surface-2)] px-2 py-0.5 text-[10px] font-mono font-medium text-[var(--text-secondary)] border border-[var(--border-subtle)]"
                  >
                    {auth}
                  </span>
                ))}
              </section>
            </section>
            <button
              type="button"
              onClick={() => router.push("/settings")}
              className="p-2.5 rounded-xl bg-[var(--surface-2)] hover:bg-[var(--surface-3)] border border-[var(--border-subtle)] text-[var(--text-muted)] hover:text-[var(--text-primary)] transition-all duration-200"
              aria-label={t.header.settings}
            >
              <Settings className="w-4 h-4" />
            </button>
          </section>
        </CardContent>
      </Card>

      {/* Account details */}
      <Card className="border-[var(--border-subtle)] bg-[var(--surface-1)] shadow-sm">
        <CardHeader>
          <CardTitle className="text-base font-semibold text-[var(--text-primary)] flex items-center gap-2">
            <Shield className="w-4 h-4 text-[var(--accent)]" />
            {t.profile.accountDetails}
          </CardTitle>
          <CardDescription className="text-[var(--text-muted)]">
            {t.profile.detailsDesc}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <section className="grid gap-5 sm:grid-cols-2">
            <CopyableField
              label={t.profile.userId}
              value={profile.id}
              icon={Hash}
              isMono
            />
            <CopyableField
              label={t.profile.username}
              value={profile.username}
              icon={User}
            />
            <CopyableField
              label={t.profile.email}
              value={profile.email}
              icon={Mail}
            />
            <article className="space-y-1.5">
              <label className="text-[11px] font-semibold uppercase tracking-wider text-[var(--text-muted)] flex items-center gap-1.5">
                <Shield className="w-3 h-3" />
                {t.profile.assignedAuth}
              </label>
              <section className="flex flex-wrap gap-1.5 pt-0.5">
                {profile.authorities && profile.authorities.length > 0 ? (
                  profile.authorities.map((auth) => (
                    <span
                      key={auth}
                      className="inline-flex items-center rounded-lg bg-[var(--surface-2)] px-2.5 py-1 text-[11px] font-mono font-medium text-[var(--text-secondary)] border border-[var(--border-subtle)]"
                    >
                      {auth}
                    </span>
                  ))
                ) : (
                  <span className="text-sm text-[var(--text-muted)] italic">
                    {t.profile.noAuth}
                  </span>
                )}
              </section>
            </article>
          </section>
        </CardContent>
      </Card>

      <ProfileMcpSection
        servers={mcp.servers}
        isLoadingServers={mcp.isLoadingServers}
        isSavingServer={mcp.isSavingServer}
        serverMessage={mcp.serverMessage}
        onSaveServer={mcp.handleSaveServer}
        onDeleteServer={mcp.handleDeleteServer}
        serverLabel={mcp.serverLabel}
        setServerLabel={mcp.setServerLabel}
        serverUrl={mcp.serverUrl}
        setServerUrl={mcp.setServerUrl}
        serverAuthToken={mcp.serverAuthToken}
        setServerAuthToken={mcp.setServerAuthToken}
        accentGradient={accentClasses.accentGradient}
        t={t}
      />

      <Card className="border-[var(--border-subtle)] bg-[var(--surface-1)] shadow-sm">
        <CardHeader>
          <CardTitle className="text-base font-semibold text-[var(--text-primary)]">
            {t.profile.prefMetadata}
          </CardTitle>
          <CardDescription className="text-[var(--text-muted)]">
            {t.profile.prefDesc}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <pre className="overflow-x-auto rounded-xl bg-zinc-950 p-4 font-mono text-xs text-emerald-400 border border-zinc-800 shadow-inner leading-relaxed max-h-48 scrollbar-thin">
            {JSON.stringify(profile.preferences, null, 2)}
          </pre>
        </CardContent>
      </Card>
    </section>
  );
}
