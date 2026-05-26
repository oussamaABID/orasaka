import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { ContextPlusMenu } from "@/features/chat-session/components/ContextPlusMenu";
import type { BootstrapFeature } from "@/features/chat-session/components/ContextPlusMenu";

const features: BootstrapFeature[] = [
  { id: "f1", label: "Generate Image", icon: "image", uriPath: "/ai", httpMethod: "POST", payloadTemplate: "{}" },
  { id: "f2", label: "Voice Chat", icon: "mic", uriPath: "/ai", httpMethod: "POST", payloadTemplate: "{}" },
];

const baseProps = {
  isOpen: true,
  onClose: jest.fn(),
  onExecuteNode: jest.fn(),
  features,
};

describe("ContextPlusMenu", () => {
  afterEach(() => jest.clearAllMocks());

  it("renders nothing when closed", () => {
    const { container } = render(<ContextPlusMenu {...baseProps} isOpen={false} />);
    expect(container.innerHTML).toBe("");
  });

  it("renders capabilities header", () => {
    render(<ContextPlusMenu {...baseProps} />);
    expect(screen.getByText("Capabilities")).toBeInTheDocument();
  });

  it("renders feature labels", () => {
    render(<ContextPlusMenu {...baseProps} />);
    expect(screen.getByText("Generate Image")).toBeInTheDocument();
    expect(screen.getByText("Voice Chat")).toBeInTheDocument();
  });

  it("renders emoji icons for features", () => {
    render(<ContextPlusMenu {...baseProps} />);
    expect(screen.getByText("🎨")).toBeInTheDocument();
    expect(screen.getByText("🎙️")).toBeInTheDocument();
  });

  it("calls onExecuteNode and onClose when feature clicked", () => {
    render(<ContextPlusMenu {...baseProps} />);
    fireEvent.click(screen.getByText("Generate Image"));
    expect(baseProps.onExecuteNode).toHaveBeenCalledWith(features[0]);
    expect(baseProps.onClose).toHaveBeenCalled();
  });

  it("shows empty state when no features", () => {
    render(<ContextPlusMenu {...baseProps} features={[]} />);
    expect(screen.getByText("No extra tools available")).toBeInTheDocument();
  });
});
