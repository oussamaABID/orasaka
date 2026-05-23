"use client";

import * as React from "react";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { ProfileView } from "@/features/profile/components/ProfileView";
import { useAuth } from "@/features/auth/hooks/useAuth";
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
    <div className="flex h-screen overflow-hidden bg-zinc-50 dark:bg-zinc-950">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Header />
        <main className="flex-1 overflow-auto p-6">
          <ProfileView />
        </main>
      </div>
    </div>
  );
}
