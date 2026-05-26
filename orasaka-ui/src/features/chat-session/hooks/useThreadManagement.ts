/**
 * @file useThreadManagement.ts
 * @description Hook managing conversation thread listings and creation.
 * Sourced from the backend via TanStack Query.
 */

import { useQueryClient, useQuery, useMutation } from "@tanstack/react-query";
import { parseISO } from "date-fns";
import { ChatThread } from "@/features/chat-session/types/chat.types";
import { useAuth } from "@/features/auth/hooks/useAuth";

/**
 * Retrieves cached conversation threads from local storage.
 * Returns an empty array if the store is empty — no default seeding.
 *
 * @param {string} userId - The active authenticated user ID.
 * @returns {ChatThread[]} The list of stored conversation threads.
 */
export const getStoredThreads = (userId: string): ChatThread[] => {
  if (typeof window === "undefined") return [];
  const key = `orasaka_threads_${userId}`;
  const stored = localStorage.getItem(key);
  if (!stored) {
    return [];
  }
  try {
    return JSON.parse(stored);
  } catch (e) {
    console.error("Failed to parse stored threads:", e);
    return [];
  }
};

/**
 * Persists the current list of conversation threads in local storage.
 *
 * @param {string} userId - The active authenticated user ID.
 * @param {ChatThread[]} threads - The list of threads to serialize and save.
 */
export const saveStoredThreads = (
  userId: string,
  threads: ChatThread[],
): void => {
  if (typeof window === "undefined") return;
  localStorage.setItem(`orasaka_threads_${userId}`, JSON.stringify(threads));
};

/**
 * Hook providing thread listing, creation, and cache management.
 * Thread data originates exclusively from the database via the BFF.
 */
export function useThreadManagement() {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const userId = user?.id || user?.email || "anonymous";
  const queryKey = ["chatThreads", userId] as const;

  const threadsQuery = useQuery({
    queryKey,
    queryFn: async (): Promise<ChatThread[]> => {
      try {
        const res = await fetch("/api/v1/chats");
        if (!res.ok) {
          return getStoredThreads(userId);
        }
        const data = await res.json();
        const backendThreads = data.map(
          (session: {
            id: string;
            title: string;
            updatedAt: string | number;
          }) => ({
            conversationId: session.id,
            title: session.title,
            updatedAt: parseISO(session.updatedAt as string).getTime(),
          }),
        );
        saveStoredThreads(userId, backendThreads);
        return backendThreads;
      } catch (e) {
        console.error("Failed to fetch threads:", e);
        return getStoredThreads(userId);
      }
    },
  });

  const createThreadMutation = useMutation({
    mutationFn: async (): Promise<ChatThread> => {
      const newId =
        typeof crypto !== "undefined" && typeof crypto.randomUUID === "function"
          ? crypto.randomUUID()
          : `thread-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

      const res = await fetch("/api/v1/chats", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          conversationId: newId,
          title: "New Memory Block",
        }),
      });
      if (!res.ok) throw new Error("Failed to create thread");
      const session = await res.json();
      return {
        conversationId: session.id,
        title: session.title,
        updatedAt: parseISO(session.updatedAt as string).getTime(),
      };
    },
    onSuccess: (newThread) => {
      const cached =
        queryClient.getQueryData<ChatThread[]>(queryKey) ||
        getStoredThreads(userId);
      const updatedThreads = [newThread, ...cached];
      saveStoredThreads(userId, updatedThreads);
      queryClient.setQueryData<ChatThread[]>(queryKey, updatedThreads);
      queryClient.invalidateQueries({ queryKey });
    },
  });

  const renameThreadMutation = useMutation({
    mutationFn: async ({
      id,
      title,
    }: {
      id: string;
      title: string;
    }): Promise<ChatThread> => {
      const res = await fetch(`/api/v1/chats/${id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title }),
      });
      if (!res.ok) throw new Error("Failed to rename thread");
      const session = await res.json();
      return {
        conversationId: session.id,
        title: session.title,
        updatedAt: parseISO(session.updatedAt as string).getTime(),
      };
    },
    onSuccess: (updatedThread) => {
      const cached =
        queryClient.getQueryData<ChatThread[]>(queryKey) ||
        getStoredThreads(userId);
      const updatedThreads = cached.map((t) =>
        t.conversationId === updatedThread.conversationId ? updatedThread : t,
      );
      saveStoredThreads(userId, updatedThreads);
      queryClient.setQueryData<ChatThread[]>(queryKey, updatedThreads);
      queryClient.invalidateQueries({ queryKey });
    },
  });

  const deleteThreadMutation = useMutation({
    mutationFn: async (id: string): Promise<void> => {
      const res = await fetch(`/api/v1/chats/${id}`, {
        method: "DELETE",
      });
      if (!res.ok) throw new Error("Failed to delete thread");
    },
    onSuccess: (_, id) => {
      const cached =
        queryClient.getQueryData<ChatThread[]>(queryKey) ||
        getStoredThreads(userId);
      const updatedThreads = cached.filter((t) => t.conversationId !== id);
      saveStoredThreads(userId, updatedThreads);
      localStorage.removeItem(`orasaka_messages_${userId}_${id}`);
      queryClient.setQueryData<ChatThread[]>(queryKey, updatedThreads);
      queryClient.invalidateQueries({ queryKey });
    },
  });

  return {
    threads: threadsQuery.data ?? [],
    isLoadingThreads: threadsQuery.isLoading,
    createThread: async (): Promise<ChatThread> => {
      return createThreadMutation.mutateAsync();
    },
    renameThread: async (id: string, title: string): Promise<ChatThread> => {
      return renameThreadMutation.mutateAsync({ id, title });
    },
    deleteThread: async (id: string): Promise<void> => {
      return deleteThreadMutation.mutateAsync(id);
    },
  };
}
