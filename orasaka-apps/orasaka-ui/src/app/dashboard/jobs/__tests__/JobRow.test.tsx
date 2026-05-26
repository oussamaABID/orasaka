import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { JobRow } from "@/app/dashboard/jobs/JobRow";

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      notifications: {
        videoGen: "Video Generation",
        imageGen: "Image Generation",
        speechGen: "Speech Generation",
        textGen: "Text Generation",
      },
      jobs: {
        payload: "Payload",
        result: "Result",
        error: "Error",
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

const baseJob = {
  id: "abcdef12-3456-7890-abcd-ef1234567890",
  featureKey: "chat.text",
  status: "COMPLETED" as const,
  createdAt: "2026-01-01T00:00:00.000Z",
  updatedAt: "2026-01-01T00:00:05.000Z",
  payload: { model: "llama3", prompt: "hello" },
  result: { output: "world" },
  errorMessage: null,
};

const baseProps = {
  copiedId: null as string | null,
  onCopy: jest.fn(),
  isExpandedError: false,
  onToggleExpandError: jest.fn(),
  onOpenModal: jest.fn(),
};

describe("JobRow", () => {
  afterEach(() => jest.clearAllMocks());

  it("renders truncated job ID", () => {
    render(
      <table><tbody><JobRow job={baseJob as never} {...baseProps} /></tbody></table>,
    );
    expect(screen.getByText("abcdef12...")).toBeInTheDocument();
  });

  it("renders feature display name for text", () => {
    render(
      <table><tbody><JobRow job={baseJob as never} {...baseProps} /></tbody></table>,
    );
    expect(screen.getByText("Text Generation")).toBeInTheDocument();
  });

  it("renders video feature name", () => {
    render(
      <table><tbody><JobRow job={{ ...baseJob, featureKey: "media.video" } as never} {...baseProps} /></tbody></table>,
    );
    expect(screen.getByText("Video Generation")).toBeInTheDocument();
  });

  it("renders COMPLETED status badge", () => {
    render(
      <table><tbody><JobRow job={baseJob as never} {...baseProps} /></tbody></table>,
    );
    expect(screen.getByText("COMPLETED")).toBeInTheDocument();
  });

  it("renders FAILED status badge", () => {
    render(
      <table><tbody><JobRow job={{ ...baseJob, status: "FAILED" } as never} {...baseProps} /></tbody></table>,
    );
    expect(screen.getByText("FAILED")).toBeInTheDocument();
  });

  it("renders PENDING status badge", () => {
    render(
      <table><tbody><JobRow job={{ ...baseJob, status: "PENDING" } as never} {...baseProps} /></tbody></table>,
    );
    expect(screen.getByText("PENDING")).toBeInTheDocument();
  });

  it("renders PROCESSING status badge", () => {
    render(
      <table><tbody><JobRow job={{ ...baseJob, status: "PROCESSING" } as never} {...baseProps} /></tbody></table>,
    );
    expect(screen.getByText("PROCESSING")).toBeInTheDocument();
  });

  it("renders model name", () => {
    render(
      <table><tbody><JobRow job={baseJob as never} {...baseProps} /></tbody></table>,
    );
    expect(screen.getByText("llama3")).toBeInTheDocument();
  });

  it("renders serialized JSON if model is an object", () => {
    const jobWithObjectModel = {
      ...baseJob,
      payload: { model: { name: "custom-model", version: 2 } },
    };
    render(
      <table><tbody><JobRow job={jobWithObjectModel as never} {...baseProps} /></tbody></table>,
    );
    expect(screen.getByText('{"name":"custom-model","version":2}')).toBeInTheDocument();
  });

  it("renders dash for no model", () => {
    render(
      <table><tbody><JobRow job={{ ...baseJob, payload: {} } as never} {...baseProps} /></tbody></table>,
    );
    expect(screen.getByText("—")).toBeInTheDocument();
  });

  it("calls onCopy when copy button clicked", () => {
    render(
      <table><tbody><JobRow job={baseJob as never} {...baseProps} /></tbody></table>,
    );
    fireEvent.click(screen.getByLabelText("Copy full job ID"));
    expect(baseProps.onCopy).toHaveBeenCalledWith(baseJob.id, baseJob.id);
  });

  it("renders duration", () => {
    render(
      <table><tbody><JobRow job={baseJob as never} {...baseProps} /></tbody></table>,
    );
    expect(screen.getByText("5s")).toBeInTheDocument();
  });

  it("renders image feature name", () => {
    render(
      <table><tbody><JobRow job={{ ...baseJob, featureKey: "media.image" } as never} {...baseProps} /></tbody></table>,
    );
    expect(screen.getByText("Image Generation")).toBeInTheDocument();
  });

  it("renders speech feature name", () => {
    render(
      <table><tbody><JobRow job={{ ...baseJob, featureKey: "chat.speech" } as never} {...baseProps} /></tbody></table>,
    );
    expect(screen.getByText("Speech Generation")).toBeInTheDocument();
  });
});
