import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { NodeHints } from "@/features/playground/components/NodeHints";

jest.mock("@/core/constants/capability.constants", () => ({
  NODE_ID: {
    MEDIA_VISION: "media_vision",
    MEDIA_AUDIO: "media_audio",
    CHAT_IMAGE: "chat_image",
  },
}));

const mockT = {
  playground: {
    posterGuidelines: "Vision Guidelines",
    tryLabel: "Try:",
    audioGuidelines: "Audio Guidelines",
    imageTokensLabel: "Style Tokens",
    imageTokenIllustration: "illustration",
    imageTokenCyberpunk: "cyberpunk",
    imageTokenPhotorealistic: "photorealistic",
    imageTokenAntigravity: "antigravity",
    illustrationExamplePrompt: "A cat in a spacesuit",
  },
} as never;

describe("NodeHints", () => {
  it("renders vision hints for media_vision node", () => {
    render(
      <NodeHints
        nodeId="media_vision"
        isLocked={false}
        inputs={{}}
        onInputChange={jest.fn()}
        t={mockT}
      />,
    );
    expect(screen.getByText("Vision Guidelines")).toBeInTheDocument();
    expect(screen.getByText("Try:")).toBeInTheDocument();
  });

  it("calls onInputChange when vision try button is clicked", () => {
    const onInputChange = jest.fn();
    render(
      <NodeHints
        nodeId="media_vision"
        isLocked={false}
        inputs={{}}
        onInputChange={onInputChange}
        t={mockT}
      />,
    );
    const tryButton = screen.getByRole("button");
    fireEvent.click(tryButton);
    expect(onInputChange).toHaveBeenCalledWith(
      "prompt",
      "Identify elements in this poster",
    );
  });

  it("disables vision button when locked", () => {
    render(
      <NodeHints
        nodeId="media_vision"
        isLocked={true}
        inputs={{}}
        onInputChange={jest.fn()}
        t={mockT}
      />,
    );
    const button = screen.getByRole("button");
    expect(button).toBeDisabled();
  });

  it("renders audio hints for media_audio node", () => {
    render(
      <NodeHints
        nodeId="media_audio"
        isLocked={false}
        inputs={{}}
        onInputChange={jest.fn()}
        t={mockT}
      />,
    );
    expect(screen.getByText("Audio Guidelines")).toBeInTheDocument();
  });

  it("renders image tokens for chat_image node", () => {
    render(
      <NodeHints
        nodeId="chat_image"
        isLocked={false}
        inputs={{}}
        onInputChange={jest.fn()}
        t={mockT}
      />,
    );
    expect(screen.getByText("Style Tokens")).toBeInTheDocument();
    expect(screen.getByText("+illustration")).toBeInTheDocument();
    expect(screen.getByText("+cyberpunk")).toBeInTheDocument();
    expect(screen.getByText("+photorealistic")).toBeInTheDocument();
    expect(screen.getByText("+antigravity")).toBeInTheDocument();
  });

  it("appends token to existing prompt when token button clicked", () => {
    const onInputChange = jest.fn();
    render(
      <NodeHints
        nodeId="chat_image"
        isLocked={false}
        inputs={{ prompt: "A cat" }}
        onInputChange={onInputChange}
        t={mockT}
      />,
    );
    fireEvent.click(screen.getByText("+illustration"));
    expect(onInputChange).toHaveBeenCalledWith(
      "prompt",
      "A cat, illustration",
    );
  });

  it("sets token as prompt when no existing prompt", () => {
    const onInputChange = jest.fn();
    render(
      <NodeHints
        nodeId="chat_image"
        isLocked={false}
        inputs={{}}
        onInputChange={onInputChange}
        t={mockT}
      />,
    );
    fireEvent.click(screen.getByText("+cyberpunk"));
    expect(onInputChange).toHaveBeenCalledWith("prompt", "cyberpunk");
  });

  it("renders nothing for unknown node", () => {
    const { container } = render(
      <NodeHints
        nodeId="unknown_node"
        isLocked={false}
        inputs={{}}
        onInputChange={jest.fn()}
        t={mockT}
      />,
    );
    expect(container.textContent).toBe("");
  });
});
