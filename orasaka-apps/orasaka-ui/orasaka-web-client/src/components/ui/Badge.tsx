import * as React from "react";

/**
 * Props for the Badge component.
 */
export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  /** The color variant of the badge. */
  variant?: "default" | "success" | "warning" | "danger" | "accent";
}

const variantStyles: Record<NonNullable<BadgeProps["variant"]>, string> = {
  default: "bg-[var(--surface-3)] text-[var(--text-secondary)]",
  success: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400",
  warning: "bg-amber-500/10 text-amber-600 dark:text-amber-400",
  danger: "bg-red-500/10 text-red-600 dark:text-red-400",
  accent: "bg-[var(--accent-soft)] text-[var(--accent)]",
};

/**
 * A small status indicator badge — Calm Obsidian 2026 design.
 *
 * <p>Pill shape with subtle background tint and no border.
 * Used for status labels, AI indicators, and metadata tags.
 *
 * @param props - Badge properties including variant.
 * @returns A span element styled as a badge.
 * @see {@link BadgeProps}
 */
export function Badge({
  className = "",
  variant = "default",
  ...props
}: Readonly<BadgeProps>) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${variantStyles[variant]} ${className}`}
      {...props}
    />
  );
}
