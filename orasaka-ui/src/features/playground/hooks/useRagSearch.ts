/**
 * @file useRagSearch.ts
 * @description Passive RAG search orchestration hook.
 * Stores search parameters and output results globally in JobStreamContext to survive route unmounts.
 */

import { useJobStream } from "@/features/jobs/context/JobStreamContext";
import { MediaApi } from "@/services/media.api";

/**
 * Hook providing a mutation for running RAG searches against semantic indexes.
 *
 * @returns An object containing the mutate trigger, loading state, error, and result data.
 */
export function useRagSearch() {
  const {
    ragResult,
    setRagResult,
    ragIsPending,
    setRagIsPending,
    ragError,
    setRagError,
  } = useJobStream();

  const search = async (query: string) => {
    setRagIsPending(true);
    setRagError(null);
    setRagResult(null);

    try {
      const contextStr = await MediaApi.searchRag(query);
      setRagResult(contextStr);
    } catch (err) {
      setRagError(
        err instanceof Error
          ? err.message
          : "An unexpected search error occurred.",
      );
    } finally {
      setRagIsPending(false);
    }
  };

  const reset = () => {
    setRagResult(null);
    setRagError(null);
  };

  return {
    search,
    result: ragResult,
    isPending: ragIsPending,
    error: ragError,
    reset,
  };
}
