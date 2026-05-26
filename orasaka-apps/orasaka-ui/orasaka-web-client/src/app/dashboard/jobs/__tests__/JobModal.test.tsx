import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { JobModal } from "@/app/dashboard/jobs/JobModal";

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      jobs: {
        jobDetails: "Job Details",
        payload: "Payload",
        result: "Result",
        error: "Error",
        close: "Close",
        noData: "No data",
        copyId: "Copy ID",
        status: "Status",
        featureKey: "Feature",
        createdAt: "Created",
        updatedAt: "Updated",
        duration: "Duration",
        progress: "Progress",
      },
    },
    locale: "en",
  }),
}));

jest.mock("@/core/constants/http.constants", () => ({
  JOB_STATUS: {
    COMPLETED: "COMPLETED",
    FAILED: "FAILED",
    PENDING: "PENDING",
    PROCESSING: "PROCESSING",
  },
}));

jest.mock("@/components/ui/Button", () => ({
  Button: ({ children, ...props }: { children: React.ReactNode } & React.ButtonHTMLAttributes<HTMLButtonElement>) => (
    <button {...props}>{children}</button>
  ),
}));

jest.mock("@/features/playground/components/ResultDisplay", () => ({
  ResultDisplay: () => <div data-testid="result-display" />,
}));

jest.mock("@/app/dashboard/jobs/JobModalParts", () => ({
  ProgressTimeline: () => <div data-testid="progress-timeline" />,
  StageLabels: () => <div data-testid="stage-labels" />,
  StatusPill: ({ status }: { status: string }) => <span>{status}</span>,
  LiveElapsed: () => <span>0s</span>,
  PillTab: ({ label, isActive, onClick }: { label: string; isActive: boolean; onClick: () => void }) => (
    <button onClick={onClick} data-active={isActive}>{label}</button>
  ),
  FileJson: () => <span>📄</span>,
  FileOutput: () => <span>📤</span>,
  AlertTriangle: () => <span>⚠️</span>,
}));

const baseJob = {
  id: "job-42",
  featureKey: "chat.text",
  status: "COMPLETED",
  createdAt: "2026-01-01T00:00:00.000Z",
  updatedAt: "2026-01-01T00:00:03.000Z",
  payload: { model: "llama3", prompt: "Hello" },
  result: { text: "Hi there", durationMs: 3000 },
  errorMessage: null,
};

describe("JobModal", () => {
  const onClose = jest.fn();
  const onCopy = jest.fn();

  afterEach(() => jest.clearAllMocks());

  it("returns null when job is null", () => {
    const { container } = render(
      <JobModal job={null} modalType="payload" onClose={onClose} copiedId={null} onCopy={onCopy} />,
    );
    expect(container.innerHTML).toBe("");
  });

  it("renders job ID", () => {
    render(
      <JobModal job={baseJob as never} modalType="payload" onClose={onClose} copiedId={null} onCopy={onCopy} />,
    );
    expect(screen.getByText(/job-42/)).toBeInTheDocument();
  });

  it("renders status pill", () => {
    render(
      <JobModal job={baseJob as never} modalType="payload" onClose={onClose} copiedId={null} onCopy={onCopy} />,
    );
    expect(screen.getByText("COMPLETED")).toBeInTheDocument();
  });

  it("closes on Escape key", () => {
    render(
      <JobModal job={baseJob as never} modalType="payload" onClose={onClose} copiedId={null} onCopy={onCopy} />,
    );
    fireEvent.keyDown(document, { key: "Escape" });
    expect(onClose).toHaveBeenCalled();
  });
});
