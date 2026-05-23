"use client";

import * as React from "react";
import { useQuery } from "@tanstack/react-query";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/Card";
import { useTenant } from "@/features/tenant/context/TenantContext";
import { useTranslation } from "@/core/context/LocaleContext";

interface UserProfile {
  id: string;
  username: string;
  email: string;
  authorities: string[];
  preferences: Record<string, unknown>;
}

const fetchProfile = async (): Promise<UserProfile> => {
  const query = `
    query GetProfile {
      me {
        id
        username
        email
        authorities
        preferences
      }
    }
  `;

  const response = await fetch("/api/graphql", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ query }),
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const result = await response.json();
  if (result.errors && result.errors.length > 0) {
    throw new Error(result.errors[0].message);
  }

  return result.data.me;
};

/**
 * ProfileView component fetches and displays the current user's profile metadata,
 * subscription tier, authorities, and raw preferences JSON.
 *
 * @returns The ProfileView component.
 */
export function ProfileView() {
  const { accentClasses } = useTenant();
  const { t } = useTranslation();

  const {
    data: profile,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["profile"],
    queryFn: fetchProfile,
  });

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="animate-pulse h-8 w-48 bg-zinc-200 dark:bg-zinc-800 rounded animate-duration-1000" />
        <div className="animate-pulse h-64 bg-zinc-200 dark:bg-zinc-800 rounded-lg animate-duration-1000" />
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div className="p-4 rounded-lg bg-red-50 text-red-600 dark:bg-red-950/20 dark:text-red-400 border border-red-200/50 dark:border-red-900/30">
        <p className="font-semibold">{t.profile.failedLoad}</p>
        <p className="text-sm">
          {(error as Error)?.message || t.profile.unauthError}
        </p>
      </div>
    );
  }

  // Map database role string patterns to user-friendly subscription tiers
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
          "bg-zinc-100 dark:bg-zinc-800 text-zinc-800 dark:text-zinc-200 font-medium",
      };
    }
    return {
      label: t.profile.freeTier,
      badgeClass:
        "bg-zinc-50 dark:bg-zinc-900 text-zinc-400 dark:text-zinc-600",
    };
  })();

  return (
    <div className="space-y-6 max-w-3xl animate-in fade-in slide-in-from-bottom-4 duration-300">
      <div>
        <h1 className="text-3xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
          {t.profile.title}
        </h1>
        <p className="text-zinc-500 dark:text-zinc-400 mt-1">
          {t.profile.subtitle}
        </p>
      </div>

      <Card className="relative overflow-hidden border-zinc-200/80 dark:border-zinc-800/80 backdrop-blur-sm bg-white/70 dark:bg-zinc-950/70 shadow-sm">
        {/* Subtle accent border at top of account card */}
        <div
          className={`absolute top-0 left-0 right-0 h-1 bg-gradient-to-r ${accentClasses.accentGradient}`}
        />

        <CardHeader className="pt-6">
          <CardTitle className="text-xl font-bold text-zinc-900 dark:text-zinc-100">
            {t.profile.accountDetails}
          </CardTitle>
          <CardDescription className="text-zinc-500 dark:text-zinc-400">
            {t.profile.detailsDesc}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="grid gap-6 sm:grid-cols-2">
            <div className="space-y-1">
              <span className="text-xs font-semibold uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
                {t.profile.userId}
              </span>
              <p className="text-sm font-mono text-zinc-900 dark:text-zinc-100 break-all select-all bg-zinc-50 dark:bg-zinc-900/50 p-2 rounded border border-zinc-100 dark:border-zinc-800/50">
                {profile.id}
              </p>
            </div>

            <div className="space-y-1">
              <span className="text-xs font-semibold uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
                {t.profile.subTier}
              </span>
              <div className="pt-1">
                <span
                  className={`inline-flex items-center rounded-full px-3 py-1 text-xs ${tierName.badgeClass}`}
                >
                  {tierName.label}
                </span>
              </div>
            </div>

            <div className="space-y-1">
              <span className="text-xs font-semibold uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
                {t.profile.username}
              </span>
              <p className="text-sm font-medium text-zinc-900 dark:text-zinc-100">
                {profile.username}
              </p>
            </div>

            <div className="space-y-1">
              <span className="text-xs font-semibold uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
                {t.profile.email}
              </span>
              <p className="text-sm font-medium text-zinc-900 dark:text-zinc-100">
                {profile.email}
              </p>
            </div>
          </div>

          <div className="border-t border-zinc-100 dark:border-zinc-800/80 pt-6">
            <span className="text-xs font-semibold uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
              {t.profile.assignedAuth}
            </span>
            <div className="mt-2 flex flex-wrap gap-2">
              {profile.authorities && profile.authorities.length > 0 ? (
                profile.authorities.map((auth) => (
                  <span
                    key={auth}
                    className="inline-flex items-center rounded-md bg-zinc-100 px-2.5 py-1 text-xs font-mono font-medium text-zinc-800 dark:bg-zinc-800 dark:text-zinc-200 border border-zinc-200/50 dark:border-zinc-700/50"
                  >
                    {auth}
                  </span>
                ))
              ) : (
                <span className="text-sm text-zinc-500 italic">
                  {t.profile.noAuth}
                </span>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      <Card className="border-zinc-200/80 dark:border-zinc-800/80 backdrop-blur-sm bg-white/70 dark:bg-zinc-950/70 shadow-sm">
        <CardHeader>
          <CardTitle className="text-xl font-bold text-zinc-900 dark:text-zinc-100">
            {t.profile.prefMetadata}
          </CardTitle>
          <CardDescription className="text-zinc-500 dark:text-zinc-400">
            {t.profile.prefDesc}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <pre className="overflow-x-auto rounded-lg bg-zinc-950 p-4 font-mono text-xs text-zinc-100 dark:bg-zinc-900/40 border border-zinc-200 dark:border-zinc-800/80 shadow-inner">
            {JSON.stringify(profile.preferences, null, 2)}
          </pre>
        </CardContent>
      </Card>
    </div>
  );
}
