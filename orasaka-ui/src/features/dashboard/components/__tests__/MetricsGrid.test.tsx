import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import { MetricsGrid } from "@/features/dashboard/components/MetricsGrid";

const mockT = {
  dashboard: {
    activeSessions: "Active Sessions",
    runningParallel: "Running in parallel",
    tokensUsed: "Tokens Used",
    estimatedCumulative: "Estimated cumulative",
    memoryNodes: "Memory Nodes",
    contextSaved: "Context entries saved",
  },
} as never;

const accent = { text: "text-amber-500" };

describe("MetricsGrid", () => {
  it("renders three metric cards", () => {
    render(
      <MetricsGrid
        metrics={{ activeSessions: 3, tokensUsed: 500, memoryNodes: 42 }}
        accentClasses={accent}
        t={mockT}
      />,
    );
    expect(screen.getByText("Active Sessions")).toBeInTheDocument();
    expect(screen.getByText("Tokens Used")).toBeInTheDocument();
    expect(screen.getByText("Memory Nodes")).toBeInTheDocument();
  });

  it("renders active sessions count", () => {
    render(
      <MetricsGrid
        metrics={{ activeSessions: 5, tokensUsed: 0, memoryNodes: 0 }}
        accentClasses={accent}
        t={mockT}
      />,
    );
    expect(screen.getByText("5")).toBeInTheDocument();
  });

  it("renders tokens below 1000 as plain number", () => {
    render(
      <MetricsGrid
        metrics={{ activeSessions: 0, tokensUsed: 999, memoryNodes: 0 }}
        accentClasses={accent}
        t={mockT}
      />,
    );
    expect(screen.getByText("999")).toBeInTheDocument();
  });

  it("renders tokens above 1000 with k suffix", () => {
    render(
      <MetricsGrid
        metrics={{ activeSessions: 0, tokensUsed: 12500, memoryNodes: 0 }}
        accentClasses={accent}
        t={mockT}
      />,
    );
    expect(screen.getByText("12.5k")).toBeInTheDocument();
  });

  it("renders memory nodes count", () => {
    render(
      <MetricsGrid
        metrics={{ activeSessions: 0, tokensUsed: 0, memoryNodes: 128 }}
        accentClasses={accent}
        t={mockT}
      />,
    );
    expect(screen.getByText("128")).toBeInTheDocument();
  });

  it("renders sub-labels", () => {
    render(
      <MetricsGrid
        metrics={{ activeSessions: 0, tokensUsed: 0, memoryNodes: 0 }}
        accentClasses={accent}
        t={mockT}
      />,
    );
    expect(screen.getByText("Running in parallel")).toBeInTheDocument();
    expect(screen.getByText("Estimated cumulative")).toBeInTheDocument();
    expect(screen.getByText("Context entries saved")).toBeInTheDocument();
  });
});
