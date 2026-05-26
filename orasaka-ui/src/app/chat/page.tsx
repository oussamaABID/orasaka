"use client";

import React, { Suspense } from "react";
import { ChatWindow } from "@/features/chat-session/components/ChatWindow";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useRouter, useSearchParams } from "next/navigation";

function ChatContent() {
  const searchParams = useSearchParams();
  const conversationId = searchParams.get("conversationId") || "";

  return (
    <ChatWindow key={conversationId} initialConversationId={conversationId} />
  );
}

/**
 * ChatPage component providing the main chat interface within the workspace.
 *
 * @returns The rendered React element for the chat page.
 */
export default function ChatPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading } = useAuth();

  React.useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isLoading, isAuthenticated, router]);

  if (isLoading || !isAuthenticated) return null;

  return (
    <section className="flex h-screen w-screen overflow-hidden bg-background">
      <Sidebar />

      <div className="flex flex-1 flex-col overflow-hidden min-w-0">
        <Header />

        <main className="flex-1 flex flex-col overflow-hidden bg-background">
          <Suspense
            fallback={
              <output className="p-6 text-zinc-500 text-sm animate-pulse">
                Loading chat...
              </output>
            }
          >
            <ChatContent />
          </Suspense>
        </main>
      </div>
    </section>
  );
}
