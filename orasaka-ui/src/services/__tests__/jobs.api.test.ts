/**
 * @file jobs.api.test.ts
 * @description Tests for the jobs API adapter.
 */

import { JobsApi } from "@/services/jobs.api";
import { getSession } from "next-auth/react";

jest.mock("next-auth/react", () => ({
  getSession: jest.fn(),
}));

const mockFetch = jest.fn();
global.fetch = mockFetch;
const mockedGetSession = getSession as jest.MockedFunction<typeof getSession>;

describe("JobsApi", () => {
  beforeEach(() => {
    mockFetch.mockClear();
    mockedGetSession.mockClear();
  });

  describe("fetchPage", () => {
    it("fetches jobs with Authorization header when session exists", async () => {
      mockedGetSession.mockResolvedValueOnce({
        user: { id: "user-123" },
        expires: "2099-01-01",
      } as never);
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          content: [{ id: "j1", status: "COMPLETED" }],
          totalPages: 1,
          totalElements: 1,
        }),
      });

      const result = await JobsApi.fetchPage(0, 20);

      expect(mockFetch).toHaveBeenCalledWith(
        "/api/v1/jobs?page=0&size=20",
        expect.objectContaining({
          headers: { Authorization: "Bearer user-123" },
        }),
      );
      expect(result.content).toHaveLength(1);
      expect(result.totalPages).toBe(1);
      expect(result.totalElements).toBe(1);
    });

    it("fetches jobs without Authorization header when no session", async () => {
      mockedGetSession.mockResolvedValueOnce(null);
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          content: [],
          totalPages: 0,
          totalElements: 0,
        }),
      });

      const result = await JobsApi.fetchPage(0, 10);

      expect(mockFetch).toHaveBeenCalledWith(
        "/api/v1/jobs?page=0&size=10",
        expect.objectContaining({ headers: {} }),
      );
      expect(result.content).toEqual([]);
    });

    it("throws on HTTP error", async () => {
      mockedGetSession.mockResolvedValueOnce(null);
      mockFetch.mockResolvedValueOnce({
        ok: false,
        statusText: "Internal Server Error",
      });

      await expect(JobsApi.fetchPage(0, 10)).rejects.toThrow(
        "Failed to fetch jobs: Internal Server Error",
      );
    });

    it("defaults to empty content when data fields are missing", async () => {
      mockedGetSession.mockResolvedValueOnce(null);
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({}),
      });

      const result = await JobsApi.fetchPage(0, 5);

      expect(result.content).toEqual([]);
      expect(result.totalPages).toBe(0);
      expect(result.totalElements).toBe(0);
    });
  });
});
