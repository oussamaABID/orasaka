import { getSession } from "next-auth/react";
import type { Job } from "@/features/jobs/types/jobs.types";

// ── Types ────────────────────────────────────────────────────────────────────

export interface JobsPage {
  content: Job[];
  totalPages: number;
  totalElements: number;
}

/**
 * Stateless adapter exposing job-related network operations.
 */
export const JobsApi = {
  /**
   * Fetches a paginated list of async jobs from the BFF REST proxy.
   *
   * @param page - Zero-indexed page number.
   * @param size - Number of items per page.
   * @returns The paginated job response with content and metadata.
   */
  fetchPage: async (page: number, size: number): Promise<JobsPage> => {
    const session = await getSession();
    const headers: Record<string, string> = {};
    if (session?.user?.id) {
      headers["Authorization"] = `Bearer ${session.user.id}`;
    }

    const response = await fetch(`/api/v1/jobs?page=${page}&size=${size}`, {
      headers,
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch jobs: ${response.statusText}`);
    }

    const data = await response.json();
    return {
      content: data.content ?? [],
      totalPages: data.totalPages ?? 0,
      totalElements: data.totalElements ?? 0,
    };
  },
} as const;
