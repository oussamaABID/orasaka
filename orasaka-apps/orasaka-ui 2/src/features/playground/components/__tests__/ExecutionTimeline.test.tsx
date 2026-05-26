import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import { ExecutionTimeline } from "@/features/playground/components/ExecutionTimeline";

const mockT = {
  executionTimeline: {
    title: "Pipeline Progress",
    step1Title: "Prepare",
    step1Desc: "Context loaded",
    step2Title: "Inference",
    step2Desc: "Running {modelName}",
    step3Title: "Post-process",
    step3Desc: "Formatting output",
    step4Title: "Done",
    step4Desc: "Result ready",
  },
};

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({ t: mockT, locale: "en" }),
}));

describe("ExecutionTimeline", () => {
  it("returns null when not pending and progress is 0", () => {
    const { container } = render(
      <ExecutionTimeline progress={0} modelName="test-model" isPending={false} />,
    );
    expect(container.firstChild).toBeNull();
  });

  it("renders timeline when isPending is true", () => {
    render(
      <ExecutionTimeline progress={0} modelName="test-model" isPending={true} />,
    );
    expect(screen.getByText("Pipeline Progress")).toBeInTheDocument();
    expect(screen.getByText("Prepare")).toBeInTheDocument();
    expect(screen.getByText("Inference")).toBeInTheDocument();
  });

  it("renders timeline when progress > 0", () => {
    render(
      <ExecutionTimeline progress={50} modelName="gpt-4" isPending={false} />,
    );
    expect(screen.getByText("Pipeline Progress")).toBeInTheDocument();
  });

  it("replaces modelName placeholder in step 2 description", () => {
    render(
      <ExecutionTimeline progress={25} modelName="whisper-base" isPending={true} />,
    );
    expect(screen.getByText("Running whisper-base")).toBeInTheDocument();
  });

  it("uses default when modelName is empty", () => {
    render(
      <ExecutionTimeline progress={25} modelName="" isPending={true} />,
    );
    expect(screen.getByText("Running default")).toBeInTheDocument();
  });

  it("shows all 4 steps", () => {
    render(
      <ExecutionTimeline progress={50} modelName="test" isPending={true} />,
    );
    expect(screen.getByText("Prepare")).toBeInTheDocument();
    expect(screen.getByText("Inference")).toBeInTheDocument();
    expect(screen.getByText("Post-process")).toBeInTheDocument();
    expect(screen.getByText("Done")).toBeInTheDocument();
  });

  it("renders with undefined progress (default to 0)", () => {
    render(
      <ExecutionTimeline progress={undefined} modelName="m" isPending={true} />,
    );
    expect(screen.getByText("Pipeline Progress")).toBeInTheDocument();
  });

  it("renders completed steps when progress is 100", () => {
    render(
      <ExecutionTimeline progress={100} modelName="m" isPending={false} />,
    );
    expect(screen.getByText("Prepare")).toBeInTheDocument();
    expect(screen.getByText("Done")).toBeInTheDocument();
  });
});
