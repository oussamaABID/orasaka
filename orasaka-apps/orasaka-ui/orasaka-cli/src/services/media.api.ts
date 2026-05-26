/**
 * @file media.api.ts
 * @description Outbound adapter service for REST-based Video Generation and Media Analysis.
 */

import { ApiClient } from "./api-client";

export interface VideoResponse {
  readonly format: string;
  readonly url: string;
}

export interface VideoSubmitResponse {
  readonly jobId: string;
  readonly status: string;
}

export interface JobResponse {
  readonly id: string;
  readonly userId: string;
  readonly featureKey: string;
  readonly status: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
  readonly result?: { url?: string; [key: string]: unknown };
  readonly errorMessage?: string;
}

export interface MediaAnalysisResponse {
  readonly analysis: string;
}

export const MediaApi = {
  generateVideo: async (prompt: string, durationSeconds: number, model?: string): Promise<VideoSubmitResponse> => {
    return ApiClient.requestRest<VideoSubmitResponse>({
      method: "POST",
      path: "/api/v1/ai/video",
      body: {
        prompt,
        durationSeconds,
        model,
        settings: {},
      },
    });
  },

  /**
   * Fetches the current execution status of an asynchronous job.
   */
  getJobStatus: async (jobId: string): Promise<JobResponse> => {
    return ApiClient.requestRest<JobResponse>({
      method: "GET",
      path: `/api/v1/jobs/${jobId}`,
    });
  },

  /**
   * Submits the uploaded image asset ID for analysis.
   */
  analyzeImage: async (prompt: string, assetId: string, model?: string): Promise<VideoSubmitResponse> => {
    return ApiClient.requestRest<VideoSubmitResponse>({
      method: "POST",
      path: "/api/v1/media/analyze-image",
      body: {
        prompt,
        assetId,
        model,
      },
    });
  },

  /**
   * Submits the uploaded audio asset ID for analysis.
   */
  analyzeAudio: async (assetId: string, threadId: string, model?: string): Promise<VideoSubmitResponse> => {
    return ApiClient.requestRest<VideoSubmitResponse>({
      method: "POST",
      path: "/api/v1/media/analyze-audio",
      body: {
        assetId,
        threadId,
        model,
      },
    });
  },
} as const;
