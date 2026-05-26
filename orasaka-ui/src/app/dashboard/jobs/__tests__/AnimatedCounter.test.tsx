import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import { AnimatedCounter } from "@/app/dashboard/jobs/AnimatedCounter";

// Mock requestAnimationFrame to instantly execute
let rafCallback: ((time: number) => void) | null = null;
beforeEach(() => {
  jest.spyOn(window, "requestAnimationFrame").mockImplementation((cb) => {
    rafCallback = cb;
    // Execute immediately with a large time to complete animation
    cb(performance.now() + 1000);
    return 1;
  });
  jest.spyOn(window, "cancelAnimationFrame").mockImplementation(() => {});
});

afterEach(() => {
  jest.restoreAllMocks();
  rafCallback = null;
});

describe("AnimatedCounter", () => {
  it("renders the value", () => {
    render(<AnimatedCounter value={42} />);
    expect(screen.getByText("42")).toBeInTheDocument();
  });

  it("renders the suffix", () => {
    render(<AnimatedCounter value={95} suffix="%" />);
    expect(screen.getByText(/95/)).toBeInTheDocument();
    expect(screen.getByText(/%/)).toBeInTheDocument();
  });

  it("renders 0 for zero value", () => {
    render(<AnimatedCounter value={0} />);
    expect(screen.getByText("0")).toBeInTheDocument();
  });

  it("renders with custom duration", () => {
    render(<AnimatedCounter value={100} duration={200} />);
    expect(screen.getByText("100")).toBeInTheDocument();
  });

  it("renders with empty suffix by default", () => {
    const { container } = render(<AnimatedCounter value={50} />);
    const span = container.querySelector("span");
    expect(span?.textContent).toBe("50");
  });
});
