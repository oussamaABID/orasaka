import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { Button } from "@/components/ui/Button";

describe("Button", () => {
  it("renders button with text", () => {
    render(<Button>Click me</Button>);
    expect(screen.getByRole("button", { name: "Click me" })).toBeInTheDocument();
  });

  it("applies primary variant by default", () => {
    const { container } = render(<Button>Primary</Button>);
    const btn = container.querySelector("button");
    expect(btn?.className).toContain("bg-[var(--accent)]");
  });

  it("applies secondary variant", () => {
    const { container } = render(<Button variant="secondary">Secondary</Button>);
    const btn = container.querySelector("button");
    expect(btn?.className).toContain("bg-[var(--surface-2)]");
  });

  it("applies outline variant", () => {
    const { container } = render(<Button variant="outline">Outline</Button>);
    const btn = container.querySelector("button");
    expect(btn?.className).toContain("bg-transparent");
  });

  it("applies ghost variant", () => {
    const { container } = render(<Button variant="ghost">Ghost</Button>);
    const btn = container.querySelector("button");
    expect(btn?.className).toContain("bg-transparent");
  });

  it("applies sm size", () => {
    const { container } = render(<Button size="sm">Small</Button>);
    const btn = container.querySelector("button");
    expect(btn?.className).toContain("h-8");
  });

  it("applies md size by default", () => {
    const { container } = render(<Button>Medium</Button>);
    const btn = container.querySelector("button");
    expect(btn?.className).toContain("h-10");
  });

  it("applies lg size", () => {
    const { container } = render(<Button size="lg">Large</Button>);
    const btn = container.querySelector("button");
    expect(btn?.className).toContain("h-11");
  });

  it("applies icon size", () => {
    const { container } = render(<Button size="icon">🔍</Button>);
    const btn = container.querySelector("button");
    expect(btn?.className).toContain("w-8");
  });

  it("handles click events", () => {
    const onClick = jest.fn();
    render(<Button onClick={onClick}>Click</Button>);
    fireEvent.click(screen.getByRole("button"));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it("supports disabled state", () => {
    render(<Button disabled>Disabled</Button>);
    expect(screen.getByRole("button")).toBeDisabled();
  });

  it("forwards ref", () => {
    const ref = { current: null as HTMLButtonElement | null };
    render(<Button ref={ref}>Ref</Button>);
    expect(ref.current).toBeInstanceOf(HTMLButtonElement);
  });

  it("merges custom className", () => {
    const { container } = render(<Button className="extra">Extra</Button>);
    const btn = container.querySelector("button");
    expect(btn?.className).toContain("extra");
    expect(btn?.className).toContain("rounded-lg"); // still has base
  });
});
