import * as React from "react";

/**
 * Props for the Skeleton placeholder component.
 */
export interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {
  /** Shape variant of the skeleton. */
  variant?: "text" | "circle" | "rect";
  /** Width override (CSS value). */
  width?: string;
  /** Height override (CSS value). */
  height?: string;
}

/**
 * Shimmer loading skeleton — Calm Obsidian 2028 design.
 *
 * <p>Renders a pulsating placeholder element for async data loading.
 * Uses the `skeleton` CSS class from globals.css with configurable
 * shape variants (text line, circle avatar, rectangular card).
 * Prevents blank screens during data fetching (§9.2 compliance).
 *
 * @param props - Skeleton properties including variant and dimensions.
 * @returns A shimmer placeholder element.
 * @see {@link SkeletonProps}
 */
export function Skeleton({
  className = "",
  variant = "rect",
  width,
  height,
  style,
  ...props
}: SkeletonProps) {
  const variantClass =
    variant === "text"
      ? "skeleton skeleton-text"
      : variant === "circle"
        ? "skeleton skeleton-circle"
        : "skeleton";

  return (
    <div
      className={`${variantClass} ${className}`}
      style={{
        width: width ?? (variant === "circle" ? "2.5rem" : "100%"),
        height:
          height ??
          (variant === "text"
            ? "0.875rem"
            : variant === "circle"
              ? "2.5rem"
              : "3rem"),
        ...style,
      }}
      aria-hidden="true"
      {...props}
    />
  );
}

/**
 * Renders a multi-line skeleton group for loading states.
 *
 * @param props - Properties for the skeleton group.
 * @param props.lines - Number of text skeleton lines to render.
 * @param props.className - Additional CSS class names.
 * @returns A group of skeleton text lines.
 */
export function SkeletonGroup({
  lines = 3,
  className = "",
}: Readonly<{
  lines?: number;
  className?: string;
}>) {
  return (
    <div className={`space-y-2.5 ${className}`} aria-hidden="true">
      {Array.from({ length: lines }, (_, i) => (
        <Skeleton
          key={i}
          variant="text"
          width={i === lines - 1 ? "60%" : "100%"}
        />
      ))}
    </div>
  );
}
