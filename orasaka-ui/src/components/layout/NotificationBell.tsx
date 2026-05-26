"use client";

import * as React from "react";
import Link from "next/link";
import { useTenant } from "@/features/tenant/context/TenantContext";
import { useJobStream } from "@/features/jobs/context/JobStreamContext";
import { useTranslation } from "@/core/context/LocaleContext";
import { MODEL_CATEGORY } from "@/core/constants/capability.constants";
import { Bell, CheckCircle2, AlertTriangle } from "lucide-react";
import { JOB_STATUS } from "@/core/constants/http.constants";

export interface NotificationBellProps {
  bellOpen: boolean;
  onToggle: (open: boolean) => void;
}

export const NotificationBell: React.FC<NotificationBellProps> = ({
  bellOpen,
  onToggle,
}) => {
  const { accentClasses } = useTenant();
  const { activeJobsCount, lastJobs } = useJobStream();
  const { t } = useTranslation();

  return (
    <div className="relative">
      <button
        onClick={() => onToggle(!bellOpen)}
        className="relative p-2 text-zinc-500 hover:text-zinc-700 dark:text-zinc-400 dark:hover:text-zinc-200 hover:bg-zinc-100/80 dark:hover:bg-zinc-900/50 rounded-xl transition-all duration-200 focus:outline-none"
        aria-label="Notifications"
      >
        <Bell className="h-5 w-5" />
        {activeJobsCount > 0 && (
          <span className="absolute top-1 right-1 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] font-bold text-white animate-pulse">
            {activeJobsCount}
          </span>
        )}
      </button>

      {bellOpen && (
        <>
          <button
            type="button"
            className="fixed inset-0 z-10 bg-transparent border-none cursor-default"
            aria-label="Close notifications"
            onClick={() => onToggle(false)}
          />
          <div className="absolute right-0 mt-2 w-80 rounded-xl border border-zinc-200/80 bg-white/90 p-1.5 shadow-xl dark:border-zinc-800/60 dark:bg-zinc-900/90 backdrop-blur-md z-20 animate-in fade-in slide-in-from-top-2 duration-200 text-zinc-800 dark:text-zinc-200">
            <div className="px-3 py-2 border-b border-zinc-200 dark:border-zinc-800/80 flex items-center justify-between">
              <span className="font-semibold text-sm">
                {t.notifications.title}
              </span>
              {activeJobsCount > 0 && (
                <span
                  className={`text-[10px] px-2 py-0.5 rounded-full font-medium ${accentClasses.bgSoft} ${accentClasses.text}`}
                >
                  {activeJobsCount} {t.notifications.active}
                </span>
              )}
            </div>
            <div className="max-h-64 overflow-y-auto py-1 scrollbar-thin">
              {lastJobs.length === 0 ? (
                <div className="text-center py-6 text-xs text-zinc-400 dark:text-zinc-500">
                  {t.notifications.noTasks}
                </div>
              ) : (
                <ul className="divide-y divide-zinc-100 dark:divide-zinc-800/40">
                  {lastJobs.map((job) => {
                    const resolveFeatureName = () => {
                      if (job.featureKey.includes(MODEL_CATEGORY.VIDEO)) return t.notifications.videoGen;
                      if (job.featureKey.includes(MODEL_CATEGORY.IMAGE)) return t.notifications.imageGen;
                      if (job.featureKey.includes(MODEL_CATEGORY.SPEECH)) return t.notifications.speechGen;
                      return t.notifications.textGen;
                    };
                    const featureName = resolveFeatureName();
                    return (
                      <li
                        key={job.id}
                        className="hover:bg-zinc-50/50 dark:hover:bg-zinc-900/20 rounded-lg transition-colors"
                      >
                        <Link
                          href={`/dashboard/jobs?jobId=${job.id}`}
                          onClick={() => onToggle(false)}
                          className="p-3 text-xs flex items-start space-x-2.5 w-full text-left"
                        >
                          {(() => {
                            if (job.status === JOB_STATUS.PROCESSING ||
                              job.status === JOB_STATUS.PENDING) {
                              return (
                                <div className="h-4 w-4 mt-0.5 rounded-full border-2 border-zinc-300 dark:border-zinc-700 border-t-zinc-600 dark:border-t-zinc-300 animate-spin flex-shrink-0" />
                              );
                            }
                            if (job.status === JOB_STATUS.COMPLETED) {
                              return (
                                <CheckCircle2 className="h-4 w-4 mt-0.5 text-emerald-500 flex-shrink-0" />
                              );
                            }
                            return (
                              <AlertTriangle className="h-4 w-4 mt-0.5 text-red-500 flex-shrink-0" />
                            );
                          })()}
                          <div className="min-w-0 flex-1">
                            <p className="font-semibold truncate text-zinc-700 dark:text-zinc-300">
                              {featureName}
                            </p>
                            <p className="text-[10px] text-zinc-400 dark:text-zinc-500 mt-0.5 truncate font-mono">
                              {job.id.substring(0, 8)}...
                            </p>
                          </div>
                        </Link>
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>
            <div className="p-1.5 border-t border-zinc-100 dark:border-zinc-800/80">
              <Link
                href="/dashboard/jobs"
                onClick={() => onToggle(false)}
                className="block text-center rounded-lg py-1.5 text-xs font-semibold hover:bg-zinc-100 dark:hover:bg-zinc-800/80 transition-colors"
              >
                {t.notifications.viewAll}
              </Link>
            </div>
          </div>
        </>
      )}
    </div>
  );
};
