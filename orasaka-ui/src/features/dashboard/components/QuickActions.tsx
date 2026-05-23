import * as React from "react";
import { MessageSquarePlus, User, Sliders, ArrowRight } from "lucide-react";

interface QuickActionsProps {
  onStartNewChat: () => void;
  onResumeProfile: () => void;
  onConfigureSettings: () => void;
  accentClasses: {
    text: string;
    bgSoft: string;
  };
  t: {
    dashboard: {
      quickActions: string;
      startNewChat: string;
      startNewChatDesc: string;
      manageProfile: string;
      manageProfileDesc: string;
      configureSettings: string;
      configureSettingsDesc: string;
    };
  };
}

/**
 * Renders the Quick Actions section of the user dashboard.
 * Provides entry points to start a new chat session, view/edit user profile, or change system settings.
 *
 * @param props - The component properties.
 * @param props.onStartNewChat - Callback triggered to initialize a new chat session.
 * @param props.onResumeProfile - Callback triggered to view or manage the user profile.
 * @param props.onConfigureSettings - Callback triggered to navigate to settings config.
 * @param props.accentClasses - Accent styling configuration mapping text and background colors.
 * @param props.t - The localized translations dictionary for dashboard interface elements.
 * @returns React element representing the quick actions panel.
 */
export function QuickActions({
  onStartNewChat,
  onResumeProfile,
  onConfigureSettings,
  accentClasses,
  t,
}: QuickActionsProps) {
  // Abstract the button style to follow the Eradicate Tailwind Class Duplication rule
  const actionButtonClass =
    "w-full text-left flex items-start p-4 rounded-2xl border border-zinc-200/80 bg-white/70 hover:bg-white dark:border-zinc-800/60 dark:bg-zinc-900/40 dark:hover:bg-zinc-900/80 transition-all duration-200 hover:translate-y-[-1px] group focus:outline-none focus:ring-2 focus:ring-zinc-400";

  return (
    <div className="md:col-span-2 space-y-4">
      <h3 className="text-lg font-bold text-zinc-800 dark:text-zinc-200">
        {t.dashboard.quickActions}
      </h3>

      <div className="space-y-3">
        {/* Action 1: New Chat */}
        <button onClick={onStartNewChat} className={actionButtonClass}>
          <div
            className={`p-2 rounded-xl ${accentClasses.bgSoft} ${accentClasses.text} mr-4`}
          >
            <MessageSquarePlus className="h-5 w-5" />
          </div>
          <div className="flex-1">
            <h4 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100 flex items-center justify-between">
              <span>{t.dashboard.startNewChat}</span>
              <ArrowRight className="h-3.5 w-3.5 opacity-0 group-hover:opacity-100 transition-opacity duration-200 text-zinc-400" />
            </h4>
            <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
              {t.dashboard.startNewChatDesc}
            </p>
          </div>
        </button>

        {/* Action 2: Manage Profile */}
        <button onClick={onResumeProfile} className={actionButtonClass}>
          <div
            className={`p-2 rounded-xl ${accentClasses.bgSoft} ${accentClasses.text} mr-4`}
          >
            <User className="h-5 w-5" />
          </div>
          <div className="flex-1">
            <h4 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100 flex items-center justify-between">
              <span>{t.dashboard.manageProfile}</span>
              <ArrowRight className="h-3.5 w-3.5 opacity-0 group-hover:opacity-100 transition-opacity duration-200 text-zinc-400" />
            </h4>
            <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
              {t.dashboard.manageProfileDesc}
            </p>
          </div>
        </button>

        {/* Action 3: Config Settings */}
        <button onClick={onConfigureSettings} className={actionButtonClass}>
          <div
            className={`p-2 rounded-xl ${accentClasses.bgSoft} ${accentClasses.text} mr-4`}
          >
            <Sliders className="h-5 w-5" />
          </div>
          <div className="flex-1">
            <h4 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100 flex items-center justify-between">
              <span>{t.dashboard.configureSettings}</span>
              <ArrowRight className="h-3.5 w-3.5 opacity-0 group-hover:opacity-100 transition-opacity duration-200 text-zinc-400" />
            </h4>
            <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
              {t.dashboard.configureSettingsDesc}
            </p>
          </div>
        </button>
      </div>
    </div>
  );
}
