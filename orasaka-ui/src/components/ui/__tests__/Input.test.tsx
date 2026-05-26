import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { Input } from "@/components/ui/Input";

describe("Input", () => {
  it("renders an input element", () => {
    render(<Input placeholder="Type here" />);
    const input = screen.getByPlaceholderText("Type here");
    expect(input).toBeInTheDocument();
    expect(input.tagName).toBe("INPUT");
  });

  it("applies default styling", () => {
    render(<Input placeholder="test" />);
    const input = screen.getByPlaceholderText("test");
    expect(input.className).toContain("h-11");
    expect(input.className).toContain("rounded-lg");
    expect(input.className).toContain("border");
  });

  it("merges custom className", () => {
    render(<Input className="custom-input" placeholder="test" />);
    const input = screen.getByPlaceholderText("test");
    expect(input.className).toContain("custom-input");
    expect(input.className).toContain("h-11"); // still has default
  });

  it("passes type prop", () => {
    render(<Input type="password" placeholder="pass" />);
    const input = screen.getByPlaceholderText("pass");
    expect(input).toHaveAttribute("type", "password");
  });

  it("passes disabled state", () => {
    render(<Input disabled placeholder="disabled" />);
    const input = screen.getByPlaceholderText("disabled");
    expect(input).toBeDisabled();
  });

  it("forwards ref correctly", () => {
    const ref = { current: null as HTMLInputElement | null };
    render(<Input ref={ref} placeholder="ref-test" />);
    expect(ref.current).toBeInstanceOf(HTMLInputElement);
  });

  it("handles onChange event", () => {
    const onChange = jest.fn();
    render(<Input onChange={onChange} placeholder="change-test" />);
    fireEvent.change(screen.getByPlaceholderText("change-test"), {
      target: { value: "new value" },
    });
    expect(onChange).toHaveBeenCalledTimes(1);
  });

  it("applies default type when not specified", () => {
    render(<Input placeholder="no-type" />);
    const input = screen.getByPlaceholderText("no-type");
    // type is undefined by default, browser defaults to "text"
    expect(input.getAttribute("type")).toBeNull();
  });
});
