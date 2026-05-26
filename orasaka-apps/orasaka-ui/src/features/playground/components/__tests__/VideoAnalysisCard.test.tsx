/* eslint-disable @typescript-eslint/no-explicit-any */
import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { VideoAnalysisCard } from "@/features/playground/components/VideoAnalysisCard";

// Mock translation and locale context
const mockT = {
  playground: {
    videoAnalysis: "Video Analysis",
    mediaPayload: "Media Payload",
    dragAndDropVideoOr: "Drag and drop or ",
    browse: "Browse",
    removeVideo: "Remove",
    supportedFormatsVideo: "MP4, MOV",
    executeIngestion: "Execute Ingestion",
    uploadingVideo: "Uploading video...",
    processingVideo: "Processing video...",
    runningIngestion: "Running Ingestion",
    analyzeAnother: "Analyze Another",
    outputResult: "Output Result",
    videoAnalysisDesc: "Description text",
    gatewayRestriction: "Gateway restriction",
    onlyMp4MovSupported: "Only MP4/MOV supported",
    fileLimitExceeded: "File limit exceeded",
    processingProgress: "Processing: {percent}%",
  },
};

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({ t: mockT, locale: "en" }),
}));

// Mock useVideoAnalysis hook
const mockAnalyze = jest.fn();
const mockReset = jest.fn();
let mockState = "idle";
let mockResult: any = null;
let mockError: any = null;

jest.mock("@/features/playground/hooks/useVideoAnalysis", () => ({
  useVideoAnalysis: () => ({
    analyze: mockAnalyze,
    state: mockState,
    result: mockResult,
    error: mockError,
    reset: mockReset,
  }),
}));

// Mock useJobStream context
let mockJobProgress: Record<string, number> = {};
let mockVideoAnalysisJobId: string | null = null;

jest.mock("@/core/context/JobStreamContext", () => ({
  useJobStream: () => ({
    jobProgress: mockJobProgress,
    videoAnalysisJobId: mockVideoAnalysisJobId,
    jobs: [],
  }),
}));

// Mock UI components to prevent styling issues
jest.mock("@/components/ui/Card", () => ({
  Card: ({ children, className }: any) => <div className={className}>{children}</div>,
}));

jest.mock("@/components/ui/Button", () => ({
  Button: ({ children, onClick, disabled, className }: any) => (
    <button onClick={onClick} disabled={disabled} className={className}>
      {children}
    </button>
  ),
}));

// Mock sub-components
jest.mock("../ResultDisplay", () => ({
  ResultDisplay: ({ payload }: any) => <div data-testid="mock-result-display">{JSON.stringify(payload)}</div>,
}));

jest.mock("../ExecutionTimeline", () => ({
  ExecutionTimeline: ({ progress, modelName, isPending }: any) => (
    <div data-testid="mock-execution-timeline">
      {progress}-{modelName}-{isPending ? "pending" : "not-pending"}
    </div>
  ),
}));

// Mock native APIs
window.alert = jest.fn();

describe("VideoAnalysisCard", () => {
  beforeEach(() => {
    mockState = "idle";
    mockResult = null;
    mockError = null;
    mockJobProgress = {};
    mockVideoAnalysisJobId = null;
    jest.clearAllMocks();
  });

  it("renders idle state with title and dropzone", () => {
    render(<VideoAnalysisCard />);
    expect(screen.getByText("Video Analysis")).toBeInTheDocument();
    expect(screen.getByText(/Drag and drop/)).toBeInTheDocument();
    expect(screen.getByText("Browse")).toBeInTheDocument();
    expect(screen.getByText("Execute Ingestion")).toBeDisabled();
  });

  it("handles model selection change", () => {
    render(<VideoAnalysisCard />);
    const select = document.getElementById("video-model-select") as HTMLSelectElement;
    fireEvent.change(select, { target: { value: "whisper-tiny-en" } });
    expect(select.value).toBe("whisper-tiny-en");
  });

  it("handles file selection via input and file removal", () => {
    render(<VideoAnalysisCard />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(["data"], "sample.mp4", { type: "video/mp4" });

    fireEvent.change(input, { target: { files: [file] } });

    expect(screen.getByText(/sample.mp4/)).toBeInTheDocument();
    expect(screen.getByText("Execute Ingestion")).toBeEnabled();

    // Click remove
    fireEvent.click(screen.getByText("Remove"));
    expect(screen.queryByText(/sample.mp4/)).not.toBeInTheDocument();
    expect(screen.getByText("Execute Ingestion")).toBeDisabled();
  });

  it("rejects unsupported file types", () => {
    render(<VideoAnalysisCard />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(["data"], "sample.jpg", { type: "image/jpeg" });

    fireEvent.change(input, { target: { files: [file] } });

    expect(window.alert).toHaveBeenCalledWith(mockT.playground.onlyMp4MovSupported);
    expect(screen.queryByText(/sample.jpg/)).not.toBeInTheDocument();
  });

  it("rejects oversized files", () => {
    render(<VideoAnalysisCard />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(["data"], "sample.mp4", { type: "video/mp4" });
    Object.defineProperty(file, "size", { value: 101 * 1024 * 1024 }); // 101MB

    fireEvent.change(input, { target: { files: [file] } });

    expect(window.alert).toHaveBeenCalledWith(mockT.playground.fileLimitExceeded);
    expect(screen.queryByText(/sample.mp4/)).not.toBeInTheDocument();
  });

  it("calls analyze on execution", () => {
    render(<VideoAnalysisCard />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(["data"], "sample.mp4", { type: "video/mp4" });

    fireEvent.change(input, { target: { files: [file] } });
    fireEvent.click(screen.getByText("Execute Ingestion"));

    expect(mockAnalyze).toHaveBeenCalledWith(file, "whisper-base");
  });

  it("renders uploading pending state", () => {
    mockState = "uploading";
    render(<VideoAnalysisCard />);
    expect(screen.getAllByText("Uploading video...")[0]).toBeInTheDocument();
  });

  it("renders processing pending state with progress bar and percent", () => {
    mockState = "processing";
    mockVideoAnalysisJobId = "job-123";
    mockJobProgress = { "job-123": 45 };

    render(<VideoAnalysisCard />);
    expect(screen.getByText("Processing video...")).toBeInTheDocument();
    expect(screen.getByText("Processing: 45%")).toBeInTheDocument();
  });

  it("renders success state with result display and resets", () => {
    mockState = "success";
    mockResult = { keyframeCount: 5, transcript: "hello" };
    render(<VideoAnalysisCard />);

    expect(screen.getByTestId("mock-result-display")).toHaveTextContent("hello");
    expect(screen.getByText("Analyze Another")).toBeInTheDocument();

    fireEvent.click(screen.getByText("Analyze Another"));
    expect(mockReset).toHaveBeenCalled();
  });

  it("renders error state", () => {
    mockState = "error";
    mockError = "Ingestion failed";
    render(<VideoAnalysisCard />);

    expect(screen.getByText("Gateway restriction")).toBeInTheDocument();
    expect(screen.getAllByText("Ingestion failed")[0]).toBeInTheDocument();
  });

  it("handles drag and drop events", () => {
    render(<VideoAnalysisCard />);
    const dropzone = screen.getByRole("region", { name: "Media Payload" });

    fireEvent.dragEnter(dropzone);
    fireEvent.dragOver(dropzone);
    fireEvent.dragLeave(dropzone);

    // Mock drop event
    const file = new File(["data"], "drop.mp4", { type: "video/mp4" });
    fireEvent.drop(dropzone, {
      dataTransfer: {
        files: [file],
      },
    });

    expect(screen.getByText(/drop.mp4/)).toBeInTheDocument();
  });
});
