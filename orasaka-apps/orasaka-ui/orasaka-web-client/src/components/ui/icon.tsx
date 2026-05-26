/**
 * @file icon.tsx
 * @description Centralized polymorphic icon registry for the Orasaka UI.
 *
 * Single ICON_REGISTRY dictionary housing custom-crafted, minimalist geometric
 * SVG paths. Every path uses vector-effect="non-scaling-stroke" for visual
 * thickness consistency during layout motion sweeps.
 *
 * Usage: <Icon name="dashboard" size={20} className="text-[var(--accent)]" />
 *
 * Default: 24×24 viewBox, 1.25px stroke, round caps, currentColor.
 */

import React from "react";

/** All available icon names in the Icon system. */
export type IconName =
  | "dashboard"
  | "chat"
  | "playground"
  | "settings"
  | "profile"
  | "history"
  | "admin"
  | "video"
  | "audio"
  | "code"
  | "image"
  | "speech"
  | "vision"
  | "text"
  | "rag"
  | "memory"
  | "mcp"
  | "refiner"
  | "router"
  | "shield"
  | "quantum"
  | "pipeline"
  | "model"
  | "key"
  | "logout"
  | "search"
  | "notification"
  | "menu"
  | "close"
  | "chevronDown"
  | "chevronRight"
  | "plus"
  | "send"
  | "eye"
  | "eyeOff"
  | "attach"
  | "spark"
  | "costShield"
  | "language"
  | "tool"
  | "context"
  | "system"
  | "newChat"
  | "loader"
  | "check"
  | "warning"
  | "info"
  | "error"
  | "arrowLeft"
  | "arrowRight"
  | "arrowDown"
  | "refresh"
  | "copy"
  | "edit"
  | "trash"
  | "upload"
  | "mail"
  | "hash"
  | "sun"
  | "moon"
  | "gauge"
  | "timer"
  | "layers"
  | "activity"
  | "trendingUp"
  | "chartBar"
  | "fileJson"
  | "fileOutput"
  | "circle"
  | "checkCircle"
  | "chevronLeft"
  | "zap";

interface IconProps {
  /** Icon identifier from the registry */
  name: IconName;
  /** Pixel size (width & height). Defaults to 20. */
  size?: number;
  /** Additional CSS classes */
  className?: string;
  /** Accessible label — if omitted, icon is decorative (aria-hidden) */
  label?: string;
}

/**
 * Polymorphic SVG icon wrapper with uniform fallback configuration.
 *
 * - Default fine strokeWidth: 1.25
 * - Round caps and joins
 * - Size control via `size` prop
 * - vector-effect="non-scaling-stroke" on all paths
 *
 * @param props — Icon configuration
 * @returns SVG element with the requested icon path
 */
export function Icon({ name, size = 20, className = "", label }: Readonly<IconProps>) {
  const paths = ICON_REGISTRY[name];
  if (!paths) return null;

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.25"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      role={label ? "img" : "presentation"}
      aria-label={label}
      aria-hidden={!label}
    >
      {paths}
    </svg>
  );
}

/* ═══════════════════════════════════════════════════════════
   ICON_REGISTRY — Minimalist Geometric SVG Paths
   ─────────────────────────────────────────────────────────
   Every path entry features vector-effect="non-scaling-stroke"
   for visual thickness consistency during layout motion sweeps.
   ═══════════════════════════════════════════════════════════ */

const VE = "non-scaling-stroke" as const;

const ICON_REGISTRY: Record<IconName, React.ReactNode> = {
  // ── Navigation ──────────────────────────────────────────
  dashboard: (
    <>
      {/* Hexagonal command center */}
      <path d="M12 2L20 7V17L12 22L4 17V7L12 2Z" vectorEffect={VE} />
      <circle cx="12" cy="12" r="3" vectorEffect={VE} />
      <path d="M12 9V2M12 22V15" strokeWidth="1" opacity="0.4" vectorEffect={VE} />
    </>
  ),

  chat: (
    <>
      {/* Angular speech terminal */}
      <path d="M4 4H20V16H12L7 20V16H4V4Z" vectorEffect={VE} />
      <path d="M8 9H16M8 12H13" strokeWidth="1.2" vectorEffect={VE} />
    </>
  ),

  playground: (
    <>
      {/* Circuit board with processing nodes */}
      <rect x="3" y="3" width="18" height="18" rx="2" vectorEffect={VE} />
      <circle cx="8" cy="8" r="1.5" fill="currentColor" vectorEffect={VE} />
      <circle cx="16" cy="8" r="1.5" fill="currentColor" vectorEffect={VE} />
      <circle cx="12" cy="16" r="1.5" fill="currentColor" vectorEffect={VE} />
      <path d="M8 9.5V14L12 14.5M16 9.5V12L12 14.5" strokeWidth="1" opacity="0.5" vectorEffect={VE} />
    </>
  ),

  settings: (
    <>
      {/* Hexagonal gear with precision markers */}
      <path d="M12 2L14.5 4L17.5 3.5L18.5 6.5L21.5 8L20 10.5L21 13.5L18 14.5L17 17.5L14 17L12 19.5L10 17L7 17.5L6 14.5L3 13.5L4 10.5L2.5 8L5.5 6.5L6.5 3.5L9.5 4L12 2Z" vectorEffect={VE} />
      <circle cx="12" cy="11" r="3" vectorEffect={VE} />
    </>
  ),

  profile: (
    <>
      {/* Angular avatar frame */}
      <path d="M12 2L19 6V12C19 16 16 19.5 12 22C8 19.5 5 16 5 12V6L12 2Z" vectorEffect={VE} />
      <circle cx="12" cy="10" r="3" vectorEffect={VE} />
      <path d="M7.5 18C8.5 16 10 15 12 15C14 15 15.5 16 16.5 18" strokeWidth="1.2" vectorEffect={VE} />
    </>
  ),

  history: (
    <>
      {/* Temporal circuit with clock core */}
      <circle cx="12" cy="12" r="9" vectorEffect={VE} />
      <path d="M12 7V12L15 15" vectorEffect={VE} />
      <path d="M3 12H5M19 12H21M12 3V5" strokeWidth="1" opacity="0.3" vectorEffect={VE} />
    </>
  ),

  admin: (
    <>
      {/* Command console with equalizer bars */}
      <rect x="3" y="3" width="18" height="18" rx="2" vectorEffect={VE} />
      <path d="M7 14V17M10 10V17M13 12V17M16 8V17M19 6" strokeWidth="1.5" vectorEffect={VE} />
      <path d="M3 8H21" strokeWidth="1" opacity="0.3" vectorEffect={VE} />
    </>
  ),

  // ── Playground Capabilities ─────────────────────────────
  video: (
    <>
      {/* Film frame with play core */}
      <rect x="2" y="4" width="20" height="16" rx="2" vectorEffect={VE} />
      <polygon points="10,9 16,12 10,15" fill="currentColor" stroke="none" vectorEffect={VE} />
      <path d="M2 8H4M20 8H22M2 16H4M20 16H22" strokeWidth="1" opacity="0.4" vectorEffect={VE} />
    </>
  ),

  audio: (
    <>
      {/* Sound wave spectrum */}
      <path d="M4 12H4.01M7 8V16M10 5V19M13 8V16M16 6V18M19 9V15M22 11V13" strokeWidth="1.8" vectorEffect={VE} />
    </>
  ),

  code: (
    <>
      {/* Angular brackets with circuit trace */}
      <path d="M8 5L3 12L8 19" vectorEffect={VE} />
      <path d="M16 5L21 12L16 19" vectorEffect={VE} />
      <path d="M14 3L10 21" strokeWidth="1" opacity="0.4" vectorEffect={VE} />
    </>
  ),

  image: (
    <>
      {/* Frame with geometric landscape */}
      <rect x="3" y="3" width="18" height="18" rx="2" vectorEffect={VE} />
      <path d="M3 16L8 11L12 15L16 10L21 16" vectorEffect={VE} />
      <circle cx="8.5" cy="8.5" r="2" vectorEffect={VE} />
    </>
  ),

  speech: (
    <>
      {/* Microphone with HUD resonance rings */}
      <rect x="9" y="2" width="6" height="10" rx="3" vectorEffect={VE} />
      <path d="M5 11C5 14.866 8.134 18 12 18C15.866 18 19 14.866 19 11" vectorEffect={VE} />
      <path d="M12 18V22M9 22H15" vectorEffect={VE} />
    </>
  ),

  vision: (
    <>
      {/* Targeting eye with scan reticle */}
      <path d="M2 12C2 12 5 5 12 5C19 5 22 12 22 12C22 12 19 19 12 19C5 19 2 12 2 12Z" vectorEffect={VE} />
      <circle cx="12" cy="12" r="3" vectorEffect={VE} />
      <circle cx="12" cy="12" r="1" fill="currentColor" stroke="none" vectorEffect={VE} />
    </>
  ),

  text: (
    <>
      {/* Document with scan lines */}
      <path d="M4 3H16L20 7V21H4V3Z" vectorEffect={VE} />
      <path d="M16 3V7H20" vectorEffect={VE} />
      <path d="M7 11H17M7 14H14M7 17H11" strokeWidth="1" opacity="0.5" vectorEffect={VE} />
    </>
  ),

  // ── Interceptor Pipeline Icons ──────────────────────────
  rag: (
    <>
      {/* Knowledge retrieval — book with search beam */}
      <path d="M4 4H18C19.1 4 20 4.9 20 6V20L17 18H6C4.9 18 4 17.1 4 16V4Z" vectorEffect={VE} />
      <circle cx="16" cy="14" r="3" vectorEffect={VE} />
      <path d="M18.5 16.5L21 19" vectorEffect={VE} />
      <path d="M7 8H13M7 11H11" strokeWidth="1" opacity="0.5" vectorEffect={VE} />
    </>
  ),

  memory: (
    <>
      {/* Brain circuit — neural network nodes */}
      <circle cx="6" cy="6" r="2" vectorEffect={VE} />
      <circle cx="18" cy="6" r="2" vectorEffect={VE} />
      <circle cx="12" cy="12" r="2.5" vectorEffect={VE} />
      <circle cx="6" cy="18" r="2" vectorEffect={VE} />
      <circle cx="18" cy="18" r="2" vectorEffect={VE} />
      <path d="M7.5 7.5L10 10M14 10L16.5 7.5M7.5 16.5L10 14M14 14L16.5 16.5" strokeWidth="1" opacity="0.4" vectorEffect={VE} />
    </>
  ),

  mcp: (
    <>
      {/* Model Context Protocol — plug with data stream */}
      <path d="M12 2V6M8 2V5M16 2V5" vectorEffect={VE} />
      <rect x="5" y="6" width="14" height="8" rx="2" vectorEffect={VE} />
      <path d="M9 14V18L12 20L15 18V14" vectorEffect={VE} />
      <circle cx="12" cy="10" r="1.5" fill="currentColor" stroke="none" vectorEffect={VE} />
    </>
  ),

  refiner: (
    <>
      {/* Precision funnel with filtering layers */}
      <path d="M3 4H21L15 12V19L9 22V12L3 4Z" vectorEffect={VE} />
      <path d="M6 7H18M8 10H16" strokeWidth="1" opacity="0.3" vectorEffect={VE} />
    </>
  ),

  router: (
    <>
      {/* Intent router — directional split paths */}
      <circle cx="5" cy="12" r="2.5" vectorEffect={VE} />
      <circle cx="19" cy="6" r="2" vectorEffect={VE} />
      <circle cx="19" cy="12" r="2" vectorEffect={VE} />
      <circle cx="19" cy="18" r="2" vectorEffect={VE} />
      <path d="M7.5 11L17 6.5M7.5 12H17M7.5 13L17 17.5" strokeWidth="1" vectorEffect={VE} />
    </>
  ),

  shield: (
    <>
      {/* Security shield with check core */}
      <path d="M12 2L21 6V12C21 17 17 21 12 22C7 21 3 17 3 12V6L12 2Z" vectorEffect={VE} />
      <path d="M8.5 12L11 14.5L16 9.5" strokeWidth="1.8" vectorEffect={VE} />
    </>
  ),

  quantum: (
    <>
      {/* Quantum validation — atomic orbital */}
      <circle cx="12" cy="12" r="2" fill="currentColor" stroke="none" vectorEffect={VE} />
      <ellipse cx="12" cy="12" rx="10" ry="4" vectorEffect={VE} />
      <ellipse cx="12" cy="12" rx="10" ry="4" transform="rotate(60 12 12)" vectorEffect={VE} />
      <ellipse cx="12" cy="12" rx="10" ry="4" transform="rotate(-60 12 12)" vectorEffect={VE} />
    </>
  ),

  pipeline: (
    <>
      {/* Pipeline flow — connected processing nodes */}
      <circle cx="4" cy="12" r="2" vectorEffect={VE} />
      <circle cx="12" cy="12" r="2.5" vectorEffect={VE} />
      <circle cx="20" cy="12" r="2" vectorEffect={VE} />
      <path d="M6 12H9.5M14.5 12H18" vectorEffect={VE} />
      <path d="M12 6V9.5M12 14.5V18" strokeWidth="1" opacity="0.3" vectorEffect={VE} />
    </>
  ),

  model: (
    <>
      {/* AI model — brain chip */}
      <rect x="4" y="4" width="16" height="16" rx="3" vectorEffect={VE} />
      <circle cx="12" cy="12" r="3" vectorEffect={VE} />
      <path d="M4 9H2M4 15H2M22 9H20M22 15H20M9 4V2M15 4V2M9 20V22M15 20V22" strokeWidth="1.2" vectorEffect={VE} />
    </>
  ),

  costShield: (
    <>
      {/* Cost monitoring — shield with gauge */}
      <path d="M12 2L20 6V12C20 17 16.5 20.5 12 22C7.5 20.5 4 17 4 12V6L12 2Z" vectorEffect={VE} />
      <path d="M8 13C8 13 10 10 12 10C14 10 16 13 16 13" strokeWidth="1.2" vectorEffect={VE} />
      <circle cx="12" cy="14" r="1" fill="currentColor" stroke="none" vectorEffect={VE} />
    </>
  ),

  language: (
    <>
      {/* Translation — globe with text */}
      <circle cx="12" cy="12" r="9" vectorEffect={VE} />
      <path d="M2.5 9H21.5M2.5 15H21.5" strokeWidth="1" opacity="0.4" vectorEffect={VE} />
      <path d="M12 3C8 7 8 17 12 21M12 3C16 7 16 17 12 21" vectorEffect={VE} />
    </>
  ),

  tool: (
    <>
      {/* Tool callback — wrench with circuit */}
      <path d="M14.7 6.3C13.3 4.9 11.1 4.6 9.3 5.5L12.5 8.7L11.1 10.1L7.9 6.9C7 8.7 7.3 10.9 8.7 12.3C10.1 13.7 12.2 14 14 13.2L19 18.2C19.4 18.6 20 18.6 20.4 18.2C20.8 17.8 20.8 17.2 20.4 16.8L15.4 11.8C16.2 10 15.9 7.9 14.7 6.3Z" vectorEffect={VE} />
    </>
  ),

  context: (
    <>
      {/* User context — badge with identity */}
      <circle cx="12" cy="8" r="4" vectorEffect={VE} />
      <path d="M4 20C4 16.7 7.6 14 12 14C16.4 14 20 16.7 20 20" vectorEffect={VE} />
      <path d="M16 7L18 5M8 7L6 5" strokeWidth="1" opacity="0.3" vectorEffect={VE} />
    </>
  ),

  system: (
    <>
      {/* System context — server rack */}
      <rect x="4" y="2" width="16" height="6" rx="1" vectorEffect={VE} />
      <rect x="4" y="10" width="16" height="6" rx="1" vectorEffect={VE} />
      <path d="M4 18H20V22H4Z" vectorEffect={VE} />
      <circle cx="17" cy="5" r="1" fill="currentColor" stroke="none" vectorEffect={VE} />
      <circle cx="17" cy="13" r="1" fill="currentColor" stroke="none" vectorEffect={VE} />
      <circle cx="17" cy="20" r="1" fill="currentColor" stroke="none" vectorEffect={VE} />
    </>
  ),

  // ── UI Actions ──────────────────────────────────────────
  key: (
    <>
      <circle cx="8" cy="10" r="5" vectorEffect={VE} />
      <path d="M12.5 13.5L21 22" vectorEffect={VE} />
      <path d="M17 17L19 15M19.5 19.5L21.5 17.5" strokeWidth="1.2" vectorEffect={VE} />
    </>
  ),

  logout: (
    <>
      <path d="M9 21H5C4.4 21 4 20.6 4 20V4C4 3.4 4.4 3 5 3H9" vectorEffect={VE} />
      <path d="M16 17L21 12L16 7" vectorEffect={VE} />
      <path d="M21 12H9" vectorEffect={VE} />
    </>
  ),

  search: (
    <>
      <circle cx="10.5" cy="10.5" r="7" vectorEffect={VE} />
      <path d="M15.5 15.5L21 21" strokeWidth="2" vectorEffect={VE} />
    </>
  ),

  notification: (
    <>
      <path d="M18 8C18 4.7 15.3 2 12 2C8.7 2 6 4.7 6 8C6 15 3 17 3 17H21C21 17 18 15 18 8Z" vectorEffect={VE} />
      <path d="M13.7 21C13.1 21.6 12.6 22 12 22C11.4 22 10.9 21.6 10.3 21" vectorEffect={VE} />
    </>
  ),

  menu: (
    <>
      <path d="M4 7H20M4 12H20M4 17H20" strokeWidth="1.8" vectorEffect={VE} />
    </>
  ),

  close: (
    <>
      <path d="M6 6L18 18M18 6L6 18" strokeWidth="1.8" vectorEffect={VE} />
    </>
  ),

  chevronDown: (
    <>
      <path d="M6 9L12 15L18 9" strokeWidth="1.8" vectorEffect={VE} />
    </>
  ),

  chevronRight: (
    <>
      <path d="M9 6L15 12L9 18" strokeWidth="1.8" vectorEffect={VE} />
    </>
  ),

  plus: (
    <>
      <path d="M12 5V19M5 12H19" strokeWidth="1.8" vectorEffect={VE} />
    </>
  ),

  send: (
    <>
      {/* Angular arrow — launch trajectory */}
      <path d="M22 2L11 13" vectorEffect={VE} />
      <path d="M22 2L15 22L11 13L2 9L22 2Z" vectorEffect={VE} />
    </>
  ),

  eye: (
    <>
      <path d="M2 12C2 12 5 5 12 5C19 5 22 12 22 12C22 12 19 19 12 19C5 19 2 12 2 12Z" vectorEffect={VE} />
      <circle cx="12" cy="12" r="3" vectorEffect={VE} />
    </>
  ),

  eyeOff: (
    <>
      <path d="M2 2L22 22" strokeWidth="1.8" vectorEffect={VE} />
      <path d="M6.7 6.7C4.3 8.5 2 12 2 12C2 12 5 19 12 19C13.8 19 15.4 18.5 16.8 17.8" vectorEffect={VE} />
      <path d="M9.9 4.2C10.6 4.1 11.3 4 12 4C19 4 22 12 22 12C22 12 21 14.1 19 16" vectorEffect={VE} />
    </>
  ),

  attach: (
    <>
      <path d="M21.4 11.6L12.2 20.8C9.8 23.2 5.8 23.2 3.4 20.8C1 18.4 1 14.4 3.4 12L13.6 1.8C15.2 0.2 17.8 0.2 19.4 1.8C21 3.4 21 6 19.4 7.6L9.2 17.8C8.4 18.6 7.1 18.6 6.3 17.8C5.5 17 5.5 15.7 6.3 14.9L15.5 5.7" vectorEffect={VE} />
    </>
  ),

  spark: (
    <>
      {/* AI spark — diamond with energy rays */}
      <path d="M12 2L14 9L21 9L15.5 13.5L17.5 21L12 16.5L6.5 21L8.5 13.5L3 9L10 9L12 2Z" vectorEffect={VE} />
    </>
  ),

  newChat: (
    <>
      {/* New conversation — chat bubble with plus */}
      <path d="M4 4H20V16H12L7 20V16H4V4Z" vectorEffect={VE} />
      <path d="M12 8V14M9 11H15" strokeWidth="1.5" vectorEffect={VE} />
    </>
  ),

  // ── Extended Icons ──────────────────────────────────────
  loader: (
    <>
      {/* Spinning loader ring */}
      <path d="M12 2C6.5 2 2 6.5 2 12" strokeWidth="2" vectorEffect={VE} />
      <path d="M12 2C17.5 2 22 6.5 22 12" strokeWidth="2" opacity="0.3" vectorEffect={VE} />
      <path d="M22 12C22 17.5 17.5 22 12 22" strokeWidth="2" opacity="0.15" vectorEffect={VE} />
      <path d="M2 12C2 17.5 6.5 22 12 22" strokeWidth="2" opacity="0.08" vectorEffect={VE} />
    </>
  ),

  check: (
    <>
      <path d="M5 12L10 17L20 7" strokeWidth="2" vectorEffect={VE} />
    </>
  ),

  warning: (
    <>
      {/* Triangle alert */}
      <path d="M12 2L22 20H2L12 2Z" vectorEffect={VE} />
      <path d="M12 10V14" strokeWidth="1.8" vectorEffect={VE} />
      <circle cx="12" cy="17" r="0.5" fill="currentColor" stroke="none" vectorEffect={VE} />
    </>
  ),

  info: (
    <>
      <circle cx="12" cy="12" r="9" vectorEffect={VE} />
      <path d="M12 8V8.01" strokeWidth="2" vectorEffect={VE} />
      <path d="M12 12V17" strokeWidth="1.5" vectorEffect={VE} />
    </>
  ),

  error: (
    <>
      <circle cx="12" cy="12" r="9" vectorEffect={VE} />
      <path d="M15 9L9 15M9 9L15 15" strokeWidth="1.8" vectorEffect={VE} />
    </>
  ),

  arrowLeft: (
    <>
      <path d="M19 12H5M12 5L5 12L12 19" strokeWidth="1.8" vectorEffect={VE} />
    </>
  ),

  arrowRight: (
    <>
      <path d="M5 12H19M12 5L19 12L12 19" strokeWidth="1.8" vectorEffect={VE} />
    </>
  ),

  arrowDown: (
    <>
      <path d="M12 5V19M5 12L12 19L19 12" strokeWidth="1.8" vectorEffect={VE} />
    </>
  ),

  refresh: (
    <>
      <path d="M3 12C3 7 7 3 12 3C15.5 3 18.5 5 20 8" vectorEffect={VE} />
      <path d="M21 12C21 17 17 21 12 21C8.5 21 5.5 19 4 16" vectorEffect={VE} />
      <path d="M20 3V8H15" vectorEffect={VE} />
      <path d="M4 21V16H9" vectorEffect={VE} />
    </>
  ),

  copy: (
    <>
      <rect x="9" y="9" width="12" height="12" rx="2" vectorEffect={VE} />
      <path d="M5 15H4C3.4 15 3 14.6 3 14V4C3 3.4 3.4 3 4 3H14C14.6 3 15 3.4 15 4V5" vectorEffect={VE} />
    </>
  ),

  edit: (
    <>
      <path d="M17 3L21 7L8 20H4V16L17 3Z" vectorEffect={VE} />
      <path d="M14.5 5.5L18.5 9.5" strokeWidth="1" opacity="0.3" vectorEffect={VE} />
    </>
  ),

  trash: (
    <>
      <path d="M3 6H21M8 6V4C8 3.4 8.4 3 9 3H15C15.6 3 16 3.4 16 4V6" vectorEffect={VE} />
      <path d="M5 6V20C5 20.6 5.4 21 6 21H18C18.6 21 19 20.6 19 20V6" vectorEffect={VE} />
      <path d="M10 10V17M14 10V17" strokeWidth="1.2" vectorEffect={VE} />
    </>
  ),

  upload: (
    <>
      <path d="M4 17V19C4 20.1 4.9 21 6 21H18C19.1 21 20 20.1 20 19V17" vectorEffect={VE} />
      <path d="M12 15V3" vectorEffect={VE} />
      <path d="M8 7L12 3L16 7" vectorEffect={VE} />
    </>
  ),

  mail: (
    <>
      <rect x="2" y="4" width="20" height="16" rx="2" vectorEffect={VE} />
      <path d="M2 7L12 13L22 7" vectorEffect={VE} />
    </>
  ),

  hash: (
    <>
      <path d="M4 9H20M4 15H20M10 3V21M14 3V21" vectorEffect={VE} />
    </>
  ),

  sun: (
    <>
      <circle cx="12" cy="12" r="4" vectorEffect={VE} />
      <path d="M12 2V4M12 20V22M4.9 4.9L6.3 6.3M17.7 17.7L19.1 19.1M2 12H4M20 12H22M4.9 19.1L6.3 17.7M17.7 6.3L19.1 4.9" vectorEffect={VE} />
    </>
  ),

  moon: (
    <>
      <path d="M21 12.8A9 9 0 1 1 11.2 3 7 7 0 0 0 21 12.8Z" vectorEffect={VE} />
    </>
  ),

  gauge: (
    <>
      <path d="M12 2C6.5 2 2 6.5 2 12C2 17.5 6.5 22 12 22C17.5 22 22 17.5 22 12" vectorEffect={VE} />
      <path d="M12 12L18 6" strokeWidth="1.8" vectorEffect={VE} />
      <circle cx="12" cy="12" r="2" fill="currentColor" stroke="none" vectorEffect={VE} />
    </>
  ),

  timer: (
    <>
      <circle cx="12" cy="13" r="8" vectorEffect={VE} />
      <path d="M12 9V13L15 16" vectorEffect={VE} />
      <path d="M10 2H14" strokeWidth="1.5" vectorEffect={VE} />
    </>
  ),

  layers: (
    <>
      <path d="M12 2L2 7L12 12L22 7L12 2Z" vectorEffect={VE} />
      <path d="M2 12L12 17L22 12" vectorEffect={VE} />
      <path d="M2 17L12 22L22 17" vectorEffect={VE} />
    </>
  ),

  activity: (
    <>
      <path d="M22 12H18L15 21L9 3L6 12H2" strokeWidth="1.8" vectorEffect={VE} />
    </>
  ),

  trendingUp: (
    <>
      <path d="M2 17L8 11L12 15L22 5" strokeWidth="1.8" vectorEffect={VE} />
      <path d="M16 5H22V11" vectorEffect={VE} />
    </>
  ),

  chartBar: (
    <>
      <rect x="3" y="12" width="4" height="9" rx="1" vectorEffect={VE} />
      <rect x="10" y="6" width="4" height="15" rx="1" vectorEffect={VE} />
      <rect x="17" y="3" width="4" height="18" rx="1" vectorEffect={VE} />
    </>
  ),

  fileJson: (
    <>
      <path d="M4 3H16L20 7V21H4V3Z" vectorEffect={VE} />
      <path d="M16 3V7H20" vectorEffect={VE} />
      <path d="M8 13C8 12 9 12 9 13V14C9 15 8 15 8 14M16 13C16 12 15 12 15 13V14C15 15 16 15 16 14" strokeWidth="1.2" vectorEffect={VE} />
      <path d="M11 12H13" strokeWidth="1" opacity="0.5" vectorEffect={VE} />
    </>
  ),

  fileOutput: (
    <>
      <path d="M4 3H16L20 7V21H4V3Z" vectorEffect={VE} />
      <path d="M16 3V7H20" vectorEffect={VE} />
      <path d="M9 15L12 18L15 15M12 11V18" strokeWidth="1.2" vectorEffect={VE} />
    </>
  ),

  circle: (
    <>
      <circle cx="12" cy="12" r="9" vectorEffect={VE} />
    </>
  ),

  checkCircle: (
    <>
      <circle cx="12" cy="12" r="9" vectorEffect={VE} />
      <path d="M8 12L11 15L17 9" strokeWidth="1.8" vectorEffect={VE} />
    </>
  ),

  chevronLeft: (
    <>
      <path d="M15 6L9 12L15 18" strokeWidth="1.8" vectorEffect={VE} />
    </>
  ),

  zap: (
    <>
      {/* Lightning bolt — energy */}
      <path d="M13 2L3 14H12L11 22L21 10H12L13 2Z" vectorEffect={VE} />
    </>
  ),
};
