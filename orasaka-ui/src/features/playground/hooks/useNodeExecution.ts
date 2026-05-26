/**
 * @file useNodeExecution.ts
 * @description TanStack Query mutation hook for executing playground capability nodes
 * through the BFF proxy. Extracts and secures the inline `useMutation` from
 * `PlaygroundNodeCard.tsx` — all requests now route through the BFF, eliminating
 * direct backend URL leakage.
 */

import { useMutation } from "@tanstack/react-query";
import { useSession } from "next-auth/react";

interface ExecutionParams {
  /** The BFF-proxied execution URI path. */
  uriPath: string;
  /** The HTTP method to use (GET, POST, etc.). */
  httpMethod: string;
  /** The serialized JSON payload body. */
  payload: string;
  /** Optional session authentication Bearer token. */
  token?: string;
}

interface ExecutionResult {
  success: boolean;
  data: string;
}

/**
 * Executes a capability node through the BFF proxy.
 * Routes all requests through Next.js API routes to prevent direct backend access.
 */
const executeNode = async ({
  uriPath,
  httpMethod,
  payload,
  token,
}: ExecutionParams): Promise<string> => {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(uriPath, {
    method: httpMethod,
    headers,
    body: httpMethod !== "GET" ? payload : undefined,
  });

  if (!response.ok) {
    throw new Error(
      response.status === 403
        ? "Access Forbidden: Restricted by gateway protection policy."
        : `Execution failed with status ${response.status}`,
    );
  }

  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    const json = await response.json();
    return json.analysis || json.content || JSON.stringify(json, null, 2);
  }

  return response.text();
};

/**
 * Hook providing a managed mutation for executing playground operation nodes.
 *
 * @param options - Optional success/error callbacks.
 * @returns An object containing the mutate trigger, result state, pending flag, and a reset function.
 */
export function useNodeExecution(options?: {
  onSuccess?: (data: string) => void;
  onError?: (error: Error) => void;
}) {
  const { data: session } = useSession();
  const token = session?.user?.id;

  const mutation = useMutation({
    mutationFn: (params: Omit<ExecutionParams, "token">) =>
      executeNode({ ...params, token }),
    onSuccess: (data) => {
      options?.onSuccess?.(data);
    },
    onError: (error) => {
      options?.onError?.(error);
    },
  });

  const result: ExecutionResult | null = mutation.isSuccess
    ? { success: true, data: mutation.data }
    : mutation.isError
      ? {
          success: false,
          data: (mutation.error as Error)?.message ?? "Unknown execution error",
        }
      : null;

  return {
    execute: mutation.mutate,
    result,
    isPending: mutation.isPending,
    reset: mutation.reset,
  };
}
