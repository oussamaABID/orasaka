import * as React from "react";

/**
 * Props for the Button component, inheriting standard HTML button attributes.
 */
export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  /** The design variant style of the button. */
  variant?: "primary" | "secondary" | "outline" | "ghost";
  /** The physical size padding and height modes. */
  size?: "sm" | "md" | "lg";
}

/**
 * Reusable, styling-rich atomic Button component.
 *
 * <p>Acts as a hybrid element that can be rendered inside React Server Components (RSC) or Client Components.
 * Supports multiple size configurations and visual variant states with smooth transitions.
 *
 * @param props - The properties for the button component.
 * @param ref - The forwarded ref targeting the native HTML button element.
 * @returns A stylized React button element.
 * @see {@link ButtonProps}
 */
export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className = "", variant = "primary", size = "md", ...props }, ref) => {
    const baseStyles =
      "inline-flex items-center justify-center rounded-xl font-medium transition-all duration-200 ease-in-out hover:scale-[1.01] active:scale-[0.99] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 disabled:pointer-events-none disabled:opacity-50";

    const variants = {
      primary:
        "bg-zinc-900 text-zinc-50 hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-200 shadow-sm border border-transparent",
      secondary:
        "bg-zinc-100 text-zinc-900 hover:bg-zinc-200/80 dark:bg-zinc-800/60 dark:text-zinc-50 dark:hover:bg-zinc-800/90 border border-transparent",
      outline:
        "border border-zinc-200 bg-white/65 hover:bg-zinc-50 hover:text-zinc-900 dark:border-zinc-800/60 dark:bg-zinc-900/30 dark:hover:bg-zinc-900/80 dark:hover:text-zinc-50 backdrop-blur-sm shadow-sm",
      ghost:
        "hover:bg-zinc-100/80 hover:text-zinc-900 dark:hover:bg-zinc-800/50 dark:hover:text-zinc-50",
    };

    const sizes = {
      sm: "h-9 px-3.5 text-xs rounded-lg",
      md: "h-10 px-4.5 py-2 text-sm",
      lg: "h-11 px-8 text-base rounded-2xl",
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
