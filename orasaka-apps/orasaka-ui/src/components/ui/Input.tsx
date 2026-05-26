import * as React from "react";

/**
 * Type representing properties of the custom Input element, inheriting standard HTML input attributes.
 */
export type InputProps = React.InputHTMLAttributes<HTMLInputElement>;

/**
 * Atomic text Input component — Calm Obsidian 2026 design.
 *
 * <p>Features surface-2 background, accent focus ring via CSS variable,
 * and 44px touch target height for accessibility.
 * Compatible with all theme variants.
 *
 * @param props - Custom properties inheriting from standard input attributes.
 * @param ref - The forwarded reference targeting the native HTML input element.
 * @returns A stylized input React element.
 * @see {@link InputProps}
 */
export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className = "", type, ...props }, ref) => {
    return (
      <input
        type={type}
        className={`flex h-11 w-full rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-2)] px-3 py-2 text-sm text-[var(--text-primary)] transition-[border-color,box-shadow] duration-200 placeholder:text-[var(--text-muted)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent)] focus-visible:border-[var(--accent)] disabled:cursor-not-allowed disabled:opacity-50 ${className}`}
        ref={ref}
        {...props}
      />
    );
  },
);
Input.displayName = "Input";
