import * as React from "react";

/**
 * The root Card layout component — Calm Obsidian 2026 design.
 *
 * <p>Uses solid surface-1 background with subtle border.
 * No glassmorphism, no cleanClassName hack.
 * Hover shows slightly stronger border for depth feedback.
 *
 * @param props - React HTML div properties.
 * @returns A card container React element.
 */
export function Card({
  className = "",
  ...props
}: Readonly<React.HTMLAttributes<HTMLDivElement>>) {
  return (
    <div
      className={`rounded-xl bg-[var(--surface-1)] border border-[var(--border-subtle)] text-[var(--text-primary)] shadow-sm transition-[border-color,box-shadow] duration-200 hover:border-[var(--border-default)] ${className}`}
      {...props}
    />
  );
}

/**
 * A header container to group CardTitle and CardDescription elements.
 *
 * @param props - React HTML div properties.
 * @returns A header layout element.
 */
export function CardHeader({
  className = "",
  ...props
}: Readonly<React.HTMLAttributes<HTMLDivElement>>) {
  return (
    <div className={`flex flex-col space-y-1.5 p-6 ${className}`} {...props} />
  );
}

/**
 * The title heading text for the Card component.
 *
 * @param props - React HTML heading properties.
 * @returns A level 3 heading element.
 */
export function CardTitle({
  className = "",
  ...props
}: Readonly<React.HTMLAttributes<HTMLHeadingElement>>) {
  return (
    <h3
      className={`text-lg font-semibold leading-none tracking-tight ${className}`}
      {...props}
    />
  );
}

/**
 * Secondary metadata or descriptions rendered in muted colors.
 *
 * @param props - React HTML paragraph properties.
 * @returns A paragraph element.
 */
export function CardDescription({
  className = "",
  ...props
}: Readonly<React.HTMLAttributes<HTMLParagraphElement>>) {
  return (
    <p
      className={`text-sm text-[var(--text-secondary)] ${className}`}
      {...props}
    />
  );
}

/**
 * The main container block for card body content.
 *
 * @param props - React HTML div properties.
 * @returns A content block element.
 */
export function CardContent({
  className = "",
  ...props
}: Readonly<React.HTMLAttributes<HTMLDivElement>>) {
  return <div className={`p-6 pt-0 ${className}`} {...props} />;
}

/**
 * The footer action or status bar container at the bottom of the card.
 *
 * @param props - React HTML div properties.
 * @returns A footer action layout element.
 */
export function CardFooter({
  className = "",
  ...props
}: Readonly<React.HTMLAttributes<HTMLDivElement>>) {
  return (
    <div className={`flex items-center p-6 pt-0 ${className}`} {...props} />
  );
}
