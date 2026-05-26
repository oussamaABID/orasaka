"use client";

import * as React from "react";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { useTranslation } from "@/core/context/LocaleContext";
import { useAuth } from "@/features/auth/hooks/useAuth";

const selectClass =
  "flex h-10 w-full rounded-md border border-card-border bg-card-bg px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-1 focus:ring-card-border";
const inputClass = "bg-card-bg border-card-border text-foreground";
const labelClass = "text-sm font-medium text-foreground opacity-80";

interface McpServerInfo {
  id: number;
  label: string;
  transportType?: string;
  url?: string | null;
  command?: string | null;
  args?: string | null;
  authToken?: string | null;
  enabled?: boolean;
  userId?: string;
}

interface McpServersSectionProps {
  fetchHeaders: () => Promise<Record<string, string>>;
}

/**
 * McpServersSection manages the MCP server registration lifecycle.
 *
 * Displays existing MCP servers, supports adding new ones (remote SSE
 * or local STDIO), and handles deletion. Admin users see platform-wide
 * servers and transport type options; regular users see only their own
 * remote SSE registrations.
 *
 * @param props.fetchHeaders - Async function returning authorization headers.
 */
export function McpServersSection({ fetchHeaders }: McpServersSectionProps) {
  const { t } = useTranslation();
  const { user } = useAuth();
  const isAdmin = user?.role === "admin";

  const [servers, setServers] = React.useState<McpServerInfo[]>([]);
  const [isLoadingServers, setIsLoadingServers] = React.useState(false);
  const [serverLabel, setServerLabel] = React.useState("");
  const [serverUrl, setServerUrl] = React.useState("");
  const [serverAuthToken, setServerAuthToken] = React.useState("");
  const [serverTransport, setServerTransport] = React.useState<
    "remote" | "local"
  >("remote");
  const [serverCommand, setServerCommand] = React.useState("");
  const [serverArgs, setServerArgs] = React.useState("");
  const [serverMessage, setServerMessage] = React.useState<string | null>(null);
  const [isSavingServer, setIsSavingServer] = React.useState(false);

  const loadServers = React.useCallback(async () => {
    setIsLoadingServers(true);
    try {
      const headers = await fetchHeaders();
      const endpoint = isAdmin
        ? "/api/v1/mcp/servers/platform"
        : "/api/v1/mcp/servers/user";
      const res = await fetch(endpoint, { headers });
      if (res.ok) setServers(await res.json());
    } catch (err) {
      console.error("Failed to load MCP servers:", err);
    } finally {
      setIsLoadingServers(false);
    }
  }, [isAdmin, fetchHeaders]);

  React.useEffect(() => {
    if (user) {
      const initServers = async () => {
        await loadServers();
      };
      initServers();
    }
  }, [user, loadServers]);

  const handleSaveServer = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!serverLabel) return;
    setIsSavingServer(true);
    setServerMessage(null);
    try {
      const headers = await fetchHeaders();
      const endpoint = isAdmin
        ? "/api/v1/mcp/servers/platform"
        : "/api/v1/mcp/servers/user";
      const body = isAdmin
        ? {
            label: serverLabel,
            transportType: serverTransport.toUpperCase(),
            url: serverTransport === "remote" ? serverUrl : null,
            command: serverTransport === "local" ? serverCommand : null,
            args: serverTransport === "local" ? serverArgs : null,
            authToken: serverTransport === "remote" ? serverAuthToken : null,
            enabled: true,
          }
        : {
            userId: user?.id || "",
            label: serverLabel,
            url: serverUrl,
            authToken: serverAuthToken || null,
            enabled: true,
          };

      const res = await fetch(endpoint, {
        method: "POST",
        headers,
        body: JSON.stringify(body),
      });

      if (res.ok) {
        setServerMessage(t.settings.mcpSuccess);
        setServerLabel("");
        setServerUrl("");
        setServerAuthToken("");
        setServerCommand("");
        setServerArgs("");
        loadServers();
      } else {
        const errorText = await res.text();
        setServerMessage(`${t.settings.mcpError}: ${errorText}`);
      }
    } catch {
      setServerMessage(t.settings.mcpError);
    } finally {
      setIsSavingServer(false);
    }
  };

  const handleDeleteServer = async (id: number) => {
    setIsSavingServer(true);
    setServerMessage(null);
    try {
      const headers = await fetchHeaders();
      const endpoint = isAdmin
        ? `/api/v1/mcp/servers/platform/${id}`
        : `/api/v1/mcp/servers/user/${id}`;
      const res = await fetch(endpoint, { method: "DELETE", headers });
      setServerMessage(
        res.ok ? t.settings.mcpDeleteSuccess : t.settings.mcpDeleteError,
      );
      if (res.ok) loadServers();
    } catch {
      setServerMessage(t.settings.mcpDeleteError);
    } finally {
      setIsSavingServer(false);
    }
  };

  return (
    <section className="space-y-4">
      <h3 className="text-sm font-semibold text-foreground">
        {t.settings.mcpServersTitle}
      </h3>
      <p className="text-xs text-foreground opacity-70">
        {t.settings.mcpServersDesc}
      </p>
      {serverMessage && (
        <div className="p-3 bg-emerald-50 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-900/40 rounded-xl text-xs font-semibold text-emerald-700 dark:text-emerald-400">
          {serverMessage}
        </div>
      )}
      <section className="space-y-2">
        {isLoadingServers ? (
          <p className="text-xs opacity-70">{t.settings.mcpLoading}</p>
        ) : servers.length === 0 ? (
          <p className="text-xs opacity-70">{t.settings.mcpNoServers}</p>
        ) : (
          <ul className="divide-y divide-card-border border border-card-border rounded-xl overflow-hidden bg-card-bg/50">
            {servers.map((srv) => (
              <li
                key={srv.id}
                className="flex items-center justify-between p-3 text-xs"
              >
                <article className="space-y-1">
                  <h4 className="font-semibold text-foreground flex items-center gap-2">
                    <span>{srv.label}</span>
                    {isAdmin && (
                      <span className="px-1.5 py-0.5 rounded bg-zinc-200 dark:bg-zinc-800 text-[10px]">
                        {srv.transportType}
                      </span>
                    )}
                  </h4>
                  <p className="opacity-70 font-mono">
                    {srv.url || srv.command}
                  </p>
                </article>
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => handleDeleteServer(srv.id)}
                  disabled={isSavingServer}
                  className="text-red-500 border-red-500/20 hover:bg-red-50 dark:hover:bg-red-950/20 px-2 py-1 h-7 text-[10px]"
                >
                  {t.settings.mcpDelete}
                </Button>
              </li>
            ))}
          </ul>
        )}
      </section>
      <form
        onSubmit={handleSaveServer}
        className="space-y-3 p-4 border border-card-border rounded-xl bg-card-bg/20"
      >
        <div className="space-y-2">
          <label className={labelClass}>{t.settings.mcpLabel}</label>
          <Input
            value={serverLabel}
            onChange={(e) => setServerLabel(e.target.value)}
            placeholder={t.settings.mcpLabelPlaceholder}
            className={inputClass}
            required
          />
        </div>
        <div className="space-y-2">
          <label className={labelClass}>{t.settings.mcpTransportType}</label>
          <select
            className={selectClass}
            value={serverTransport}
            onChange={(e) =>
              setServerTransport(e.target.value as "remote" | "local")
            }
            disabled={!isAdmin}
          >
            <option value="remote">{t.settings.mcpRemoteLabel}</option>
            {isAdmin && (
              <option value="local">{t.settings.mcpLocalLabel}</option>
            )}
          </select>
        </div>
        {serverTransport === "remote" ? (
          <>
            <div className="space-y-2">
              <label className={labelClass}>{t.settings.mcpUrl}</label>
              <Input
                value={serverUrl}
                onChange={(e) => setServerUrl(e.target.value)}
                placeholder={t.settings.mcpUrlPlaceholder}
                className={inputClass}
                required
              />
            </div>
            <div className="space-y-2">
              <label className={labelClass}>{t.settings.mcpAuthToken}</label>
              <Input
                type="password"
                value={serverAuthToken}
                onChange={(e) => setServerAuthToken(e.target.value)}
                placeholder={t.settings.mcpAuthTokenPlaceholder}
                className={inputClass}
              />
            </div>
          </>
        ) : (
          <>
            <div className="space-y-2">
              <label className={labelClass}>{t.settings.mcpCommand}</label>
              <Input
                value={serverCommand}
                onChange={(e) => setServerCommand(e.target.value)}
                placeholder={t.settings.mcpCommandPlaceholder}
                className={inputClass}
                required
              />
            </div>
            <div className="space-y-2">
              <label className={labelClass}>{t.settings.mcpArgs}</label>
              <Input
                value={serverArgs}
                onChange={(e) => setServerArgs(e.target.value)}
                placeholder={t.settings.mcpArgsPlaceholder}
                className={inputClass}
              />
            </div>
          </>
        )}
        <Button
          type="submit"
          disabled={
            isSavingServer ||
            !serverLabel ||
            (serverTransport === "remote" ? !serverUrl : !serverCommand)
          }
          className="w-full mt-2"
        >
          {t.settings.mcpRegister}
        </Button>
      </form>
    </section>
  );
}
