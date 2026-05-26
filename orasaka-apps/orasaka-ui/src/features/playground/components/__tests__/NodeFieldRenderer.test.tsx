import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { NodeFieldRenderer } from "@/features/playground/components/NodeFieldRenderer";

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      playground: {
        enterValue: "Enter {field}",
        speechHelper: "Use sample text",
        videoExamplesLabel: "Examples",
        videoExampleVerification: "Verify example",
        videoExampleCinematic: "Cinematic example",
        videoExampleDynamic: "Dynamic example",
      },
    },
    locale: "en",
  }),
}));

jest.mock("@/features/playground/components/MediaUploadField", () => ({
  MediaUploadField: ({ field }: { field: string }) => (
    <div data-testid={`media-upload-${field}`}>MediaUpload: {field}</div>
  ),
}));

jest.mock("@/features/playground/components/NodeModelSelector", () => ({
  NodeModelSelector: () => <div data-testid="model-selector">ModelSelector</div>,
}));

jest.mock("@/core/constants/capability.constants", () => ({
  NODE_ID: { CHAT_SPEECH: "chat.speech", MEDIA_AUDIO: "media.audio", MEDIA_VIDEO: "media.video" },
  NODE_ICON: { MIC: "mic" },
}));

const baseProps = {
  nodeId: "chat.text",
  placeholders: ["prompt"],
  inputs: {} as Record<string, string>,
  isLocked: false,
  displayPending: false,
  category: "text",
  currentProvider: "ollama",
  uniqueProviders: ["ollama"],
  modelCatalog: [],
  uploadingFields: {} as Record<string, boolean>,
  onInputChange: jest.fn(),
  onFileUpload: jest.fn(),
  onExecute: jest.fn(),
};

describe("NodeFieldRenderer", () => {
  afterEach(() => jest.clearAllMocks());

  it("renders text input for regular placeholder", () => {
    render(<NodeFieldRenderer {...baseProps} />);
    expect(screen.getByPlaceholderText("Enter prompt")).toBeInTheDocument();
  });

  it("renders label for placeholder field", () => {
    render(<NodeFieldRenderer {...baseProps} />);
    expect(screen.getByText("prompt")).toBeInTheDocument();
  });

  it("calls onInputChange when text input changes", () => {
    render(<NodeFieldRenderer {...baseProps} />);
    fireEvent.change(screen.getByPlaceholderText("Enter prompt"), { target: { value: "hello" } });
    expect(baseProps.onInputChange).toHaveBeenCalledWith("prompt", "hello");
  });

  it("calls onExecute on Enter key", () => {
    render(<NodeFieldRenderer {...baseProps} />);
    fireEvent.keyDown(screen.getByPlaceholderText("Enter prompt"), { key: "Enter" });
    expect(baseProps.onExecute).toHaveBeenCalled();
  });

  it("does not call onExecute on Enter when locked", () => {
    render(<NodeFieldRenderer {...baseProps} isLocked={true} />);
    fireEvent.keyDown(screen.getByPlaceholderText("Enter prompt"), { key: "Enter" });
    expect(baseProps.onExecute).not.toHaveBeenCalled();
  });

  it("renders MediaUploadField for base64 fields", () => {
    render(<NodeFieldRenderer {...baseProps} placeholders={["imageBase64"]} />);
    expect(screen.getByTestId("media-upload-imageBase64")).toBeInTheDocument();
  });

  it("renders ModelSelector for model field", () => {
    render(<NodeFieldRenderer {...baseProps} placeholders={["model"]} />);
    expect(screen.getByTestId("model-selector")).toBeInTheDocument();
  });

  it("renders voice selector for voice field", () => {
    render(<NodeFieldRenderer {...baseProps} placeholders={["voice"]} />);
    expect(screen.getByText("alloy")).toBeInTheDocument();
    expect(screen.getByText("echo")).toBeInTheDocument();
  });

  it("uses model options for voice when available", () => {
    render(
      <NodeFieldRenderer
        {...baseProps}
        placeholders={["voice"]}
        inputs={{ model: "tts-1" }}
        modelCatalog={[
          { id: 1, modelName: "tts-1", modelLabel: "TTS", category: "speech", providerName: "openai", options: "nova,coral" },
        ]}
      />,
    );
    expect(screen.getByText("nova")).toBeInTheDocument();
    expect(screen.getByText("coral")).toBeInTheDocument();
  });

  it("renders speech helper for chat.speech node with text field", () => {
    render(
      <NodeFieldRenderer
        {...baseProps}
        nodeId="chat.speech"
        placeholders={["text"]}
      />,
    );
    expect(screen.getByText("Use sample text")).toBeInTheDocument();
  });

  it("renders video examples for media.video node with prompt field", () => {
    render(
      <NodeFieldRenderer
        {...baseProps}
        nodeId="media.video"
        placeholders={["prompt"]}
      />,
    );
    expect(screen.getByText("Examples")).toBeInTheDocument();
    expect(screen.getByText(/Verify example/)).toBeInTheDocument();
  });
});
