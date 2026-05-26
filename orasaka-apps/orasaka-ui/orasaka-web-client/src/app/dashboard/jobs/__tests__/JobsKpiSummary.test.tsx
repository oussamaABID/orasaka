import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import { JobsKpiSummary } from "@/app/dashboard/jobs/JobsKpiSummary";

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      jobs: {
        kpiTotalJobs: "Total Jobs",
        kpiSuccessRate: "Success Rate",
        kpiModelsUsed: "Models Used",
        kpiCapabilities: "Capabilities",
        kpiTotalInference: "Total Inference",
      },
    },
    locale: "en",
  }),
}));

jest.mock("@/app/dashboard/jobs/jobStats.utils", () => ({
  computeJobStats: () => ({
    total: 42,
    completed: 30,
    failed: 5,
    pending: 4,
    processing: 3,
    successRate: 85.7,
    avgDurationMs: 2500,
    models: new Set(["llama3", "gpt4"]),
    capabilities: new Set(["chat.text", "media.image"]),
    totalInferenceMs: 120000,
  }),
}));

jest.mock("@/app/dashboard/jobs/AnimatedCounter", () => ({
  AnimatedCounter: ({ value, suffix }: { value: number; suffix?: string }) => (
    <span>{value}{suffix}</span>
  ),
}));

describe("JobsKpiSummary", () => {
  const jobs: never[] = [];

  it("renders Total Jobs label", () => {
    render(<JobsKpiSummary jobs={jobs} />);
    expect(screen.getByText("Total Jobs")).toBeInTheDocument();
  });

  it("renders Success Rate label", () => {
    render(<JobsKpiSummary jobs={jobs} />);
    expect(screen.getByText("Success Rate")).toBeInTheDocument();
  });

  it("renders Models Used label", () => {
    render(<JobsKpiSummary jobs={jobs} />);
    expect(screen.getByText("Models Used")).toBeInTheDocument();
  });

  it("renders total count from AnimatedCounter", () => {
    render(<JobsKpiSummary jobs={jobs} />);
    expect(screen.getByText("42")).toBeInTheDocument();
  });
});
