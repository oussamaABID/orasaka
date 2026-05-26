/**
 * @file SentinelMini.tsx
 * @description Miniaturized Sentinel nucleus for sidebar/header branding.
 * Lives in components/ui to avoid cross-boundary imports between
 * layout components and feature modules.
 */

interface SentinelMiniProps {
  /** SVG size in pixels. Defaults to 24. */
  size?: number;
  /** Additional CSS classes. */
  className?: string;
}

/**
 * Compact 24×24 Sentinel mascot with simplified core + single orbital ring.
 *
 * @param props - Sentinel mini configuration.
 * @returns A compact SVG brand mark element.
 */
export function SentinelMini({ size = 24, className = "" }: Readonly<SentinelMiniProps>) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-hidden="true"
    >
      {/* Outer orbital */}
      <circle
        cx="12"
        cy="12"
        r="10"
        stroke="var(--accent)"
        strokeWidth="0.5"
        strokeDasharray="3 5 2 4"
        opacity="0.4"
      />
      {/* Hexagonal frame */}
      <polygon
        points="12,4 18.9,8 18.9,16 12,20 5.1,16 5.1,8"
        stroke="var(--accent)"
        strokeWidth="0.6"
        opacity="0.25"
        fill="none"
      />
      {/* Core ring */}
      <circle
        cx="12"
        cy="12"
        r="4"
        stroke="var(--accent)"
        strokeWidth="1"
        opacity="0.6"
        fill="none"
      />
      {/* Inner core */}
      <circle
        cx="12"
        cy="12"
        r="2"
        fill="var(--accent)"
        opacity="0.8"
      />
      {/* Core highlight */}
      <circle cx="12" cy="11" r="0.8" fill="white" opacity="0.3" />
      {/* Energy nodes */}
      <circle cx="22" cy="12" r="0.8" fill="var(--accent)" opacity="0.5" />
      <circle cx="12" cy="2" r="0.8" fill="var(--accent)" opacity="0.5" />
    </svg>
  );
}
