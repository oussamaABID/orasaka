import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { CopyableField } from "@/features/profile/components/ProfileViewParts";

const MockIcon = () => <span data-testid="mock-icon">icon</span>;

// Mock navigator.clipboard
Object.assign(navigator, {
  clipboard: {
    writeText: jest.fn().mockResolvedValue(undefined),
  },
});

describe("CopyableField", () => {
  it("renders label and value", () => {
    render(<CopyableField label="User ID" value="abc-123" icon={MockIcon} />);
    expect(screen.getByText("User ID")).toBeInTheDocument();
    expect(screen.getByText("abc-123")).toBeInTheDocument();
  });

  it("renders icon", () => {
    render(<CopyableField label="Email" value="a@b.com" icon={MockIcon} />);
    expect(screen.getByTestId("mock-icon")).toBeInTheDocument();
  });

  it("copies value on click", async () => {
    render(<CopyableField label="Token" value="secret-token" icon={MockIcon} />);
    fireEvent.click(screen.getByLabelText("Copy"));
    expect(navigator.clipboard.writeText).toHaveBeenCalledWith("secret-token");
  });

  it("applies mono styling when isMono is true", () => {
    render(<CopyableField label="API Key" value="key-123" icon={MockIcon} isMono />);
    const valueEl = screen.getByText("key-123");
    expect(valueEl.className).toContain("font-mono");
  });
});
