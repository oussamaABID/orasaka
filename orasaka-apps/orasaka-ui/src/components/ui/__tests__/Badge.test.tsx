import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import { Badge } from "@/components/ui/Badge";

describe("Badge", () => {
  it("renders children text", () => {
    render(<Badge>Active</Badge>);
    expect(screen.getByText("Active")).toBeInTheDocument();
  });

  it("applies default variant styling", () => {
    const { container } = render(<Badge>Default</Badge>);
    const span = container.querySelector("span");
    expect(span?.className).toContain("rounded-full");
  });

  it("applies success variant", () => {
    const { container } = render(<Badge variant="success">OK</Badge>);
    const span = container.querySelector("span");
    expect(span?.className).toContain("emerald");
  });

  it("applies warning variant", () => {
    const { container } = render(<Badge variant="warning">Warn</Badge>);
    const span = container.querySelector("span");
    expect(span?.className).toContain("amber");
  });

  it("applies danger variant", () => {
    const { container } = render(<Badge variant="danger">Error</Badge>);
    const span = container.querySelector("span");
    expect(span?.className).toContain("red");
  });

  it("applies accent variant", () => {
    const { container } = render(<Badge variant="accent">AI</Badge>);
    const span = container.querySelector("span");
    expect(span?.className).toContain("accent");
  });

  it("merges custom className", () => {
    const { container } = render(<Badge className="custom-class">Custom</Badge>);
    const span = container.querySelector("span");
    expect(span?.className).toContain("custom-class");
  });

  it("passes additional HTML attributes", () => {
    render(<Badge data-testid="badge-test">Test</Badge>);
    expect(screen.getByTestId("badge-test")).toBeInTheDocument();
  });
});
