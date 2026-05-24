"use client";

import React from "react";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useRouter } from "next/navigation";
import { Cpu, Loader2 } from "lucide-react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { OperationNode } from "./types";
import { PlaygroundNodeCard } from "./components/PlaygroundNodeCard";
import { RagSearchCard } from "./components/RagSearchCard";
import { VideoAnalysisCard } from "./components/VideoAnalysisCard";

const fetchOperationGraph = async (): Promise<OperationNode[]> => {
  const query = `
    query GetOperationGraph {
      operationGraph {
        nodes {
          id
          label
          icon
          presentationContext
          state {
            type
            reason
            lockedAt
          }
          executionDetails {
            uriPath
            httpMethod
            payloadTemplate
          }
        }
      }
    }
  `;
  const response = await fetch("/api/graphql", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ query }),
  });
  if (!response.ok) throw new Error("Failed to fetch Operation Graph");
  const result = await response.json();
  if (result.errors && result.errors.length > 0) {
    throw new Error(result.errors[0].message || "GraphQL Query Error");
  }
  return result.data?.operationGraph?.nodes || [];
};

export default function PlaygroundPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading } = useAuth();
  const queryClient = useQueryClient();

  const [nodes, setNodes] = React.useState<OperationNode[]>([]);

  React.useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isLoading, isAuthenticated, router]);

  const { data: queryData, isLoading: isLoadingGraph } = useQuery({
    queryKey: ["operationGraph"],
    queryFn: fetchOperationGraph,
    refetchOnWindowFocus: false,
    enabled: isAuthenticated,
  });

  React.useEffect(() => {
    if (queryData) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setNodes(queryData);
    }
  }, [queryData]);

  const handleNodeExecuted = () => {
    queryClient.invalidateQueries({ queryKey: ["operationGraph"] });
  };

  if (isLoading || !isAuthenticated) return null;

  return (
    <section className="flex h-screen w-screen overflow-hidden bg-zinc-50 dark:bg-zinc-950">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden min-w-0">
        <Header />
        <main className="flex-1 overflow-y-auto p-6 text-zinc-900 dark:text-zinc-100">
          <div className="max-w-6xl mx-auto space-y-8">
            <header className="flex flex-col gap-2">
              <h1 className="text-3xl font-bold tracking-tight flex items-center gap-2.5">
                <Cpu className="h-8 w-8 text-amber-500" />
                Orasaka AI Playground
              </h1>
              <p className="text-zinc-500 dark:text-zinc-400 max-w-2xl">
                Interrogate and execute the dynamic capabilities of the Orasaka
                Orchestration Engine. Blueprints are resolved in real-time from
                the Operation Graph.
              </p>
            </header>

            {isLoadingGraph ? (
              <div className="flex items-center justify-center p-12">
                <Loader2 className="h-8 w-8 animate-spin text-amber-500" />
              </div>
            ) : !Array.isArray(nodes) || nodes.length === 0 ? (
              <article className="flex flex-col items-center justify-center p-12 border border-dashed border-zinc-200 dark:border-zinc-800 rounded-xl bg-white dark:bg-zinc-900/50 space-y-4">
                <Cpu className="h-12 w-12 text-zinc-400 dark:text-zinc-600 animate-pulse" />
                <div className="text-center space-y-1">
                  <p className="font-semibold text-zinc-800 dark:text-zinc-200">
                    No Active Capabilities
                  </p>
                  <p className="text-sm text-zinc-500 dark:text-zinc-400">
                    The operation graph returned an empty or invalid dataset
                    configuration.
                  </p>
                </div>
              </article>
            ) : (
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                {nodes
                  .filter((n) => n && n.state && n.state.type !== "INVISIBLE")
                  .map((node) => (
                    <PlaygroundNodeCard
                      key={node.id}
                      node={node}
                      onExecuted={handleNodeExecuted}
                    />
                  ))}

                <RagSearchCard />
                <VideoAnalysisCard />
              </div>
            )}
          </div>
        </main>
      </div>
    </section>
  );
}
