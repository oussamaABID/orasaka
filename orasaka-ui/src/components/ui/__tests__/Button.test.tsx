import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import { Button } from "@/components/ui/Button";

describe("Button", () => {
  it("renders with default variant and size", () => {
    render(<Button>Click me</Button>);

    const button = screen.getByRole("button", { name: /click me/i });
    expect(button).toBeInTheDocument();
  });

  it("renders with primary variant by default", () => {
    render(<Button>Submit</Button>);

    const button = screen.getByRole("button", { name: /submit/i });
    expect(button.className).toContain("bg-[var(--accent)]");
  });

  it("applies outline variant styles", () => {
    render(<Button variant="outline">Cancel</Button>);

    const button = screen.getByRole("button", { name: /cancel/i });
    expect(button.className).toContain("border");
    expect(button.className).toContain("bg-transparent");
  });

  it("applies size classes correctly", () => {
    render(<Button size="lg">Large</Button>);

    const button = screen.getByRole("button", { name: /large/i });
    expect(button.className).toContain("h-11");
  });

  it("forwards disabled state to native button", () => {
    render(<Button disabled>Disabled</Button>);

    const button = screen.getByRole("button", { name: /disabled/i });
    expect(button).toBeDisabled();
  });

  it("merges custom className with internal styles", () => {
    render(<Button className="custom-class">Merge</Button>);

    const button = screen.getByRole("button", { name: /merge/i });
    expect(button.className).toContain("custom-class");
  });
});
