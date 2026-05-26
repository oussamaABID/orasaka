/* eslint-disable no-restricted-syntax */
"use client";

import { useState } from "react";
import { format, formatISO, parseISO, subMinutes } from "date-fns";

interface AutomationJob {
  id: string;
  connectorType: string;
  action: string;
  status:
    | "PENDING_APPROVAL"
    | "APPROVED"
    | "RUNNING"
    | "COMPLETED"
    | "FAILED"
    | "AWAITING_CLI_EXECUTION";
  userId: string;
  createdAt: string;
  payload: Record<string, unknown>;
}

const STATUS_CONFIG: Record<
  AutomationJob["status"],
  { label: string; color: string; icon: string }
> = {
  PENDING_APPROVAL: {
    label: "Pending Approval",
    color: "hsl(45, 100%, 51%)",
    icon: "⏳",
  },
  APPROVED: {
    label: "Approved",
    color: "hsl(210, 100%, 56%)",
    icon: "✅",
  },
  RUNNING: {
    label: "Running",
    color: "hsl(280, 80%, 60%)",
    icon: "⚡",
  },
  COMPLETED: {
    label: "Completed",
    color: "hsl(142, 72%, 42%)",
    icon: "✓",
  },
  FAILED: {
    label: "Failed",
    color: "hsl(0, 84%, 60%)",
    icon: "✗",
  },
  AWAITING_CLI_EXECUTION: {
    label: "Awaiting CLI",
    color: "hsl(200, 80%, 50%)",
    icon: "🖥️",
  },
};

// Demo data for development
const DEMO_JOBS: AutomationJob[] = [
  {
    id: "job-001",
    connectorType: "JIRA",
    action: "CREATE_TICKET",
    status: "PENDING_APPROVAL",
    userId: "user-1",
    createdAt: formatISO(new Date()),
    payload: { project: "ORASAKA", summary: "Deploy automation pipeline" },
  },
  {
    id: "job-002",
    connectorType: "CLI_AGENT",
    action: "SORT_FILES",
    status: "PENDING_APPROVAL",
    userId: "user-1",
    createdAt: formatISO(new Date()),
    payload: { directory: "~/Photos", pattern: "*.jpg" },
  },
  {
    id: "job-003",
    connectorType: "WHATSAPP",
    action: "SEND_NOTIFICATION",
    status: "RUNNING",
    userId: "user-1",
    createdAt: formatISO(subMinutes(new Date(), 1)),
    payload: { recipient: "+1234567890", message: "Build completed" },
  },
  {
    id: "job-004",
    connectorType: "SLACK",
    action: "POST_SUMMARY",
    status: "COMPLETED",
    userId: "user-1",
    createdAt: formatISO(subMinutes(new Date(), 2)),
    payload: { channel: "#deployments" },
  },
];

function JobStatusChip({
  status,
}: Readonly<{ status: AutomationJob["status"] }>) {
  const config = STATUS_CONFIG[status];
  return (
    <span
      className="job-status-chip"
      style={{ "--chip-color": config.color } as React.CSSProperties}
    >
      <span className="job-status-chip-icon">{config.icon}</span>
      {config.label}
    </span>
  );
}

function ApprovalActions({
  jobId,
  onApprove,
  onRevoke,
}: Readonly<{
  jobId: string;
  onApprove: (id: string) => void;
  onRevoke: (id: string) => void;
}>) {
  return (
    <div className="job-approval-actions">
      <button
        className="job-approve-btn"
        onClick={() => onApprove(jobId)}
        id={`approve-${jobId}`}
        aria-label="Approve execution"
      >
        <span className="job-approve-btn-icon">▶</span>
        <span>Approve Execution</span>
      </button>
      <button
        className="job-revoke-btn"
        onClick={() => onRevoke(jobId)}
        id={`revoke-${jobId}`}
        aria-label="Revoke job"
      >
        Revoke Job
      </button>
    </div>
  );
}

export default function LiveJobGrid() {
  const [jobs, setJobs] = useState<AutomationJob[]>(DEMO_JOBS);

  const handleApprove = async (jobId: string) => {
    setJobs((prev) =>
      prev.map((j) =>
        j.id === jobId ? { ...j, status: "APPROVED" as const } : j,
      ),
    );
    try {
      await fetch(`/api/v1/automation/jobs/${jobId}/approve`, {
        method: "POST",
      });
    } catch {
      // Silently handle — job already updated optimistically
    }
  };

  const handleRevoke = async (jobId: string) => {
    setJobs((prev) => prev.filter((j) => j.id !== jobId));
    try {
      await fetch(`/api/v1/automation/jobs/${jobId}/revoke`, {
        method: "POST",
      });
    } catch {
      // Silently handle
    }
  };

  const pendingCount = jobs.filter(
    (j) => j.status === "PENDING_APPROVAL",
  ).length;
  const runningCount = jobs.filter((j) => j.status === "RUNNING").length;

  return (
    <section className="live-job-grid" id="live-job-grid">
      <header className="live-job-grid-header">
        <div>
          <h2 className="hud-title live-job-grid-title">Active Automations</h2>
          <p className="live-job-grid-subtitle">
            Real-time task monitoring and approval control
          </p>
        </div>
        <div className="live-job-grid-stats">
          <div className="live-job-stat">
            <span className="hud-label">Pending</span>
            <span className="hud-value live-job-stat-pending">
              {pendingCount}
            </span>
          </div>
          <div className="live-job-stat">
            <span className="hud-label">Running</span>
            <span className="hud-value live-job-stat-running">
              {runningCount}
            </span>
          </div>
        </div>
      </header>

      <div className="live-job-list stagger-children">
        {jobs.map((job) => (
          <article
            key={job.id}
            className={`glass-card live-job-card ${(() => {
              if (job.status === "PENDING_APPROVAL") return "live-job-card-pending";
              if (job.status === "RUNNING") return "glass-card-active";
              return "";
            })()}`}
            id={`job-card-${job.id}`}
          >
            <div className="live-job-card-header">
              <div className="live-job-card-meta">
                <span className="hud-label">{job.connectorType}</span>
                <h3 className="live-job-card-action">{job.action}</h3>
              </div>
              <JobStatusChip status={job.status} />
            </div>

            <div className="live-job-card-payload">
              <span className="hud-label">Payload</span>
              <code className="live-job-card-code">
                {JSON.stringify(job.payload, null, 2)}
              </code>
            </div>

            <div className="live-job-card-footer">
              <span className="live-job-card-time">
                {format(parseISO(job.createdAt), "HH:mm:ss")}
              </span>
              {job.status === "PENDING_APPROVAL" && (
                <ApprovalActions
                  jobId={job.id}
                  onApprove={handleApprove}
                  onRevoke={handleRevoke}
                />
              )}
            </div>
          </article>
        ))}

        {jobs.length === 0 && (
          <div className="live-job-empty glass-card">
            <p>No active automation jobs</p>
          </div>
        )}
      </div>
    </section>
  );
}
