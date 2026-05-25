/**
 * @file useThreadManagement.ts
 * @description Hook managing conversation thread listings and creation.
 * Thread data is sourced exclusively from the backend via TanStack Query.
 * Local storage acts as a write-back cache for newly created threads only.
 */

import { useQueryClient, useQuery } from "@tanstack/react-query";
import { ChatThread } from "@/features/chat-session/types/chat.types";

const THREADS_KEY = "orasaka_threads";
const QUERY_KEY = ["chatThreads"] as const;

/**
 * Retrieves cached conversation threads from local storage.
 * Returns an empty array if the store is empty — no default seeding.
 *
 * @returns {ChatThread[]} The list of stored conversation threads.
 */
export const getStoredThreads = (): ChatThread[] => {
  if (typeof window === "undefined") return [];
  const stored = localStorage.getItem(THREADS_KEY);
  if (!stored) return [];
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
 * @param {ChatThread[]} threads - The list of threads to serialize and save.
 */
export const saveStoredThreads = (threads: ChatThread[]): void => {
  if (typeof window === "undefined") return;
  localStorage.setItem(THREADS_KEY, JSON.stringify(threads));
};

/**
 * Hook providing thread listing, creation, and cache management.
 * Thread data originates exclusively from the database via the BFF.
 *
 * @returns An object containing the threads list, loading state, and createThread handler.
 */
export function useThreadManagement() {
  const queryClient = useQueryClient();

  const threadsQuery = useQuery({
    queryKey: QUERY_KEY,
    queryFn: getStoredThreads,
  });

  const createThread = (): ChatThread => {
    const newId =
      typeof crypto !== "undefined" && typeof crypto.randomUUID === "function"
        ? crypto.randomUUID()
        : `thread-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

    const newThread: ChatThread = {
      conversationId: newId,
      title: "New Memory Block",
      updatedAt: Date.now(),
    };

    const updatedThreads = [newThread, ...getStoredThreads()];
    saveStoredThreads(updatedThreads);

    queryClient.setQueryData(QUERY_KEY, updatedThreads);
    queryClient.invalidateQueries({ queryKey: QUERY_KEY });

    return newThread;
  };

  return {
    threads: threadsQuery.data ?? [],
    isLoadingThreads: threadsQuery.isLoading,
    createThread,
  };
}
