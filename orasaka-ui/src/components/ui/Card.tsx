import * as React from "react";

/**
 * The root Card layout component supplying premium border, background, and dark mode backdrop-blur styles.
 *
 * @param props - React HTML div properties.
 * @returns A card container React element.
 */
export function Card({
  className = "",
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={`rounded-2xl border border-zinc-200/80 bg-white/80 text-zinc-900 shadow-sm dark:border-zinc-800/60 dark:bg-zinc-900/50 dark:text-zinc-100 dark:backdrop-blur-md transition-all duration-200 ${className}`}
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
}: React.HTMLAttributes<HTMLDivElement>) {
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
}: React.HTMLAttributes<HTMLHeadingElement>) {
  return (
    <h3
      className={`text-2xl font-semibold leading-none tracking-tight ${className}`}
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
}: React.HTMLAttributes<HTMLParagraphElement>) {
  return (
    <p
      className={`text-sm text-zinc-500 dark:text-zinc-400 ${className}`}
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
}: React.HTMLAttributes<HTMLDivElement>) {
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
}: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={`flex items-center p-6 pt-0 ${className}`} {...props} />
  );
}
