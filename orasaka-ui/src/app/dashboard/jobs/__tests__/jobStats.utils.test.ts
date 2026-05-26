/**
 * @file jobStats.utils.test.ts
 * @description Tests for job statistics computation and duration formatting.
 */

import { computeJobStats, formatMs } from "../jobStats.utils";
import type { Job } from "@/features/jobs/types/jobs.types";

// ── Factory ──────────────────────────────────────────────────────────────────

function makeJob(overrides: Partial<Job> = {}): Job {
  return {
    id: "job-1",
    userId: "user-1",
    featureKey: "ai.chat.stream",
    status: "COMPLETED",
    createdAt: "2025-01-01T10:00:00.000Z",
    updatedAt: "2025-01-01T10:00:05.000Z",
    payload: { model: "gpt-4o" },
    result: null,
    ...overrides,
  } as Job;
}

// ── formatMs ─────────────────────────────────────────────────────────────────

describe("formatMs", () => {
  test("returns dash for zero", () => {
    expect(formatMs(0)).toBe("—");
  });

  test("formats sub-second durations as ms", () => {
    expect(formatMs(450)).toBe("450ms");
  });

  test("formats seconds with tenths", () => {
    expect(formatMs(3500)).toBe("3.5s");
  });

  test("formats minutes and seconds", () => {
    expect(formatMs(125_000)).toBe("2m 5s");
  });

  test("formats hours and minutes", () => {
    expect(formatMs(3_661_000)).toBe("1h 1m");
  });
});

// ── computeJobStats ──────────────────────────────────────────────────────────

describe("computeJobStats", () => {
  test("returns zero stats for empty array", () => {
    const stats = computeJobStats([]);
    expect(stats.total).toBe(0);
    expect(stats.completed).toBe(0);
    expect(stats.failed).toBe(0);
    expect(stats.avgMs).toBe(0);
    expect(stats.topModel).toBeNull();
    expect(stats.topFeature).toBeNull();
  });

  test("counts completed and failed correctly", () => {
    const jobs = [
      makeJob({ id: "1", status: "COMPLETED" }),
      makeJob({ id: "2", status: "FAILED" }),
      makeJob({ id: "3", status: "COMPLETED" }),
      makeJob({ id: "4", status: "RUNNING" }),
    ];
    const stats = computeJobStats(jobs);
    expect(stats.total).toBe(4);
    expect(stats.completed).toBe(2);
    expect(stats.failed).toBe(1);
  });

  test("computes average duration from timestamps", () => {
    const jobs = [
      makeJob({
        id: "1",
        status: "COMPLETED",
        createdAt: "2025-01-01T10:00:00.000Z",
        updatedAt: "2025-01-01T10:00:02.000Z",
      }),
      makeJob({
        id: "2",
        status: "COMPLETED",
        createdAt: "2025-01-01T10:00:00.000Z",
        updatedAt: "2025-01-01T10:00:04.000Z",
      }),
    ];
    const stats = computeJobStats(jobs);
    expect(stats.avgMs).toBe(3000);
  });

  test("identifies top model by frequency", () => {
    const jobs = [
      makeJob({ id: "1", payload: { model: "gpt-4o" } }),
      makeJob({ id: "2", payload: { model: "gpt-4o" } }),
      makeJob({ id: "3", payload: { model: "claude-3" } }),
    ];
    const stats = computeJobStats(jobs);
    expect(stats.topModel).toBe("⚡ gpt-4o");
    expect(stats.uniqueModels).toBe(2);
  });

  test("identifies top feature by frequency", () => {
    const jobs = [
      makeJob({ id: "1", featureKey: "ai.chat.stream" }),
      makeJob({ id: "2", featureKey: "ai.chat.stream" }),
      makeJob({ id: "3", featureKey: "ai.code.generate" }),
    ];
    const stats = computeJobStats(jobs);
    expect(stats.topFeature).toBe("⚡ stream");
    expect(stats.uniqueFeatures).toBe(2);
  });

  test("handles jobs without timestamps gracefully", () => {
    const jobs = [
      makeJob({
        id: "1",
        status: "COMPLETED",
        createdAt: undefined as unknown as string,
      }),
    ];
    const stats = computeJobStats(jobs);
    expect(stats.avgMs).toBe(0);
  });

  test("uses result.durationMs as fallback", () => {
    const jobs = [
      makeJob({
        id: "1",
        status: "PENDING",
        createdAt: undefined as unknown as string,
        updatedAt: undefined as unknown as string,
        result: { durationMs: 1500 } as unknown as Job["result"],
      }),
    ];
    const stats = computeJobStats(jobs);
    expect(stats.avgMs).toBe(1500);
  });
});
