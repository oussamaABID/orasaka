"use client";

import React from "react";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useRouter } from "next/navigation";
import { Cpu, Loader2 } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { OperationNode } from "./types";
import { PlaygroundNodeCard } from "./components/PlaygroundNodeCard";
import { RagSearchCard } from "./components/RagSearchCard";

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
  return result.data.operationGraph.nodes;
};

export default function PlaygroundPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading } = useAuth();

  React.useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isLoading, isAuthenticated, router]);

  const { data: nodes = [], isLoading: isLoadingGraph } = useQuery({
    queryKey: ["operationGraph"],
    queryFn: fetchOperationGraph,
    refetchOnWindowFocus: false,
    enabled: isAuthenticated,
  });

  if (isLoading || !isAuthenticated) return null;

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-zinc-50 dark:bg-zinc-950">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden min-w-0">
        <Header />
        <main className="flex-1 overflow-y-auto p-6 text-zinc-900 dark:text-zinc-100">
          <div className="max-w-6xl mx-auto space-y-8">
            <div className="flex flex-col gap-2">
              <h1 className="text-3xl font-bold tracking-tight flex items-center gap-2.5">
                <Cpu className="h-8 w-8 text-amber-500" />
                Orasaka AI Playground
              </h1>
              <p className="text-zinc-500 dark:text-zinc-400 max-w-2xl">
                Interrogate and execute the dynamic capabilities of the Orasaka
                Orchestration Engine. Blueprints are resolved in real-time from
                the Operation Graph.
              </p>
            </div>

            {isLoadingGraph ? (
              <div className="flex items-center justify-center p-12">
                <Loader2 className="h-8 w-8 animate-spin text-amber-500" />
              </div>
            ) : (
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                {nodes
                  .filter((n) => n.state.type !== "INVISIBLE")
                  .map((node) => (
                    <PlaygroundNodeCard key={node.id} node={node} />
                  ))}

                <RagSearchCard />
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}
