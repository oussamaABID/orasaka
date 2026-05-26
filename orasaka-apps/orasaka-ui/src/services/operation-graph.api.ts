/**
 * @file operation-graph.api.ts
 * @description Stateless outbound adapter for the Operation Graph capability query.
 * Consolidates the previously duplicated `fetchOperationGraph` implementations
 * from `ContextPlusMenu.tsx` and `playground/page.tsx`.
 */

import { graphqlRequest } from "./graphql-client";
import type { OperationNode } from "@/features/playground/types/playground.types";

interface OperationGraphData {
  operationGraph: { nodes: OperationNode[] };
}

const OPERATION_GRAPH_QUERY = `
  query GetOperationGraph {
    operationGraph {
      nodes {
        id
        label
        icon
        presentationContext
        state { type reason lockedAt }
        executionDetails { uriPath httpMethod payloadTemplate }
      }
    }
  }
`;

/**
 * Stateless adapter exposing operation graph network operations.
 */
export const OperationGraphApi = {
  /**
   * Fetches all visible operation nodes from the orchestration engine.
   *
   * @returns A promise resolving to the array of operation nodes.
   */
  fetchNodes: async (): Promise<OperationNode[]> => {
    const data = await graphqlRequest<OperationGraphData>(
      OPERATION_GRAPH_QUERY,
    );
    return data.operationGraph?.nodes ?? [];
  },
} as const;
