/**
 * @file media.api.test.ts
 * @description Tests for MediaApi — video generation, job status, and media analysis.
 */

import { MediaApi } from "../../services/media.api";
import { ApiClient } from "../../services/api-client";

jest.mock("../../services/api-client");

const mockRequestRest = ApiClient.requestRest as jest.MockedFunction<typeof ApiClient.requestRest>;

beforeEach(() => jest.clearAllMocks());

describe("MediaApi", () => {
  test("generateVideo calls POST /api/v1/ai/video with payload", async () => {
    const response = { jobId: "job-1", status: "PENDING" };
    mockRequestRest.mockResolvedValue(response);

    const result = await MediaApi.generateVideo("sunset", 5, "model-x");

    expect(mockRequestRest).toHaveBeenCalledWith({
      method: "POST",
      path: "/api/v1/ai/video",
      body: { prompt: "sunset", durationSeconds: 5, model: "model-x", settings: {} },
    });
    expect(result).toEqual(response);
  });

  test("getJobStatus calls GET /api/v1/jobs/:id", async () => {
    const job = { id: "job-1", userId: "u1", featureKey: "video", status: "COMPLETED" };
    mockRequestRest.mockResolvedValue(job);

    const result = await MediaApi.getJobStatus("job-1");

    expect(mockRequestRest).toHaveBeenCalledWith({
      method: "GET",
      path: "/api/v1/jobs/job-1",
    });
    expect(result).toEqual(job);
  });

  test("analyzeImage calls POST with assetId and prompt", async () => {
    const response = { jobId: "job-2", status: "PENDING" };
    mockRequestRest.mockResolvedValue(response);

    const result = await MediaApi.analyzeImage("describe", "asset-1", "vision-model");

    expect(mockRequestRest).toHaveBeenCalledWith({
      method: "POST",
      path: "/api/v1/media/analyze-image",
      body: { prompt: "describe", assetId: "asset-1", model: "vision-model" },
    });
    expect(result).toEqual(response);
  });

  test("analyzeAudio calls POST with assetId and threadId", async () => {
    const response = { jobId: "job-3", status: "PENDING" };
    mockRequestRest.mockResolvedValue(response);

    const result = await MediaApi.analyzeAudio("asset-2", "thread-1", "whisper");

    expect(mockRequestRest).toHaveBeenCalledWith({
      method: "POST",
      path: "/api/v1/media/analyze-audio",
      body: { assetId: "asset-2", threadId: "thread-1", model: "whisper" },
    });
    expect(result).toEqual(response);
  });
});
