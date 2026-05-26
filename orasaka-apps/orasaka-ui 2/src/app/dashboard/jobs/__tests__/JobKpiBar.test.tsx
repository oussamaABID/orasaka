import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import { JobKpiBar } from "@/app/dashboard/jobs/JobKpiBar";

jest.mock("@/core/constants/http.constants", () => ({
  JOB_STATUS: {
    COMPLETED: "COMPLETED",
    FAILED: "FAILED",
    PENDING: "PENDING",
    PROCESSING: "PROCESSING",
  },
}));

jest.mock("@/core/constants/capability.constants", () => ({
  resolveProviderFromFeature: (key: string) => {
    if (key.includes("video")) return "Stable Diffusion";
    if (key.includes("chat")) return "Ollama";
    return null;
  },
}));

const baseJob = {
  id: "job-1",
  featureKey: "chat.text",
  status: "COMPLETED" as const,
  createdAt: "2026-01-01T00:00:00.000Z",
  updatedAt: "2026-01-01T00:00:03.000Z",
  payload: { model: "llama3", provider: "ollama" },
  result: { durationMs: 2500, format: "text/plain" },
  errorMessage: null,
};

describe("JobKpiBar", () => {
  it("renders model chip", () => {
    render(
      <table><tbody><JobKpiBar job={baseJob as never} /></tbody></table>,
    );
    expect(screen.getByText("llama3")).toBeInTheDocument();
    expect(screen.getByText("MODEL")).toBeInTheDocument();
  });

  it("renders provider chip", () => {
    render(
      <table><tbody><JobKpiBar job={baseJob as never} /></tbody></table>,
    );
    expect(screen.getByText("ollama")).toBeInTheDocument();
    expect(screen.getByText("PROVIDER")).toBeInTheDocument();
  });

  it("renders inference duration", () => {
    render(
      <table><tbody><JobKpiBar job={baseJob as never} /></tbody></table>,
    );
    expect(screen.getByText("2.5s")).toBeInTheDocument();
    expect(screen.getByText("INFERENCE")).toBeInTheDocument();
  });

  it("renders output format chip", () => {
    render(
      <table><tbody><JobKpiBar job={baseJob as never} /></tbody></table>,
    );
    expect(screen.getByText("TEXT/PLAIN")).toBeInTheDocument();
    expect(screen.getByText("OUTPUT")).toBeInTheDocument();
  });

  it("renders voice chip when present", () => {
    const jobWithVoice = {
      ...baseJob,
      payload: { ...baseJob.payload, voice: "alloy" },
    };
    render(
      <table><tbody><JobKpiBar job={jobWithVoice as never} /></tbody></table>,
    );
    expect(screen.getByText("alloy")).toBeInTheDocument();
    expect(screen.getByText("VOICE")).toBeInTheDocument();
  });

  it("renders userId when showUser is true", () => {
    const jobWithUser = {
      ...baseJob,
      userId: "user-42",
    };
    render(
      <table><tbody><JobKpiBar job={jobWithUser as never} showUser /></tbody></table>,
    );
    expect(screen.getByText("user-42")).toBeInTheDocument();
    expect(screen.getByText("USER")).toBeInTheDocument();
  });

  it("resolves provider from feature when not in payload", () => {
    const job = {
      ...baseJob,
      featureKey: "media.video.generate",
      payload: { model: "svd-xt" },
    };
    render(
      <table><tbody><JobKpiBar job={job as never} /></tbody></table>,
    );
    expect(screen.getByText("Stable Diffusion")).toBeInTheDocument();
  });

  it("renders extra metrics", () => {
    const jobWithMetrics = {
      ...baseJob,
      result: {
        ...baseJob.result,
        metrics: { tokenCount: 150 },
        keyframeCount: 24,
      },
    };
    render(
      <table><tbody><JobKpiBar job={jobWithMetrics as never} /></tbody></table>,
    );
    expect(screen.getByText("150")).toBeInTheDocument();
    expect(screen.getByText("24")).toBeInTheDocument();
  });

  it("renders extra metrics with serialized objects if metric value is an object", () => {
    const jobWithNestedMetrics = {
      ...baseJob,
      result: {
        ...baseJob.result,
        metrics: { tokenDetails: { prompt: 100, completion: 50 }, details: [1, 2] },
      },
    };
    render(
      <table><tbody><JobKpiBar job={jobWithNestedMetrics as never} /></tbody></table>,
    );
    expect(screen.getByText('{"prompt":100,"completion":50}')).toBeInTheDocument();
    expect(screen.getByText('[1,2]')).toBeInTheDocument();
  });

  it("computes duration from timestamps when durationMs absent", () => {
    const job = {
      ...baseJob,
      result: { format: "text/plain" },
    };
    render(
      <table><tbody><JobKpiBar job={job as never} /></tbody></table>,
    );
    expect(screen.getByText("3.0s")).toBeInTheDocument();
  });

  it("renders no chips for empty job", () => {
    const emptyJob = {
      id: "j2",
      featureKey: "unknown",
      status: "PENDING" as const,
      createdAt: "2026-01-01T00:00:00.000Z",
      updatedAt: "2026-01-01T00:00:00.000Z",
      payload: {},
      result: null,
      errorMessage: null,
    };
    const { container } = render(
      <table><tbody><JobKpiBar job={emptyJob as never} /></tbody></table>,
    );
    expect(container.querySelectorAll("[title]")).toHaveLength(0);
  });
});
