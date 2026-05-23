import * as React from "react";

/**
 * Type representing properties of the custom Input element, inheriting standard HTML input attributes.
 */
export type InputProps = React.InputHTMLAttributes<HTMLInputElement>;

/**
 * Reusable, premium styled text Input component.
 *
 * <p>Supports backdrop-blur transparency effects, custom border transitions, and focus ring indicator
 * styles configured for dark mode environments. Compatible with React Server Components (RSC) and Client Components.
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
        className={`flex h-10 w-full rounded-xl border border-zinc-200/80 bg-white/60 px-3 py-2 text-sm backdrop-blur-sm transition-all duration-200 placeholder:text-zinc-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-300 dark:focus-visible:ring-zinc-800 focus-visible:border-transparent disabled:cursor-not-allowed disabled:opacity-50 dark:border-zinc-800/60 dark:bg-zinc-900/40 dark:placeholder:text-zinc-500 ${className}`}
        ref={ref}
        {...props}
      />
    );
  },
);
Input.displayName = "Input";
