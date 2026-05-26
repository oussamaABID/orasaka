import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { ToastOverlay } from "@/features/jobs/components/ToastOverlay";

describe("ToastOverlay", () => {
  const mockRemove = jest.fn();

  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders empty when no toasts", () => {
    const { container } = render(
      <ToastOverlay toasts={[]} onRemoveToast={mockRemove} />,
    );
    const section = container.querySelector("section");
    expect(section).toBeInTheDocument();
    expect(section?.children).toHaveLength(0);
  });

  it("renders toast messages", () => {
    const toasts = [
      { id: "1", message: "Job completed", type: "success" as const },
      { id: "2", message: "Upload failed", type: "error" as const },
    ];
    render(<ToastOverlay toasts={toasts} onRemoveToast={mockRemove} />);
    expect(screen.getByText("Job completed")).toBeInTheDocument();
    expect(screen.getByText("Upload failed")).toBeInTheDocument();
  });

  it("calls onRemoveToast when close button is clicked", () => {
    const toasts = [
      { id: "toast-1", message: "Info msg", type: "info" as const },
    ];
    render(<ToastOverlay toasts={toasts} onRemoveToast={mockRemove} />);
    fireEvent.click(screen.getByRole("button", { name: "Dismiss notification" }));
    expect(mockRemove).toHaveBeenCalledWith("toast-1");
  });

  it("applies success styling for success type", () => {
    const toasts = [
      { id: "1", message: "Success!", type: "success" as const },
    ];
    const { container } = render(
      <ToastOverlay toasts={toasts} onRemoveToast={mockRemove} />,
    );
    const toastCard = container.querySelector("[role='alert']");
    expect(toastCard?.className).toContain("border-emerald");
  });

  it("applies error styling for error type", () => {
    const toasts = [
      { id: "1", message: "Error!", type: "error" as const },
    ];
    const { container } = render(
      <ToastOverlay toasts={toasts} onRemoveToast={mockRemove} />,
    );
    const toastCard = container.querySelector("[role='alert']");
    expect(toastCard?.className).toContain("border-red");
  });

  it("applies info styling for info type", () => {
    const toasts = [
      { id: "1", message: "Info!", type: "info" as const },
    ];
    const { container } = render(
      <ToastOverlay toasts={toasts} onRemoveToast={mockRemove} />,
    );
    const toastCard = container.querySelector("[role='alert']");
    expect(toastCard?.className).toContain("border-[var(--accent)]");
  });

  it("renders close button on each toast", () => {
    const toasts = [
      { id: "1", message: "Test", type: "info" as const },
    ];
    render(<ToastOverlay toasts={toasts} onRemoveToast={mockRemove} />);
    expect(screen.getByRole("button", { name: "Dismiss notification" })).toBeInTheDocument();
  });

  it("renders multiple toasts in order", () => {
    const toasts = [
      { id: "a", message: "First", type: "info" as const },
      { id: "b", message: "Second", type: "success" as const },
      { id: "c", message: "Third", type: "error" as const },
    ];
    render(<ToastOverlay toasts={toasts} onRemoveToast={mockRemove} />);
    const buttons = screen.getAllByRole("button", { name: "Dismiss notification" });
    expect(buttons).toHaveLength(3);
  });
});
