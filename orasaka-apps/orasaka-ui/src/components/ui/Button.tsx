import * as React from "react";

/**
 * Props for the Button component, inheriting standard HTML button attributes.
 */
export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  /** The design variant style of the button. */
  variant?: "primary" | "secondary" | "outline" | "ghost";
  /** The physical size padding and height modes. */
  size?: "sm" | "md" | "lg" | "icon";
}

/**
 * Atomic Button component — Calm Obsidian 2026 design.
 *
 * <p>Features accent focus ring, subtle active press (scale 0.98),
 * and clean transitions. No shimmer, no decorative hover scale.
 * Compatible with all theme variants via CSS variable references.
 *
 * @param props - The properties for the button component.
 * @param ref - The forwarded ref targeting the native HTML button element.
 * @returns A stylized React button element.
 * @see {@link ButtonProps}
 */
export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className = "", variant = "primary", size = "md", ...props }, ref) => {
    const baseStyles =
      "inline-flex items-center justify-center rounded-lg font-medium transition-all duration-200 ease-out active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent)] focus-visible:ring-offset-1 focus-visible:ring-offset-[var(--surface-0)] disabled:pointer-events-none disabled:opacity-50 cursor-pointer";

    const variants = {
      primary:
        "bg-[var(--accent)] text-zinc-950 hover:bg-[var(--accent-hover)] shadow-sm",
      secondary:
        "bg-[var(--surface-2)] text-[var(--text-primary)] hover:bg-[var(--surface-3)] border border-[var(--border-subtle)]",
      outline:
        "border border-[var(--border-default)] bg-transparent text-[var(--text-primary)] hover:border-[var(--accent)] hover:shadow-[var(--accent-glow)]",
      ghost:
        "bg-transparent text-[var(--text-secondary)] hover:bg-[var(--surface-2)] hover:text-[var(--text-primary)]",
    };

    const sizes = {
      sm: "h-8 px-3 text-xs",
      md: "h-10 px-4 py-2 text-sm",
      lg: "h-11 px-6 text-sm font-semibold",
      icon: "h-8 w-8 p-0 text-sm",
    };

    return (
      <button
        className={`${baseStyles} ${variants[variant]} ${sizes[size]} ${className}`}
        ref={ref}
        {...props}
      />
    );
  },
);
Button.displayName = "Button";
