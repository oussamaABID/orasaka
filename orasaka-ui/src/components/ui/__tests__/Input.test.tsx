import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import { Input } from "@/components/ui/Input";

describe("Input", () => {
  it("renders a text input by default", () => {
    render(<Input placeholder="Enter text" />);

    const input = screen.getByPlaceholderText("Enter text");
    expect(input).toBeInTheDocument();
    expect(input.tagName).toBe("INPUT");
  });

  it("renders with specified type", () => {
    render(<Input type="email" placeholder="email" />);

    const input = screen.getByPlaceholderText("email");
    expect(input).toHaveAttribute("type", "email");
  });

  it("forwards disabled state", () => {
    render(<Input disabled placeholder="disabled" />);

    const input = screen.getByPlaceholderText("disabled");
    expect(input).toBeDisabled();
  });

  it("merges custom className", () => {
    render(<Input className="extra" placeholder="merge" />);

    const input = screen.getByPlaceholderText("merge");
    expect(input.className).toContain("extra");
  });
});
