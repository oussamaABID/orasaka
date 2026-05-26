"use client";

import * as React from "react";
import { Job } from "@/features/jobs/types/jobs.types";
import { useTranslation } from "@/core/context/LocaleContext";
import { JobRow } from "./JobRow";
import { JobKpiBar } from "./JobKpiBar";
import { Loader2, ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/Button";

export interface JobsTableProps {
  jobs: Job[];
  loading: boolean;
  totalPages: number;
  totalElements: number;
  currentPage: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (size: number) => void;
  onOpenModal: (job: Job, type: "payload" | "result") => void;
  copiedId: string | null;
  onCopy: (text: string, id: string) => void;
}

export const JobsTable: React.FC<JobsTableProps> = ({
  jobs,
  loading,
  totalPages,
  totalElements,
  currentPage,
  pageSize,
  onPageChange,
  onPageSizeChange,
  onOpenModal,
  copiedId,
  onCopy,
}) => {
  const { t } = useTranslation();
  const [expandedErrorJobId, setExpandedErrorJobId] = React.useState<
    string | null
  >(null);

  const toggleExpandError = (jobId: string) => {
    setExpandedErrorJobId((prev) => (prev === jobId ? null : jobId));
  };

  return (
    <article className="glass-card rounded-2xl shadow-md overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-zinc-200/85 dark:border-zinc-800/60 bg-zinc-50/50 dark:bg-zinc-900/40 text-xs font-bold text-zinc-500 dark:text-zinc-400">
              <th className="p-4">{t.jobs?.colId || "Job ID"}</th>
              <th className="p-4">{t.jobs?.colType || "Task Type"}</th>
              <th className="p-4">{t.playground?.model || "Model"}</th>
              <th className="p-4">{t.jobs?.colStatus || "Status"}</th>
              <th className="p-4">{t.jobs?.colCreated || "Created At"}</th>
              <th className="p-4">{t.jobs?.colDuration || "Duration"}</th>
              <th className="p-4 text-right">
                {t.jobs?.colActions || "Actions"}
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-200/80 dark:divide-zinc-800/40 text-sm">
            {(() => {
              if (loading && jobs.length === 0) return (
              <tr>
                <td
                  colSpan={7}
                  className="text-center py-24 text-zinc-400 dark:text-zinc-500"
                >
                  <Loader2 className="w-6 h-6 animate-spin mx-auto mb-2 text-zinc-400" />
                  <span>{t.jobs.loading}</span>
                </td>
              </tr>
              );
              if (jobs.length === 0) return (
              <tr>
                <td
                  colSpan={7}
                  className="text-center py-20 text-zinc-400 dark:text-zinc-500"
                >
                  {t.jobs?.noJobs || "No background tasks found in history."}
                </td>
              </tr>
              );
              return jobs.map((job) => (
                <React.Fragment key={job.id}>
                  <JobRow
                    job={job}
                    copiedId={copiedId}
                    onCopy={onCopy}
                    isExpandedError={expandedErrorJobId === job.id}
                    onToggleExpandError={() => toggleExpandError(job.id)}
                    onOpenModal={onOpenModal}
                  />
                  <JobKpiBar job={job} />
                </React.Fragment>
              ));
            })()}
          </tbody>
        </table>
      </div>

      {/* Pagination controls */}
      {totalPages > 1 && (
        <footer className="flex items-center justify-between p-4 border-t border-zinc-200/80 dark:border-zinc-800/60 bg-zinc-50/50 dark:bg-zinc-900/40 text-xs font-semibold text-zinc-500 dark:text-zinc-400">
          <div className="flex items-center space-x-4">
            <span>
              {t.jobs.paginationShowing} {currentPage * pageSize + 1} -{" "}
              {Math.min((currentPage + 1) * pageSize, totalElements)}{" "}
              {t.jobs.paginationOf} {totalElements} {t.jobs.paginationTasks}
            </span>
            <select
              value={pageSize}
              onChange={(e) => {
                onPageSizeChange(Number(e.target.value));
              }}
              className="border border-zinc-200 dark:border-zinc-800 bg-transparent rounded-lg p-1 text-xs focus:outline-none"
            >
              <option value={10}>10 {t.jobs.perPage}</option>
              <option value={20}>20 {t.jobs.perPage}</option>
              <option value={50}>50 {t.jobs.perPage}</option>
            </select>
          </div>
          <div className="flex items-center space-x-2">
            <Button
              variant="ghost"
              disabled={currentPage === 0 || loading}
              onClick={() => onPageChange(Math.max(0, currentPage - 1))}
              className="h-8 px-2.5 rounded-lg border border-zinc-200 dark:border-zinc-800 hover:bg-zinc-100 dark:hover:bg-zinc-800 disabled:opacity-40"
            >
              <ChevronLeft className="w-4 h-4 mr-1" />
              <span>{t.jobs.previous}</span>
            </Button>
            <span className="px-2.5 py-1 rounded-lg bg-white dark:bg-zinc-800 border border-zinc-200/70 dark:border-zinc-700/60 text-zinc-700 dark:text-zinc-300 font-bold">
              {currentPage + 1} / {totalPages}
            </span>
            <Button
              variant="ghost"
              disabled={currentPage === totalPages - 1 || loading}
              onClick={() =>
                onPageChange(Math.min(totalPages - 1, currentPage + 1))
              }
              className="h-8 px-2.5 rounded-lg border border-zinc-200 dark:border-zinc-800 hover:bg-zinc-100 dark:hover:bg-zinc-800 disabled:opacity-40"
            >
              <span>{t.jobs.next}</span>
              <ChevronRight className="w-4 h-4 ml-1" />
            </Button>
          </div>
        </footer>
      )}
    </article>
  );
};
