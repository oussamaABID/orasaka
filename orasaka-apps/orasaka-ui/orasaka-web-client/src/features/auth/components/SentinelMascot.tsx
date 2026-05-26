/* eslint-disable */
/* eslint-disable no-restricted-syntax */
/**
 * @file SentinelMascot.tsx
 * @description Hardware-accelerated animated SVG mascot for the login hero panel.
 * Renders a "Quantum Jarvis / Stark UI" sentinel core with orbital HUD rings,
 * pulsing energy nodes, levitation motion, breathing accent glow bloom,
 * and an orbital text ring displaying "AUTONOMOUS · INTELLIGENT · SECURE".
 *
 * 100% transparent background, GPU-backed via will-change + transform3d.
 * Animations: sentinelLevitation, hudPulse, rotator-fast, rotator-slow, breathing-glow
 * All run at 60fps via CSS keyframes — zero JS animation frames.
 */

"use client";

import React from "react";

/** Orbital text tokens displayed around the outer ring */
const ORBITAL_TEXT = "AUTONOMOUS · INTELLIGENT · SECURE · MODULAR · EXTENSIBLE · ";

/**
 * Standalone animated SVG sentinel mascot with breathing glow and orbital text.
 * Transparent background, GPU-composited CSS keyframes.
 */
export function SentinelMascot() {
  return (
    <div className="sentinel-container" aria-hidden="true">
      <svg
        viewBox="0 0 400 400"
        xmlns="http://www.w3.org/2000/svg"
        className="sentinel-svg"
        role="img"
      >
        <defs>
          {/* Core energy gradient */}
          <radialGradient id="core-glow" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="var(--accent)" stopOpacity="0.9" />
            <stop offset="40%" stopColor="var(--accent)" stopOpacity="0.4" />
            <stop offset="100%" stopColor="var(--accent)" stopOpacity="0" />
          </radialGradient>

          {/* Breathing bloom gradient — larger, softer */}
          <radialGradient id="breathing-bloom" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="var(--accent)" stopOpacity="0.25" />
            <stop offset="50%" stopColor="var(--accent)" stopOpacity="0.08" />
            <stop offset="100%" stopColor="var(--accent)" stopOpacity="0" />
          </radialGradient>

          {/* Ring gradient */}
          <linearGradient id="ring-gradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="var(--accent)" stopOpacity="0.8" />
            <stop offset="50%" stopColor="var(--accent)" stopOpacity="0.1" />
            <stop offset="100%" stopColor="var(--accent)" stopOpacity="0.6" />
          </linearGradient>

          {/* Outer ring gradient */}
          <linearGradient id="ring-gradient-outer" x1="100%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor="var(--accent)" stopOpacity="0.5" />
            <stop offset="40%" stopColor="var(--accent)" stopOpacity="0.05" />
            <stop offset="80%" stopColor="var(--accent)" stopOpacity="0.4" />
          </linearGradient>

          {/* HUD scanline filter */}
          <filter id="glow-filter">
            <feGaussianBlur in="SourceGraphic" stdDeviation="2" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
          </filter>

          <filter id="core-bloom">
            <feGaussianBlur in="SourceGraphic" stdDeviation="6" result="bloom" />
            <feComposite in="SourceGraphic" in2="bloom" operator="over" />
          </filter>

          {/* Circular text path for orbital labels */}
          <path
            id="orbital-text-path"
            d="M200,200 m-155,0 a155,155 0 1,1 310,0 a155,155 0 1,1 -310,0"
            fill="none"
          />
        </defs>

        {/* ── Breathing accent glow bloom (behind everything) ── */}
        <circle
          cx="200"
          cy="200"
          r="100"
          fill="url(#breathing-bloom)"
          className="hud-pulse"
          style={{ transformOrigin: "200px 200px" }}
        />

        {/* ── Orbital text ring (slow rotation) ───────────────── */}
        <g className="rotator-slow" style={{ transformOrigin: "200px 200px" }}>
          <text
            fill="var(--accent)"
            opacity="0.25"
            fontSize="9"
            fontFamily="var(--font-inter, 'Inter'), sans-serif"
            fontWeight="600"
            letterSpacing="0.15em"
          >
            <textPath href="#orbital-text-path" startOffset="0%">
              {ORBITAL_TEXT}
            </textPath>
          </text>
        </g>

        {/* ── Outer orbital ring (slow rotation) ─────────────── */}
        <g className="rotator-slow" style={{ transformOrigin: "200px 200px" }}>
          <ellipse
            cx="200"
            cy="200"
            rx="170"
            ry="170"
            fill="none"
            stroke="url(#ring-gradient-outer)"
            strokeWidth="0.5"
            strokeDasharray="8 16 4 12"
            opacity="0.5"
          />
          {/* Orbital energy nodes */}
          <circle cx="370" cy="200" r="3" fill="var(--accent)" opacity="0.7" className="hud-pulse" />
          <circle cx="30" cy="200" r="2" fill="var(--accent)" opacity="0.5" className="hud-pulse" />
          <circle cx="200" cy="30" r="2.5" fill="var(--accent)" opacity="0.6" className="hud-pulse" />
        </g>

        {/* ── Middle orbital ring (fast rotation) ────────────── */}
        <g className="rotator-fast" style={{ transformOrigin: "200px 200px" }}>
          <ellipse
            cx="200"
            cy="200"
            rx="130"
            ry="130"
            fill="none"
            stroke="url(#ring-gradient)"
            strokeWidth="1"
            strokeDasharray="6 20 3 15"
          />
          {/* Tick marks — HUD targeting reticle */}
          {[0, 45, 90, 135, 180, 225, 270, 315].map((angle) => {
            const rad = (angle * Math.PI) / 180;
            const x1 = 200 + 125 * Math.cos(rad);
            const y1 = 200 + 125 * Math.sin(rad);
            const x2 = 200 + 135 * Math.cos(rad);
            const y2 = 200 + 135 * Math.sin(rad);
            return (
              <line
                key={angle}
                x1={x1}
                y1={y1}
                x2={x2}
                y2={y2}
                stroke="var(--accent)"
                strokeWidth="1"
                opacity="0.3"
              />
            );
          })}
          {/* Fast ring energy nodes */}
          <circle cx="330" cy="200" r="2.5" fill="var(--accent)" className="hud-pulse" />
          <circle cx="200" cy="330" r="2" fill="var(--accent)" opacity="0.8" className="hud-pulse" />
        </g>

        {/* ── Inner hexagon frame ────────────────────────────── */}
        <g className="sentinel-levitation" style={{ transformOrigin: "200px 200px" }}>
          <polygon
            points="200,120 269,160 269,240 200,280 131,240 131,160"
            fill="none"
            stroke="var(--accent)"
            strokeWidth="0.8"
            opacity="0.25"
          />
          {/* Inner hexagon (smaller, solid) */}
          <polygon
            points="200,145 247,170 247,230 200,255 153,230 153,170"
            fill="none"
            stroke="var(--accent)"
            strokeWidth="0.5"
            opacity="0.15"
          />
        </g>

        {/* ── Central core — energy reactor ──────────────────── */}
        <g className="sentinel-levitation" style={{ transformOrigin: "200px 200px" }}>
          {/* Bloom glow */}
          <circle
            cx="200"
            cy="200"
            r="50"
            fill="url(#core-glow)"
            filter="url(#core-bloom)"
            className="hud-pulse"
          />
          {/* Solid core ring */}
          <circle
            cx="200"
            cy="200"
            r="28"
            fill="none"
            stroke="var(--accent)"
            strokeWidth="1.5"
            opacity="0.8"
          />
          {/* Inner solid core */}
          <circle
            cx="200"
            cy="200"
            r="12"
            fill="var(--accent)"
            opacity="0.9"
          />
          {/* Core highlight */}
          <circle
            cx="200"
            cy="196"
            r="5"
            fill="white"
            opacity="0.3"
          />
        </g>

        {/* ── HUD crosshair scanlines ────────────────────────── */}
        <g opacity="0.12">
          <line x1="200" y1="80" x2="200" y2="160" stroke="var(--accent)" strokeWidth="0.5" />
          <line x1="200" y1="240" x2="200" y2="320" stroke="var(--accent)" strokeWidth="0.5" />
          <line x1="80" y1="200" x2="160" y2="200" stroke="var(--accent)" strokeWidth="0.5" />
          <line x1="240" y1="200" x2="320" y2="200" stroke="var(--accent)" strokeWidth="0.5" />
        </g>

        {/* ── Corner brackets — targeting frame ──────────────── */}
        <g stroke="var(--accent)" strokeWidth="1" opacity="0.2" fill="none">
          {/* Top-left */}
          <polyline points="60,80 60,60 80,60" />
          {/* Top-right */}
          <polyline points="320,60 340,60 340,80" />
          {/* Bottom-left */}
          <polyline points="60,320 60,340 80,340" />
          {/* Bottom-right */}
          <polyline points="320,340 340,340 340,320" />
        </g>
      </svg>
    </div>
  );
}

// Re-export SentinelMini from its canonical location in components/ui.
// Kept here for backward compatibility with existing imports.
export { SentinelMini } from "@/components/ui/SentinelMini";

