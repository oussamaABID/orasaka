"use client";

import React, {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  useRef,
} from "react";
import { useAuth } from "@/core/hooks/useAuth";
import type { Job } from "@/core/types/jobs.types";
import { JobsApi } from "@/services/jobs.api";
import { ToastOverlay } from "@/core/components/ToastOverlay";
import type { Toast } from "@/core/components/ToastOverlay";
import { parseISO } from "date-fns";
import { useChatStreamClient } from "@/core/hooks/useChatStreamClient";
import { useJobSSE } from "@/core/hooks/useJobSSE";
import { JOB_STATUS } from "@/core/constants/http.constants";

interface JobStreamContextType {
  jobs: Job[];
  activeJobsCount: number;
  lastJobs: Job[];
  toasts: Toast[];
  removeToast: (id: string) => void;
  refreshJobs: () => Promise<void>;
  activeConversationId: string;
  setActiveConversationId: (id: string) => void;
  playgroundInputs: Record<string, Record<string, string>>;
  setPlaygroundInput: (nodeId: string, field: string, value: string) => void;
  playgroundResults: Record<string, unknown>;
  setPlaygroundResult: (nodeId: string, result: unknown) => void;
  activeJobIdByNodeId: Record<string, string>;
  setActiveJobIdForNode: (nodeId: string, jobId: string | null) => void;
  videoAnalysisJobId: string | null;
  setVideoAnalysisJobId: (jobId: string | null) => void;
  chatInput: string;
  setChatInput: (val: string) => void;
  isChatStreaming: boolean;
  startChatStream: (
    conversationId: string,
    prompt: string,
    assetIds?: string[],
  ) => void;
  stopChatStream: () => void;
  videoAnalysisIsUploading: boolean;
  setVideoAnalysisIsUploading: (val: boolean) => void;
  videoAnalysisError: string | null;
  setVideoAnalysisError: (val: string | null) => void;
  ragQuery: string;
  setRagQuery: (val: string) => void;
  ragResult: string | null;
  setRagResult: (val: string | null) => void;
  ragIsPending: boolean;
  setRagIsPending: (val: boolean) => void;
  ragError: string | null;
  setRagError: (val: string | null) => void;
  jobProgress: Record<string, number>;
}

const JobStreamContext = createContext<JobStreamContextType | undefined>(
  undefined,
);

let toastIdCounter = 0;
const generateToastId = () => {
  toastIdCounter += 1;
  return `toast-${toastIdCounter}`;
};

/**
 * Global React Context Provider establishing a persistent EventSource stream
 * pointing to the BFF proxy. Tracks and broadcasts all async task progress.
 */
export function JobStreamProvider({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const { isAuthenticated, user } = useAuth();
  const [jobs, setJobs] = useState<Job[]>([]);
  const [toasts, setToasts] = useState<Toast[]>([]);
  const [jobProgress, setJobProgress] = useState<Record<string, number>>({});
  const jobsRef = useRef<Job[]>([]);
  useEffect(() => {
    jobsRef.current = jobs;
  }, [jobs]);

  const [activeConversationId, setActiveConversationId] = useState<string>("");

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setActiveConversationId("");
  }, [user?.id, isAuthenticated]);

  const [playgroundInputs, setPlaygroundInputs] = useState<
    Record<string, Record<string, string>>
  >({});
  const [playgroundResults, setPlaygroundResults] = useState<
    Record<string, unknown>
  >({});
  const [activeJobIdByNodeId, setActiveJobIdByNodeId] = useState<
    Record<string, string>
  >({});
  const [videoAnalysisJobId, setVideoAnalysisJobId] = useState<string | null>(
    null,
  );

  const [chatInput, setChatInput] = useState<string>("");
  const { isChatStreaming, startChatStream, stopChatStream } =
    useChatStreamClient();

  const [videoAnalysisIsUploading, setVideoAnalysisIsUploading] =
    useState<boolean>(false);
  const [videoAnalysisError, setVideoAnalysisError] = useState<string | null>(
    null,
  );

  const [ragQuery, setRagQuery] = useState<string>("");
  const [ragResult, setRagResult] = useState<string | null>(null);
  const [ragIsPending, setRagIsPending] = useState<boolean>(false);
  const [ragError, setRagError] = useState<string | null>(null);

  const activeJobsCount = jobs.filter(
    (j) => j.status === JOB_STATUS.PENDING || j.status === JOB_STATUS.PROCESSING,
  ).length;

  const lastJobs = [...jobs]
    .sort(
      (a, b) =>
        parseISO(b.createdAt).getTime() - parseISO(a.createdAt).getTime(),
    )
    .slice(0, 5);

  const removeToast = React.useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const addToast = React.useCallback(
    (message: string, type: "info" | "success" | "error") => {
      const id = generateToastId();
      setToasts((prev) => [...prev, { id, message, type }]);
      setTimeout(() => {
        removeToast(id);
      }, 5000);
    },
    [removeToast],
  );

  const setPlaygroundInput = React.useCallback(
    (nodeId: string, field: string, value: string) => {
      setPlaygroundInputs((prev) => ({
        ...prev,
        [nodeId]: {
          ...prev[nodeId],
          [field]: value,
        },
      }));
    },
    [],
  );

  const setPlaygroundResult = React.useCallback(
    (nodeId: string, result: unknown) => {
      setPlaygroundResults((prev) => ({
        ...prev,
        [nodeId]: result,
      }));
    },
    [],
  );

  const setActiveJobIdForNode = React.useCallback(
    (nodeId: string, jobId: string | null) => {
      setActiveJobIdByNodeId((prev) => {
        if (jobId === null) {
          const next = { ...prev };
          delete next[nodeId];
          return next;
        }
        return {
          ...prev,
          [nodeId]: jobId,
        };
      });
    },
    [],
  );

  const fetchJobs = React.useCallback(async () => {
    if (!isAuthenticated) return;
    try {
      const pageData = await JobsApi.fetchPage(0, 20);
      setJobs(pageData.content);
      const initialProgress: Record<string, number> = {};
      pageData.content.forEach((job) => {
        if (job.progress !== undefined) {
          initialProgress[job.id] = job.progress;
        }
      });
      setJobProgress(initialProgress);
    } catch (err) {
      console.error("Failed to load jobs history", err);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (isAuthenticated) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      fetchJobs();
    } else {
       
      setJobs([]);
    }
  }, [isAuthenticated, fetchJobs]);

  const hasActiveJobs = activeJobsCount > 0;

  const handleJobUpdate = React.useCallback((updatedJob: Job) => {
    setJobs((prev) => {
      const index = prev.findIndex((j) => j.id === updatedJob.id);
      if (index !== -1) {
        const updated = [...prev];
        updated[index] = updatedJob;
        return updated;
      }
      return [updatedJob, ...prev];
    });
  }, []);

  const handleJobProgress = React.useCallback(
    (jobId: string, progress: number) => {
      setJobs((prev) => {
        const index = prev.findIndex((j) => j.id === jobId);
        if (index !== -1) {
          const updated = [...prev];
          updated[index] = { ...updated[index], progress };
          return updated;
        }
        return prev;
      });
      setJobProgress((prev) => ({ ...prev, [jobId]: progress }));
    },
    [],
  );

  useJobSSE(isAuthenticated, hasActiveJobs, {
    onJobUpdate: handleJobUpdate,
    onJobProgress: handleJobProgress,
    onToast: addToast,
    getJobs: () => jobsRef.current,
  });

  const contextValue = useMemo<JobStreamContextType>(() => ({
    jobs,
    activeJobsCount,
    lastJobs,
    toasts,
    removeToast,
    refreshJobs: fetchJobs,
    activeConversationId,
    setActiveConversationId,
    playgroundInputs,
    setPlaygroundInput,
    playgroundResults,
    setPlaygroundResult,
    activeJobIdByNodeId,
    setActiveJobIdForNode,
    videoAnalysisJobId,
    setVideoAnalysisJobId,
    chatInput,
    setChatInput,
    isChatStreaming,
    startChatStream,
    stopChatStream,
    videoAnalysisIsUploading,
    setVideoAnalysisIsUploading,
    videoAnalysisError,
    setVideoAnalysisError,
    ragQuery,
    setRagQuery,
    ragResult,
    setRagResult,
    ragIsPending,
    setRagIsPending,
    ragError,
    setRagError,
    jobProgress,
  }), [
    jobs, activeJobsCount, lastJobs, toasts, removeToast, fetchJobs,
    activeConversationId, playgroundInputs, setPlaygroundInput,
    playgroundResults, setPlaygroundResult, activeJobIdByNodeId,
    setActiveJobIdForNode, videoAnalysisJobId, chatInput, isChatStreaming,
    startChatStream, stopChatStream, videoAnalysisIsUploading,
    videoAnalysisError, ragQuery, ragResult, ragIsPending, ragError,
    jobProgress,
  ]);

  return (
    <JobStreamContext.Provider value={contextValue}>
      {children}
      <ToastOverlay toasts={toasts} onRemoveToast={removeToast} />
    </JobStreamContext.Provider>
  );
}

export function useJobStream() {
  const context = useContext(JobStreamContext);
  if (context === undefined) {
    throw new Error("useJobStream must be used within a JobStreamProvider");
  }
  return context;
}
