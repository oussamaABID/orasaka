"use client";

import * as React from "react";
import { getSession } from "next-auth/react";
import { Copy, Check } from "lucide-react";
import { useTranslation } from "@/core/context/LocaleContext";

/* ─── MCP Server Types ─── */
export interface McpServerInfo {
  id: number;
  label: string;
  transportType: string;
  url?: string;
  command?: string;
  args?: string;
  authToken?: string;
  enabled: boolean;
}

/* ─── MCP Server Hook ─── */
export function useMcpServers(profileLoaded: boolean, t: ReturnType<typeof useTranslation>["t"]) {
  const [servers, setServers] = React.useState<McpServerInfo[]>([]);
  const [isLoadingServers, setIsLoadingServers] = React.useState(true);
  const [isSavingServer, setIsSavingServer] = React.useState(false);
  const [serverMessage, setServerMessage] = React.useState<string | null>(null);
  const [serverLabel, setServerLabel] = React.useState("");
  const [serverUrl, setServerUrl] = React.useState("");
  const [serverAuthToken, setServerAuthToken] = React.useState("");

  const fetchServers = React.useCallback(async () => {
    setIsLoadingServers(true);
    try {
      const session = await getSession();
      const headers = {
        "Content-Type": "application/json",
        ...(session?.user?.id
          ? { Authorization: `Bearer ${session.user.id}` }
          : {}),
      };
      const res = await fetch("/api/v1/mcp/servers/user", { headers });
      if (res.ok) {
        setServers(await res.json());
      }
    } catch (err) {
      console.error("Failed to load MCP servers:", err);
    } finally {
      setIsLoadingServers(false);
    }
  }, []);

  React.useEffect(() => {
    if (profileLoaded) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      fetchServers();
    }
  }, [profileLoaded, fetchServers]);

  const handleSaveServer = async (e: React.SubmitEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!serverLabel || !serverUrl) return;
    setIsSavingServer(true);
    setServerMessage(null);
    try {
      const session = await getSession();
      const headers = {
        "Content-Type": "application/json",
        ...(session?.user?.id
          ? { Authorization: `Bearer ${session.user.id}` }
          : {}),
      };
      const body = {
        userId: session?.user?.id || "",
        label: serverLabel,
        url: serverUrl,
        authToken: serverAuthToken || null,
        enabled: true,
      };
      const res = await fetch("/api/v1/mcp/servers/user", {
        method: "POST",
        headers,
        body: JSON.stringify(body),
      });
      if (res.ok) {
        setServerMessage(t.settings.mcpSuccess);
        setServerLabel("");
        setServerUrl("");
        setServerAuthToken("");
        fetchServers();
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
      const session = await getSession();
      const headers = {
        "Content-Type": "application/json",
        ...(session?.user?.id
          ? { Authorization: `Bearer ${session.user.id}` }
          : {}),
      };
      const res = await fetch(`/api/v1/mcp/servers/user/${id}`, {
        method: "DELETE",
        headers,
      });
      if (res.ok) {
        setServerMessage(t.settings.mcpDeleteSuccess);
        fetchServers();
      } else {
        setServerMessage(t.settings.mcpDeleteError);
      }
    } catch {
      setServerMessage(t.settings.mcpDeleteError);
    } finally {
      setIsSavingServer(false);
    }
  };

  return {
    servers,
    isLoadingServers,
    isSavingServer,
    serverMessage,
    serverLabel,
    setServerLabel,
    serverUrl,
    setServerUrl,
    serverAuthToken,
    setServerAuthToken,
    handleSaveServer,
    handleDeleteServer,
  };
}

/* ─── Copyable Field Component ─── */
export function CopyableField({
  label,
  value,
  icon: Icon,
  isMono = false,
}: Readonly<{
  label: string;
  value: string;
  icon: React.ElementType;
  isMono?: boolean;
}>) {
  const [copied, setCopied] = React.useState(false);

  const handleCopy = async () => {
    await navigator.clipboard.writeText(value);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <article className="group space-y-1.5">
      <label className="text-[11px] font-semibold uppercase tracking-wider text-[var(--text-muted)] flex items-center gap-1.5">
        <Icon className="w-3 h-3" />
        {label}
      </label>
      <section className="flex items-center gap-2">
        <p
          className={`text-sm text-[var(--text-primary)] flex-1 ${
            isMono
              ? "font-mono break-all select-all bg-[var(--surface-2)] p-2.5 rounded-lg border border-[var(--border-subtle)]"
              : "font-medium"
          }`}
        >
          {value}
        </p>
        <button
          type="button"
          onClick={handleCopy}
          className="p-1.5 rounded-lg text-[var(--text-muted)] hover:text-[var(--text-primary)] hover:bg-[var(--surface-2)] transition-all duration-150 opacity-0 group-hover:opacity-100"
          aria-label="Copy"
        >
          {copied ? (
            <Check className="w-3.5 h-3.5 text-emerald-500" />
          ) : (
            <Copy className="w-3.5 h-3.5" />
          )}
        </button>
      </section>
    </article>
  );
}
