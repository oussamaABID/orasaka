"use client";

/**
 * @file SidebarContext.tsx
 * @description Provides navigation sidebar open/close state controls.
 * Useful for switching layout overlays on smaller devices or collapsible dashboards.
 *
 * Client Directive: "use client"
 * Statefulness: Encapsulates toggle, close, and open actions using standard local state.
 */

import React, { createContext, useContext, useState } from "react";

/**
 * Shape of the navigation sidebar drawer control context.
 */
interface SidebarContextType {
  /** Indicates whether the sidebar drawer is expanded. */
  isOpen: boolean;
  /** Toggles the sidebar visibility state. */
  toggle: () => void;
  /** Forces the sidebar state to closed. */
  close: () => void;
  /** Forces the sidebar state to open. */
  open: () => void;
}

/**
 * React Context containing drawer state and callback controller actions.
 */
const SidebarContext = createContext<SidebarContextType | undefined>(undefined);

/**
 * SidebarProvider component wrapping layout elements and supplying visibility states.
 *
 * @param props - Component React properties.
 * @param props.children - Node children elements.
 * @returns The context Provider element.
 */
export function SidebarProvider({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const [isOpen, setIsOpen] = useState(false);

  const toggle = () => setIsOpen((prev) => !prev);
  const close = () => setIsOpen(false);
  const open = () => setIsOpen(true);

  return (
    <SidebarContext.Provider value={{ isOpen, toggle, close, open }}>
      {children}
    </SidebarContext.Provider>
  );
}

/**
 * Custom React Hook to consume drawer status and control methods.
 *
 * @throws {Error} If called outside of a wrapping {@link SidebarProvider}.
 * @returns The active {@link SidebarContextType} drawer state.
 */
export function useSidebar() {
  const context = useContext(SidebarContext);
  if (context === undefined) {
    throw new Error("useSidebar must be used within a SidebarProvider");
  }
  return context;
}
