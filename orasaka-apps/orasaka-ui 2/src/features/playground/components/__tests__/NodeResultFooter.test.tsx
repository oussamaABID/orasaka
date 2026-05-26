import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import { NodeResultFooter } from "@/features/playground/components/NodeResultFooter";

jest.mock("@/core/context/JobStreamContext", () => ({
  useJobStream: () => ({ jobs: [] }),
}));

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      playground: {
        outputResult: "Output Result",
        gatewayRestriction: "Gateway Restriction",
        noAssetOutput: "No output",
        taskQueued: "Queued",
        taskActive: "Active",
        taskFailed: "Failed",
        unknownError: "Unknown",
        taskCompletedNoOutput: "Done",
        status: "Status",
        jobId: "Job:",
        keyframesExtracted: "Keyframes:",
        transcript: "Transcript",
        noDialogueDetected: "No dialogue",
        localCPlusPlusInference: "C++ Inference",
        localAudioGeneration: "Audio",
        localImageGeneration: "Image",
      },
    },
    locale: "en",
  }),
}));

jest.mock("@/core/constants/http.constants", () => ({
  JOB_STATUS: {
    PENDING: "PENDING",
    PROCESSING: "PROCESSING",
    COMPLETED: "COMPLETED",
    FAILED: "FAILED",
  },
}));

const mockTranslations = {
  playground: {
    outputResult: "Output Result",
    gatewayRestriction: "Gateway Restriction",
  },
};

describe("NodeResultFooter", () => {
  it("renders success state with output result label", () => {
    render(
      <NodeResultFooter
        result={{ success: true, data: "https://example.com/image.png" }}
        t={mockTranslations as never}
      />,
    );
    expect(screen.getByText("Output Result")).toBeInTheDocument();
  });

  it("renders error state with gateway restriction label", () => {
    render(
      <NodeResultFooter
        result={{ success: false, data: "Error: timeout", error: "timeout" }}
        t={mockTranslations as never}
      />,
    );
    expect(screen.getByText("Gateway Restriction")).toBeInTheDocument();
    expect(screen.getByText("Error: timeout")).toBeInTheDocument();
  });

  it("applies success styling when result is successful", () => {
    const { container } = render(
      <NodeResultFooter
        result={{ success: true, data: "ok" }}
        t={mockTranslations as never}
      />,
    );
    const wrapper = container.firstChild as HTMLElement;
    expect(wrapper.className).toContain("bg-[var(--surface-1)]");
  });

  it("applies error styling when result is not successful", () => {
    const { container } = render(
      <NodeResultFooter
        result={{ success: false, data: "fail" }}
        t={mockTranslations as never}
      />,
    );
    const wrapper = container.firstChild as HTMLElement;
    expect(wrapper.className).toContain("bg-red-50");
  });
});
