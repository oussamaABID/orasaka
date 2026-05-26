/**
 * @file jobs.types.ts
 * @description TypeScript interface matching the backend JobDto record structure.
 */

export type JobStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";

export interface Job {
  id: string;
  userId: string;
  featureKey: string;
  status: JobStatus;
  payload: Record<string, unknown>;
  result: Record<string, unknown>;
  errorMessage?: string;
  progress?: number;
  createdAt: string; // ISO-8601 string
  updatedAt: string; // ISO-8601 string
}
