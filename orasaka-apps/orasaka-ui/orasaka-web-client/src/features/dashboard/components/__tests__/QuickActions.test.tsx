import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { QuickActions } from "@/features/dashboard/components/QuickActions";

const mockT = {
  dashboard: {
    quickActions: "Quick Actions",
    startNewChat: "Start New Chat",
    startNewChatDesc: "Begin a new AI conversation",
    manageProfile: "Manage Profile",
    manageProfileDesc: "Update your profile settings",
    configureSettings: "Configure Settings",
    configureSettingsDesc: "Adjust application preferences",
  },
} as never;

const accentClasses = { text: "text-amber-500", bgSoft: "bg-amber-500/10" };

describe("QuickActions", () => {
  const onStartNewChat = jest.fn();
  const onResumeProfile = jest.fn();
  const onConfigureSettings = jest.fn();

  afterEach(() => jest.clearAllMocks());

  it("renders the section heading", () => {
    render(
      <QuickActions
        onStartNewChat={onStartNewChat}
        onResumeProfile={onResumeProfile}
        onConfigureSettings={onConfigureSettings}
        accentClasses={accentClasses}
        t={mockT}
      />,
    );
    expect(screen.getByText("Quick Actions")).toBeInTheDocument();
  });

  it("renders all three action buttons", () => {
    render(
      <QuickActions
        onStartNewChat={onStartNewChat}
        onResumeProfile={onResumeProfile}
        onConfigureSettings={onConfigureSettings}
        accentClasses={accentClasses}
        t={mockT}
      />,
    );
    expect(screen.getByText("Start New Chat")).toBeInTheDocument();
    expect(screen.getByText("Manage Profile")).toBeInTheDocument();
    expect(screen.getByText("Configure Settings")).toBeInTheDocument();
  });

  it("renders action descriptions", () => {
    render(
      <QuickActions
        onStartNewChat={onStartNewChat}
        onResumeProfile={onResumeProfile}
        onConfigureSettings={onConfigureSettings}
        accentClasses={accentClasses}
        t={mockT}
      />,
    );
    expect(screen.getByText("Begin a new AI conversation")).toBeInTheDocument();
    expect(screen.getByText("Update your profile settings")).toBeInTheDocument();
    expect(screen.getByText("Adjust application preferences")).toBeInTheDocument();
  });

  it("calls onStartNewChat when first action clicked", () => {
    render(
      <QuickActions
        onStartNewChat={onStartNewChat}
        onResumeProfile={onResumeProfile}
        onConfigureSettings={onConfigureSettings}
        accentClasses={accentClasses}
        t={mockT}
      />,
    );
    fireEvent.click(screen.getByText("Start New Chat"));
    expect(onStartNewChat).toHaveBeenCalledTimes(1);
  });

  it("calls onResumeProfile when profile action clicked", () => {
    render(
      <QuickActions
        onStartNewChat={onStartNewChat}
        onResumeProfile={onResumeProfile}
        onConfigureSettings={onConfigureSettings}
        accentClasses={accentClasses}
        t={mockT}
      />,
    );
    fireEvent.click(screen.getByText("Manage Profile"));
    expect(onResumeProfile).toHaveBeenCalledTimes(1);
  });

  it("calls onConfigureSettings when settings action clicked", () => {
    render(
      <QuickActions
        onStartNewChat={onStartNewChat}
        onResumeProfile={onResumeProfile}
        onConfigureSettings={onConfigureSettings}
        accentClasses={accentClasses}
        t={mockT}
      />,
    );
    fireEvent.click(screen.getByText("Configure Settings"));
    expect(onConfigureSettings).toHaveBeenCalledTimes(1);
  });
});
