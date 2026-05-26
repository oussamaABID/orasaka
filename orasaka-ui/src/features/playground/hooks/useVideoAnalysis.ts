import { MediaApi } from "@/services/media.api";
import type { VideoAnalysisResult } from "@/services/media.api";
import { useJobStream } from "@/features/jobs/context/JobStreamContext";
import { JOB_STATUS } from "@/core/constants/http.constants";

export type AnalysisState =
  | "idle"
  | "uploading"
  | "processing"
  | "success"
  | "error";

/**
 * Hook to manage video upload and keyframe/transcript extraction state.
 */
export function useVideoAnalysis() {
  const {
    videoAnalysisJobId,
    setVideoAnalysisJobId,
    videoAnalysisIsUploading,
    setVideoAnalysisIsUploading,
    videoAnalysisError,
    setVideoAnalysisError,
    jobs,
    refreshJobs,
  } = useJobStream();

  const isUploading = videoAnalysisIsUploading;
  const setIsUploading = setVideoAnalysisIsUploading;
  const localError = videoAnalysisError;
  const setLocalError = setVideoAnalysisError;

  const analyze = async (file: File, model: string) => {
    setIsUploading(true);
    setLocalError(null);
    setVideoAnalysisJobId(null);

    try {
      // Step 1: Upload raw video file
      const uploadRes = await MediaApi.uploadMedia(file);

      // Step 2: Trigger analysis job with asset ID and selected model
      const res = await MediaApi.analyzeVideo(uploadRes.assetId, model);
      setVideoAnalysisJobId(res.jobId);
      await refreshJobs();
    } catch (err) {
      setLocalError(
        err instanceof Error
          ? err.message
          : "An unexpected upload or analysis error occurred.",
      );
    } finally {
      setIsUploading(false);
    }
  };

  const reset = () => {
    setVideoAnalysisJobId(null);
    setLocalError(null);
  };

  let state: AnalysisState = "idle";
  let result: VideoAnalysisResult | null = null;
  let error: string | null = localError;

  if (isUploading) {
    state = "uploading";
  } else if (videoAnalysisJobId) {
    const job = jobs.find((j) => j.id === videoAnalysisJobId);
    if (!job) {
      state = "processing";
    } else if (job.status === JOB_STATUS.PENDING || job.status === JOB_STATUS.PROCESSING) {
      state = "processing";
    } else if (job.status === JOB_STATUS.COMPLETED) {
      state = "success";
      const jobResult = job.result as Record<string, unknown> | null;
      result = {
        transcript: (jobResult?.transcript as string) ?? "",
        keyframeCount: (jobResult?.keyframeCount as number) ?? 0,
      };
    } else if (job.status === JOB_STATUS.FAILED) {
      state = "error";
      error = job.errorMessage || "Video analysis failed.";
    }
  }

  return {
    analyze,
    state,
    result,
    error,
    reset,
  };
}
