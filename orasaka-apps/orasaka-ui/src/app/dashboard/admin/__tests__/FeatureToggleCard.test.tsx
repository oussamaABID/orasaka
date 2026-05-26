import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { FeatureToggleCard } from "@/app/dashboard/admin/FeatureToggleCard";

const mockT = {
  admin: {
    featureOverridesTitle: "Feature Overrides",
    featureNoOverrides: "No feature overrides configured",
    featureOverrideEnabled: "Enabled",
  },
} as never;

describe("FeatureToggleCard", () => {
  it("renders the title", () => {
    render(<FeatureToggleCard features={[]} onToggle={jest.fn()} t={mockT} />);
    expect(screen.getByText("Feature Overrides")).toBeInTheDocument();
  });

  it("shows empty state when no features", () => {
    render(<FeatureToggleCard features={[]} onToggle={jest.fn()} t={mockT} />);
    expect(screen.getByText("No feature overrides configured")).toBeInTheDocument();
  });

  it("renders feature items", () => {
    const features = [
      { featureKey: "chat.text", isEnabled: true },
      { featureKey: "media.video", isEnabled: false },
    ];
    render(<FeatureToggleCard features={features} onToggle={jest.fn()} t={mockT} />);
    expect(screen.getByText("chat.text")).toBeInTheDocument();
    expect(screen.getByText("media.video")).toBeInTheDocument();
  });

  it("renders checkboxes for each feature", () => {
    const features = [
      { featureKey: "chat.text", isEnabled: true },
      { featureKey: "media.video", isEnabled: false },
    ];
    render(<FeatureToggleCard features={features} onToggle={jest.fn()} t={mockT} />);
    const checkboxes = screen.getAllByRole("checkbox");
    expect(checkboxes).toHaveLength(2);
    expect(checkboxes[0]).toBeChecked();
    expect(checkboxes[1]).not.toBeChecked();
  });

  it("calls onToggle when checkbox changed", () => {
    const onToggle = jest.fn();
    const features = [{ featureKey: "chat.text", isEnabled: true }];
    render(<FeatureToggleCard features={features} onToggle={onToggle} t={mockT} />);
    fireEvent.click(screen.getByRole("checkbox"));
    expect(onToggle).toHaveBeenCalledWith("chat.text", true);
  });
});
