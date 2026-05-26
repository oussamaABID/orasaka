"use client";

import { useEffect, useRef, useCallback } from "react";
import type { Job } from "@/features/jobs/types/jobs.types";
import { resolveFeatureLabel } from "@/core/constants/capability.constants";

interface JobSSECallbacks {
  onJobUpdate: (job: Job) => void;
  onJobProgress: (jobId: string, progress: number) => void;
  onToast: (message: string, type: "info" | "success" | "error") => void;
  getJobs: () => Job[];
}

/**
 * Custom hook that establishes and manages the SSE connection for job status streaming.
 * Handles reconnection with exponential backoff and automatic cleanup.
 *
 * @param isAuthenticated Whether the user is authenticated.
 * @param hasActiveJobs Whether there are active jobs to monitor.
 * @param callbacks Event handlers for job updates, progress, and toast notifications.
 */
export function useJobSSE(
  isAuthenticated: boolean,
  hasActiveJobs: boolean,
  callbacks: JobSSECallbacks,
) {
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const reconnectDelayRef = useRef<number>(1000);
  const eventSourceRef = useRef<EventSource | null>(null);
  const callbacksRef = useRef(callbacks);

  useEffect(() => {
    callbacksRef.current = callbacks;
  }, [callbacks]);

  const resolveFeatureName = useCallback(
    (featureKey: string): string => resolveFeatureLabel(featureKey),
    [],
  );

  useEffect(() => {
    if (!isAuthenticated || !hasActiveJobs) {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
      return;
    }

    function connectSSE() {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }

      const es = new EventSource("/api/v1/jobs/stream");
      eventSourceRef.current = es;

      es.onopen = () => {
        console.log("[useJobSSE] SSE Connection established");
        reconnectDelayRef.current = 1000;
      };

      es.addEventListener("job-status", (event: MessageEvent) => {
        try {
          const updatedJob: Job = JSON.parse(event.data);
          callbacksRef.current.onJobUpdate(updatedJob);

          const featureName = resolveFeatureName(updatedJob.featureKey);

          if (updatedJob.status === "PROCESSING") {
            callbacksRef.current.onToast(
              `⚡ Task [${featureName}] has started processing`,
              "info",
            );
          } else if (updatedJob.status === "COMPLETED") {
            callbacksRef.current.onJobProgress(updatedJob.id, 100);
            callbacksRef.current.onToast(
              `🎉 ${featureName} Completed successfully!`,
              "success",
            );
          } else if (updatedJob.status === "FAILED") {
            callbacksRef.current.onJobProgress(updatedJob.id, 0);
            callbacksRef.current.onToast(
              `❌ ${featureName} Failed: ${updatedJob.errorMessage || "Unknown error"}`,
              "error",
            );
          }

          if (["COMPLETED", "FAILED"].includes(updatedJob.status)) {
            const otherActive = callbacksRef.current
              .getJobs()
              .filter(
                (j) =>
                  j.id !== updatedJob.id &&
                  (j.status === "PENDING" || j.status === "PROCESSING"),
              );
            if (otherActive.length === 0) {
              console.log(
                "[useJobSSE] All active tasks completed. Closing SSE connection.",
              );
              if (eventSourceRef.current) {
                eventSourceRef.current.close();
                eventSourceRef.current = null;
              }
            }
          }
        } catch (err) {
          console.error("Failed to parse job SSE event data", err);
        }
      });

      es.addEventListener("job-progress", (event: MessageEvent) => {
        try {
          const progressPayload: { jobId: string; progress: number } =
            JSON.parse(event.data);
          callbacksRef.current.onJobProgress(
            progressPayload.jobId,
            progressPayload.progress,
          );
        } catch (err) {
          console.error("Failed to parse job progress SSE event data", err);
        }
      });

      es.onerror = (err) => {
        console.error("SSE stream connection error, reconnecting...", err);
        es.close();
        eventSourceRef.current = null;

        const nextDelay = Math.min(reconnectDelayRef.current * 2, 30000);
        reconnectDelayRef.current = nextDelay;

        reconnectTimeoutRef.current = setTimeout(() => {
          connectSSE();
        }, reconnectDelayRef.current);
      };
    }

    connectSSE();

    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
    };
  }, [isAuthenticated, hasActiveJobs, resolveFeatureName]);
}
