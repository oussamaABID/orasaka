/**
 * @file useOperationGraph.ts
 * @description TanStack Query hook for consuming the Operation Graph.
 * Consolidates the duplicated query logic from `playground/page.tsx`
 * and `ChatWindow.tsx` into a single reusable hook.
 */

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { OperationGraphApi } from "@/services/operation-graph.api";

const QUERY_KEY = ["operationGraph"] as const;

/**
 * Hook providing cached, reactive access to the operation graph nodes.
 *
 * @param enabled - Whether to enable the query (typically bound to auth state).
 * @returns An object with nodes, loading state, error, and an invalidation trigger.
 */
export function useOperationGraph(enabled = true) {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: QUERY_KEY,
    queryFn: OperationGraphApi.fetchNodes,
    refetchOnWindowFocus: false,
    enabled,
  });

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: QUERY_KEY });

  return {
    nodes: query.data ?? [],
    isLoading: query.isLoading,
    error: query.error,
    invalidate,
  };
}
