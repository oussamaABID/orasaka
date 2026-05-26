/* eslint-disable no-restricted-syntax */
"use client";

import { useState } from "react";

interface Connector {
  id: string;
  name: string;
  icon: string;
  description: string;
  status: "connected" | "disconnected" | "pending";
  color: string;
}

const CONNECTORS: Connector[] = [
  {
    id: "jira",
    name: "Jira Cloud",
    icon: "🎯",
    description:
      "Automated ticket creation, status mutations, and comment indexing",
    status: "disconnected",
    color: "hsl(210, 100%, 56%)",
  },
  {
    id: "whatsapp",
    name: "WhatsApp Business",
    icon: "💬",
    description: "Outbound notification streaming via WhatsApp Cloud Graph API",
    status: "disconnected",
    color: "hsl(142, 72%, 42%)",
  },
  {
    id: "messenger",
    name: "Messenger",
    icon: "📱",
    description: "Real-time chat summary delivery to Messenger conversations",
    status: "disconnected",
    color: "hsl(220, 100%, 60%)",
  },
  {
    id: "slack",
    name: "Slack",
    icon: "⚡",
    description: "Push summaries and alerts into Slack channels and threads",
    status: "disconnected",
    color: "hsl(340, 82%, 52%)",
  },
  {
    id: "cli-agent",
    name: "Local CLI Agent",
    icon: "🖥️",
    description:
      "Execute automations directly on your local machine via reverse tunnel",
    status: "disconnected",
    color: "hsl(45, 100%, 51%)",
  },
];

function StatusBadge({ status }: Readonly<{ status: Connector["status"] }>) {
  const configs = {
    connected: { label: "Connected", dotClass: "status-connected" },
    disconnected: { label: "Inactive", dotClass: "status-disconnected" },
    pending: { label: "Pending...", dotClass: "status-pending" },
  };
  const config = configs[status];

  return (
    <span className="connector-status-badge">
      <span className={`connector-status-dot ${config.dotClass}`} />
      <span className="connector-status-label">{config.label}</span>
    </span>
  );
}

export default function ConnectorCatalogue() {
  const [connectors, setConnectors] = useState(CONNECTORS);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const handleToggle = (id: string) => {
    setConnectors((prev) =>
      prev.map((c) =>
        c.id === id
          ? {
              ...c,
              status: c.status === "connected" ? "disconnected" : "connected",
            }
          : c,
      ),
    );
  };

  return (
    <section className="connector-catalogue" id="connector-catalogue">
      <header className="connector-catalogue-header">
        <h2 className="hud-title connector-catalogue-title">
          Integration Connectors
        </h2>
        <p className="connector-catalogue-subtitle">
          Connect your enterprise tools for automated workflows
        </p>
      </header>

      <div className="connector-grid stagger-children">
        {connectors.map((connector) => (
          <article
            key={connector.id}
            className={`glass-card connector-card ${
              connector.status === "connected" ? "glass-card-active" : ""
            }`}
            style={
              {
                "--connector-accent": connector.color,
                "--glow-rgb": connector.color
                  .replace("hsl(", "")
                  .replace(")", ""),
              } as React.CSSProperties
            }
            id={`connector-${connector.id}`}
          >
            <div className="connector-card-header">
              <div className="connector-card-icon-wrapper">
                <span className="connector-card-icon">{connector.icon}</span>
              </div>
              <div className="connector-card-info">
                <h3 className="connector-card-name">{connector.name}</h3>
                <StatusBadge status={connector.status} />
              </div>
            </div>

            <p className="connector-card-description">
              {connector.description}
            </p>

            <div className="connector-card-actions">
              <button
                className={`connector-toggle-btn ${
                  connector.status === "connected"
                    ? "connector-toggle-active"
                    : ""
                }`}
                onClick={() => handleToggle(connector.id)}
                id={`toggle-${connector.id}`}
                aria-label={`Toggle ${connector.name} connection`}
              >
                {connector.status === "connected" ? "Disconnect" : "Connect"}
              </button>
              <button
                className="connector-config-btn"
                onClick={() =>
                  setExpandedId(
                    expandedId === connector.id ? null : connector.id,
                  )
                }
                id={`config-${connector.id}`}
                aria-label={`Configure ${connector.name}`}
              >
                ⚙️
              </button>
            </div>

            {expandedId === connector.id && (
              <div className="connector-config-panel">
                <label
                  className="hud-label"
                  htmlFor={`api-key-${connector.id}`}
                >
                  API Key
                </label>
                <input
                  type="password"
                  id={`api-key-${connector.id}`}
                  className="connector-config-input"
                  placeholder="Enter your API key securely..."
                />
                <button
                  className="connector-save-btn"
                  id={`save-${connector.id}`}
                >
                  Save Credentials
                </button>
              </div>
            )}
          </article>
        ))}
      </div>
    </section>
  );
}
