import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { ThemePreviewCard } from "@/features/settings/components/ThemePreviewCard";

const defaultProps = {
  value: "dark" as const,
  label: "Dark Mode",
  desc: "Easy on the eyes",
  icon: <span data-testid="icon">🌙</span>,
  preview: {
    sidebar: "bg-zinc-900",
    header: "bg-zinc-800",
    body: "bg-zinc-850",
    accent: "bg-amber-500",
    text: "bg-zinc-200",
  },
  isActive: false,
  onClick: jest.fn(),
  index: 0,
  clickToApplyLabel: "Click to apply",
};

describe("ThemePreviewCard", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders label and description", () => {
    render(<ThemePreviewCard {...defaultProps} />);
    expect(screen.getByText("Dark Mode")).toBeInTheDocument();
    expect(screen.getByText("Easy on the eyes")).toBeInTheDocument();
  });

  it("renders the icon", () => {
    render(<ThemePreviewCard {...defaultProps} />);
    expect(screen.getByTestId("icon")).toBeInTheDocument();
  });

  it("calls onClick when clicked (not active)", () => {
    const onClick = jest.fn();
    render(<ThemePreviewCard {...defaultProps} onClick={onClick} />);
    fireEvent.click(screen.getByRole("button"));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it("does NOT call onClick when already active", () => {
    const onClick = jest.fn();
    render(
      <ThemePreviewCard {...defaultProps} isActive={true} onClick={onClick} />,
    );
    fireEvent.click(screen.getByRole("button"));
    expect(onClick).not.toHaveBeenCalled();
  });

  it("shows checkmark when active", () => {
    const { container } = render(
      <ThemePreviewCard {...defaultProps} isActive={true} />,
    );
    const mark = container.querySelector("mark");
    expect(mark).toBeInTheDocument();
  });

  it("does not show checkmark when not active", () => {
    const { container } = render(
      <ThemePreviewCard {...defaultProps} isActive={false} />,
    );
    const mark = container.querySelector("mark");
    expect(mark).not.toBeInTheDocument();
  });

  it("shows hover overlay when not active", () => {
    render(<ThemePreviewCard {...defaultProps} isActive={false} />);
    expect(screen.getByText("Click to apply")).toBeInTheDocument();
  });

  it("does not show hover overlay when active", () => {
    render(<ThemePreviewCard {...defaultProps} isActive={true} />);
    expect(screen.queryByText("Click to apply")).not.toBeInTheDocument();
  });

  it("applies active class when isActive", () => {
    render(<ThemePreviewCard {...defaultProps} isActive={true} />);
    const button = screen.getByRole("button");
    expect(button.className).toContain("theme-card-active");
  });

  it("applies animation delay based on index", () => {
    render(<ThemePreviewCard {...defaultProps} index={3} />);
    const button = screen.getByRole("button");
    expect(button.style.animationDelay).toBe("150ms");
  });
});
