/* eslint-disable no-restricted-syntax */
"use client";

import React from "react";
import { Icon } from "@/components/ui/icon";

/** Structured plan step from the agent's generated execution plan. */
export interface PlanStep {
  id: string;
  label: string;
  description?: string;
  status: "pending" | "approved" | "rejected";
}

interface PlanApprovalCardProps {
  /** The agent-generated plan steps awaiting user approval */
  steps: PlanStep[];
  /** Callback when the user approves the full plan */
  onApprove: () => void;
  /** Callback when the user requests adjustments */
  onRequestAdjustments: () => void;
  /** Whether the approval action is currently processing */
  isProcessing?: boolean;
}

/**
 * Interactive task-matrix card for the AWAITING_APPROVAL conversation state.
 *
 * When the agent suspends execution after plan generation, this component
 * replaces standard Markdown output with an explicit approval gate.
 * The user must either approve the plan or request adjustments.
 *
 * Design: Krizaka razor geometry, frosted glass, ice-blue accent nodes.
 */
export function PlanApprovalCard({
  steps,
  onApprove,
  onRequestAdjustments,
  isProcessing = false,
}: Readonly<PlanApprovalCardProps>) {
  return (
    <section
      className="glass-card p-0 overflow-hidden animate-in fade-in slide-in-from-bottom-3 duration-500"
      style={{ borderRadius: "var(--radius-lg)" }}
    >
      {/* Header bar */}
      <header className="flex items-center gap-3 px-5 py-4 border-b border-[var(--border-subtle)] bg-[var(--surface-2)]/30">
        <span className="inline-flex items-center justify-center w-8 h-8 bg-[var(--accent-soft)] border border-[var(--accent)]/20"
              style={{ borderRadius: "var(--radius-sm)" }}>
          <Icon name="shield" size={16} className="text-[var(--accent)]" />
        </span>
        <div>
          <h3 className="text-sm font-bold text-[var(--text-primary)] tracking-tight">
            Execution Plan — Awaiting Approval
          </h3>
          <p className="text-[11px] text-[var(--text-muted)] mt-0.5">
            Review the proposed steps before execution proceeds.
          </p>
        </div>

        {/* Status pill */}
        <span className="ml-auto inline-flex items-center gap-1.5 px-3 py-1 text-[10px] font-semibold uppercase tracking-wider bg-amber-500/10 text-amber-600 border border-amber-500/20"
              style={{ borderRadius: "var(--radius-full)" }}>
          <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse" />
          Awaiting
        </span>
      </header>

      {/* Step matrix */}
      <ul className="divide-y divide-[var(--border-subtle)]">
        {steps.map((step, idx) => (
          <li
            key={step.id}
            className="flex items-start gap-3 px-5 py-3 transition-colors duration-150 hover:bg-[var(--surface-2)]/30"
          >
            {/* Step index badge */}
            <span
              className="flex-shrink-0 w-6 h-6 flex items-center justify-center text-[10px] font-bold border border-[var(--border-default)] bg-[var(--surface-2)] text-[var(--text-muted)]"
              style={{ borderRadius: "var(--radius-sm)" }}
            >
              {idx + 1}
            </span>

            {/* Step content */}
            <div className="flex-1 min-w-0">
              <p className="text-[13px] font-medium text-[var(--text-primary)] leading-snug">
                {step.label}
              </p>
              {step.description && (
                <p className="text-[11px] text-[var(--text-secondary)] mt-0.5 leading-relaxed">
                  {step.description}
                </p>
              )}
            </div>

            {/* Status indicator */}
            <span className="flex-shrink-0 mt-0.5">
              {step.status === "approved" ? (
                <Icon name="checkCircle" size={16} className="text-emerald-500" />
              ) : step.status === "rejected" ? (
                <Icon name="error" size={16} className="text-red-500" />
              ) : (
                <Icon name="circle" size={16} className="text-[var(--text-muted)]" />
              )}
            </span>
          </li>
        ))}
      </ul>

      {/* Action bar */}
      <footer className="flex items-center gap-3 px-5 py-4 border-t border-[var(--border-subtle)] bg-[var(--surface-2)]/20">
        {/* Primary: Approve */}
        <button
          id="plan-approve-btn"
          type="button"
          onClick={onApprove}
          disabled={isProcessing}
          className="inline-flex items-center gap-2 px-5 py-2 text-[12px] font-bold bg-[var(--accent)] text-white shadow-md hover:opacity-90 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
          style={{ borderRadius: "var(--radius-sm)" }}
        >
          {isProcessing ? (
            <Icon name="loader" size={14} className="animate-spin" />
          ) : (
            <Icon name="check" size={14} />
          )}
          Approve &amp; Execute Planning
        </button>

        {/* Secondary: Request Adjustments */}
        <button
          id="plan-adjust-btn"
          type="button"
          onClick={onRequestAdjustments}
          disabled={isProcessing}
          className="inline-flex items-center gap-2 px-4 py-2 text-[12px] font-medium border border-[var(--border-default)] bg-[var(--surface-2)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--surface-3)] transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
          style={{ borderRadius: "var(--radius-sm)" }}
        >
          <Icon name="edit" size={14} />
          Request Adjustments
        </button>
      </footer>
    </section>
  );
}
