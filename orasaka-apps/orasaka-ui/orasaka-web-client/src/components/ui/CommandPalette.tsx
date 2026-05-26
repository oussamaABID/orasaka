"use client";

import React from "react";
import { useRouter } from "next/navigation";
import { Icon, type IconName } from "@/components/ui/icon";

/** Command entry definition */
interface CommandEntry {
  id: string;
  label: string;
  icon: IconName;
  href?: string;
  action?: () => void;
  section: string;
}

/**
 * CommandPalette — Global ⌘K overlay for lightning-fast navigation.
 * Glassmorphic floating panel with search, keyboard navigation, and fuzzy matching.
 */
export function CommandPalette() {
  const router = useRouter();
  const [isOpen, setIsOpen] = React.useState(false);
  const [query, setQuery] = React.useState("");
  const [selectedIndex, setSelectedIndex] = React.useState(0);
  const inputRef = React.useRef<HTMLInputElement>(null);

  const commands: CommandEntry[] = [
    { id: "dashboard", label: "Go to Dashboard", icon: "dashboard", href: "/", section: "Navigation" },
    { id: "chat", label: "Open Chat", icon: "chat", href: "/chat", section: "Navigation" },
    { id: "playground", label: "Open Playground", icon: "playground", href: "/playground", section: "Navigation" },
    { id: "settings", label: "Settings", icon: "settings", href: "/settings", section: "Navigation" },
    { id: "profile", label: "View Profile", icon: "profile", href: "/profile", section: "Navigation" },
    { id: "jobs", label: "Jobs History", icon: "history", href: "/dashboard/jobs", section: "Navigation" },
    { id: "admin", label: "Admin Panel", icon: "admin", href: "/dashboard/admin", section: "Navigation" },
    { id: "new-chat", label: "Start New Chat", icon: "newChat", href: "/chat", section: "Actions" },
    { id: "video-gen", label: "Generate Video", icon: "video", href: "/playground/video/generate", section: "Playground" },
    { id: "video-analyze", label: "Analyze Video", icon: "vision", href: "/playground/video/analyze", section: "Playground" },
    { id: "audio-analyze", label: "Analyze Audio", icon: "audio", href: "/playground/audio/analyze", section: "Playground" },
    { id: "text-chat", label: "Text Chat", icon: "text", href: "/playground/text/chat", section: "Playground" },
    { id: "image-gen", label: "Generate Image", icon: "image", href: "/playground/image/generate", section: "Playground" },
    { id: "code-scaffold", label: "Feature to Code", icon: "code", href: "/playground/code/scaffold", section: "Playground" },
  ];

  // Filter commands by query
  const filtered = query.trim()
    ? commands.filter((c) => c.label.toLowerCase().includes(query.toLowerCase()))
    : commands;

  // Group by section
  const grouped = filtered.reduce<Record<string, CommandEntry[]>>((acc, cmd) => {
    (acc[cmd.section] ??= []).push(cmd);
    return acc;
  }, {});

  // Flatten for keyboard nav
  const flatFiltered = Object.values(grouped).flat();

  // ⌘K global listener
  React.useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") {
        e.preventDefault();
        setIsOpen((prev) => !prev);
        setQuery("");
        setSelectedIndex(0);
      }
      if (e.key === "Escape") {
        setIsOpen(false);
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, []);

  // Focus input on open
  React.useEffect(() => {
    if (isOpen) {
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [isOpen]);

  // Keyboard navigation
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setSelectedIndex((i) => Math.min(i + 1, flatFiltered.length - 1));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setSelectedIndex((i) => Math.max(i - 1, 0));
    } else if (e.key === "Enter") {
      e.preventDefault();
      const selected = flatFiltered[selectedIndex];
      if (selected) {
        executeCommand(selected);
      }
    }
  };

  const executeCommand = (cmd: CommandEntry) => {
    setIsOpen(false);
    if (cmd.href) router.push(cmd.href);
    if (cmd.action) cmd.action();
  };

  if (!isOpen) return null;

  let runningIndex = 0;

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center pt-[15vh]">
      {/* Backdrop */}
      <button
        type="button"
        className="absolute inset-0 bg-black/50 backdrop-blur-sm border-none cursor-default"
        onClick={() => setIsOpen(false)}
        aria-label="Close command palette"
      />

      {/* Panel */}
      <div
        className="relative w-full max-w-lg rounded-[var(--radius-xl)] border border-[var(--border-default)] bg-[color-mix(in_srgb,var(--surface-1)_90%,transparent)] backdrop-blur-xl backdrop-saturate-[180%] shadow-2xl overflow-hidden animate-in fade-in slide-in-from-top-3 duration-200"
        role="dialog"
        aria-label="Command palette"
      >
        {/* Search input */}
        <header className="flex items-center gap-3 px-4 py-3 border-b border-[var(--border-subtle)]">
          <Icon name="search" size={18} className="text-[var(--text-muted)] flex-shrink-0" />
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setSelectedIndex(0);
            }}
            onKeyDown={handleKeyDown}
            placeholder="Type a command or search..."
            className="flex-1 bg-transparent border-none outline-none text-sm text-[var(--text-primary)] placeholder:text-[var(--text-muted)]"
            autoComplete="off"
          />
          <kbd className="flex items-center px-1.5 py-0.5 rounded-md bg-[var(--surface-3)] border border-[var(--border-subtle)] text-[10px] font-mono text-[var(--text-muted)]">
            ESC
          </kbd>
        </header>

        {/* Results */}
        <nav className="max-h-[320px] overflow-auto p-2 space-y-3">
          {flatFiltered.length === 0 && (
            <p className="text-center text-sm text-[var(--text-muted)] py-8">
              No commands found
            </p>
          )}

          {Object.entries(grouped).map(([section, items]) => (
            <div key={section}>
              <span className="hud-label px-2 mb-1 block">{section}</span>
              <div className="space-y-0.5">
                {items.map((cmd) => {
                  const idx = runningIndex++;
                  const isSelected = idx === selectedIndex;
                  return (
                    <button
                      key={cmd.id}
                      onClick={() => executeCommand(cmd)}
                      onMouseEnter={() => setSelectedIndex(idx)}
                      className={`w-full flex items-center gap-3 px-3 py-2 rounded-[var(--radius-sm)] text-left text-sm transition-colors duration-100 ${
                        isSelected
                          ? "bg-[var(--accent-soft)] text-[var(--text-primary)]"
                          : "text-[var(--text-secondary)] hover:bg-[var(--surface-2)]"
                      }`}
                    >
                      <Icon
                        name={cmd.icon}
                        size={16}
                        className={isSelected ? "text-[var(--accent)]" : "text-[var(--text-muted)]"}
                      />
                      <span className="flex-1">{cmd.label}</span>
                      {isSelected && (
                        <kbd className="text-[9px] font-mono text-[var(--text-muted)]">↵</kbd>
                      )}
                    </button>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>

        {/* Footer */}
        <footer className="flex items-center justify-between px-4 py-2 border-t border-[var(--border-subtle)] bg-[var(--surface-2)]">
          <section className="flex items-center gap-3 text-[10px] text-[var(--text-muted)]">
            <span className="flex items-center gap-1">
              <kbd className="px-1 py-0.5 rounded bg-[var(--surface-3)] font-mono">↑↓</kbd>
              Navigate
            </span>
            <span className="flex items-center gap-1">
              <kbd className="px-1 py-0.5 rounded bg-[var(--surface-3)] font-mono">↵</kbd>
              Open
            </span>
            <span className="flex items-center gap-1">
              <kbd className="px-1 py-0.5 rounded bg-[var(--surface-3)] font-mono">esc</kbd>
              Close
            </span>
          </section>
          <span className="text-[10px] text-[var(--text-muted)] font-mono">
            {flatFiltered.length} commands
          </span>
        </footer>
      </div>
    </div>
  );
}
