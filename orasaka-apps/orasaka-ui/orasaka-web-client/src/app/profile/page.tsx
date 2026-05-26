"use client";

import * as React from "react";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { ProfileView } from "@/features/profile/components/ProfileView";
import { useAuth } from "@/core/hooks/useAuth";
import { useRouter } from "next/navigation";

/**
 * ProfilePage component rendering the user's profile view.
 *
 * @returns The profile page layout.
 */
export default function ProfilePage() {
  const router = useRouter();
  const { isAuthenticated, isLoading } = useAuth();

  React.useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isLoading, isAuthenticated, router]);

  if (isLoading || !isAuthenticated) return null;

  return (
    <section className="flex h-screen overflow-hidden bg-[var(--surface-0)] transition-colors duration-200">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Header />
        <main className="flex-1 overflow-auto p-6 scrollbar-thin ambient-grid">
          <div className="mx-auto max-w-5xl animate-in fade-in slide-in-from-bottom-3 duration-300">
            <ProfileView />
          </div>
        </main>
      </div>
    </section>
  );
}
