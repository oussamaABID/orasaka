"use client";

import * as React from "react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import type { TranslationDictionary } from "@/core/context/LocaleContext";

interface McpServerInfo {
  id: number;
  label: string;
  transportType: string;
  url?: string;
  command?: string;
  args?: string;
  authToken?: string;
  enabled: boolean;
}

interface ProfileMcpSectionProps {
  servers: McpServerInfo[];
  isLoadingServers: boolean;
  isSavingServer: boolean;
  serverMessage: string | null;
  onSaveServer: (e: React.SubmitEvent<HTMLFormElement>) => void;
  onDeleteServer: (id: number) => void;
  serverLabel: string;
  setServerLabel: (v: string) => void;
  serverUrl: string;
  setServerUrl: (v: string) => void;
  serverAuthToken: string;
  setServerAuthToken: (v: string) => void;
  accentGradient: string;
  t: TranslationDictionary;
}

/**
 * MCP server list and registration form extracted from ProfileView.
 */
export const ProfileMcpSection: React.FC<ProfileMcpSectionProps> = ({
  servers,
  isLoadingServers,
  isSavingServer,
  serverMessage,
  onSaveServer,
  onDeleteServer,
  serverLabel,
  setServerLabel,
  serverUrl,
  setServerUrl,
  serverAuthToken,
  setServerAuthToken,
  accentGradient,
  t,
}) => (
  <Card className="border-card-border/60 dark:border-card-border/40 backdrop-blur-md bg-card-bg/60 dark:bg-zinc-950/60 shadow-sm relative overflow-hidden rounded-2xl">
    <div
      className={`absolute top-0 left-0 right-0 h-1 bg-gradient-to-r ${accentGradient}`}
    />
    <CardHeader className="pt-6">
      <CardTitle className="text-xl font-bold text-zinc-900 dark:text-zinc-100">
        {t.settings.mcpServersTitle}
      </CardTitle>
      <CardDescription className="text-zinc-500 dark:text-zinc-400">
        {t.settings.mcpServersDesc}
      </CardDescription>
    </CardHeader>
    <CardContent className="space-y-6">
      {serverMessage && (
        <div className="p-3 bg-emerald-50 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-900/40 rounded-xl text-xs font-semibold text-emerald-700 dark:text-emerald-400">
          {serverMessage}
        </div>
      )}

        {(() => {
          if (isLoadingServers) {
            return (
              <p className="text-xs text-zinc-500 dark:text-zinc-400">
                {t.settings.mcpLoading}
              </p>
            );
          }
          if (servers.length === 0) {
            return (
              <p className="text-xs text-zinc-500 dark:text-zinc-400 italic">
                {t.settings.mcpNoServers}
              </p>
            );
          }
          return (
            <ul className="divide-y divide-card-border/40 border border-card-border/60 rounded-xl overflow-hidden bg-card-bg/30">
              {servers.map((srv) => (
                <li
                  key={srv.id}
                  className="flex items-center justify-between p-4 text-xs"
                >
                  <article className="space-y-1">
                    <h4 className="font-semibold text-zinc-800 dark:text-zinc-200">
                      {srv.label}
                    </h4>
                    <p className="text-zinc-500 dark:text-zinc-400 font-mono break-all select-all">
                      {srv.url}
                    </p>
                  </article>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => onDeleteServer(srv.id)}
                    disabled={isSavingServer}
                    className="text-red-655 border-red-500/20 hover:bg-red-50/50 dark:hover:bg-red-950/20 px-2.5 py-1 h-7 text-[10px] rounded-lg"
                  >
                    {t.settings.mcpDelete}
                  </Button>
                </li>
              ))}
            </ul>
          );
        })()}

      <form
        onSubmit={onSaveServer}
        className="space-y-4 p-5 border border-card-border/40 rounded-2xl bg-card-bg/25"
      >
        <h4 className="text-xs font-extrabold tracking-widest uppercase text-zinc-500 dark:text-zinc-400/80 mb-2">
          {t.settings.mcpRegister}
        </h4>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
              {t.settings.mcpLabel}
            </label>
            <Input
              value={serverLabel}
              onChange={(e) => setServerLabel(e.target.value)}
              placeholder={t.settings.mcpLabelPlaceholder}
              className="bg-input-bg border-input-border text-input-text w-full rounded-xl"
              required
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
              {t.settings.mcpUrl}
            </label>
            <Input
              value={serverUrl}
              onChange={(e) => setServerUrl(e.target.value)}
              placeholder={t.settings.mcpUrlPlaceholder}
              className="bg-input-bg border-input-border text-input-text w-full rounded-xl"
              required
            />
          </div>
        </div>

        <div className="space-y-1.5">
          <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
            {t.settings.mcpAuthToken}
          </label>
          <Input
            type="password"
            value={serverAuthToken}
            onChange={(e) => setServerAuthToken(e.target.value)}
            placeholder={t.settings.mcpAuthTokenPlaceholder}
            className="bg-input-bg border-input-border text-input-text w-full rounded-xl"
          />
        </div>

        <div className="pt-2 flex justify-end">
          <Button
            type="submit"
            disabled={isSavingServer || !serverLabel || !serverUrl}
            className="px-4 py-2 h-9 text-xs font-semibold tracking-widest uppercase rounded-lg"
          >
            {isSavingServer ? t.settings.saving : t.settings.mcpRegister}
          </Button>
        </div>
      </form>
    </CardContent>
  </Card>
);
