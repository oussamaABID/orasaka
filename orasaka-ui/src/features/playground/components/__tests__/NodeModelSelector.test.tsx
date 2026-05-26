import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { NodeModelSelector } from "@/features/playground/components/NodeModelSelector";

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      playground: {
        providerLabel: "Provider",
        modelSelectorLabel: "Model",
      },
    },
    locale: "en",
  }),
}));

jest.mock("@/core/constants/capability.constants", () => ({
  FALLBACK_MODELS: {
    text: ["llama3", "mistral"],
    image: ["sdxl"],
  },
  COMMERCIAL_DEFAULTS: {
    openai: {
      text: "gpt-4o",
      image: "dall-e-3",
      speech: "tts-1",
      audio: "whisper-1",
      default: "gpt-4o",
    },
    gemini: {
      default: "gemini-1.5-pro",
    },
    anthropic: {
      default: "claude-3-5-sonnet-latest",
    },
  },
}));

const catalogModels = [
  { id: 1, modelName: "llama3:latest", modelLabel: "Llama 3", category: "text", providerName: "ollama" },
  { id: 2, modelName: "sdxl", modelLabel: "SDXL", category: "image", providerName: "localai" },
  { id: 3, modelName: "gpt-4o", modelLabel: "GPT-4o", category: "text", providerName: "openai" },
];

describe("NodeModelSelector", () => {
  it("renders provider and model labels", () => {
    render(
      <NodeModelSelector
        category="text"
        currentProvider="ollama"
        uniqueProviders={["ollama", "openai"]}
        modelCatalog={catalogModels}
        inputs={{}}
        isLocked={false}
        onInputChange={jest.fn()}
      />,
    );
    expect(screen.getByText("Provider")).toBeInTheDocument();
    expect(screen.getByText("Model")).toBeInTheDocument();
  });

  it("renders provider options", () => {
    render(
      <NodeModelSelector
        category="text"
        currentProvider="ollama"
        uniqueProviders={["ollama", "openai"]}
        modelCatalog={catalogModels}
        inputs={{}}
        isLocked={false}
        onInputChange={jest.fn()}
      />,
    );
    expect(screen.getByText("OLLAMA")).toBeInTheDocument();
    expect(screen.getByText("OPENAI")).toBeInTheDocument();
  });

  it("renders filtered model options for current provider", () => {
    render(
      <NodeModelSelector
        category="text"
        currentProvider="ollama"
        uniqueProviders={["ollama"]}
        modelCatalog={catalogModels}
        inputs={{}}
        isLocked={false}
        onInputChange={jest.fn()}
      />,
    );
    expect(screen.getByText("Llama 3")).toBeInTheDocument();
  });

  it("falls back to commercial defaults when no models for provider", () => {
    render(
      <NodeModelSelector
        category="text"
        currentProvider="gemini"
        uniqueProviders={["gemini"]}
        modelCatalog={[]}
        inputs={{}}
        isLocked={false}
        onInputChange={jest.fn()}
      />,
    );
    expect(screen.getByText("gemini-1.5-pro")).toBeInTheDocument();
  });

  it("falls back to FALLBACK_MODELS when no commercial models either", () => {
    render(
      <NodeModelSelector
        category="text"
        currentProvider="custom"
        uniqueProviders={["custom"]}
        modelCatalog={[]}
        inputs={{}}
        isLocked={false}
        onInputChange={jest.fn()}
      />,
    );
    expect(screen.getByText("llama3")).toBeInTheDocument();
    expect(screen.getByText("mistral")).toBeInTheDocument();
  });

  it("calls onInputChange when model select changes", () => {
    const onChange = jest.fn();
    render(
      <NodeModelSelector
        category="text"
        currentProvider="ollama"
        uniqueProviders={["ollama"]}
        modelCatalog={catalogModels}
        inputs={{}}
        isLocked={false}
        onInputChange={onChange}
      />,
    );
    const selects = screen.getAllByRole("combobox");
    const modelSelect = selects[1]; // second select is model
    fireEvent.change(modelSelect, { target: { value: "llama3:latest" } });
    expect(onChange).toHaveBeenCalledWith("model", "llama3:latest");
  });

  it("calls onInputChange with provider model default on provider change", () => {
    const onChange = jest.fn();
    render(
      <NodeModelSelector
        category="text"
        currentProvider="ollama"
        uniqueProviders={["ollama", "openai"]}
        modelCatalog={catalogModels}
        inputs={{}}
        isLocked={false}
        onInputChange={onChange}
      />,
    );
    const selects = screen.getAllByRole("combobox");
    fireEvent.change(selects[0], { target: { value: "openai" } });
    expect(onChange).toHaveBeenCalledWith("model", "gpt-4o");
  });

  it("disables selects when locked", () => {
    render(
      <NodeModelSelector
        category="text"
        currentProvider="ollama"
        uniqueProviders={["ollama"]}
        modelCatalog={catalogModels}
        inputs={{}}
        isLocked={true}
        onInputChange={jest.fn()}
      />,
    );
    const selects = screen.getAllByRole("combobox");
    expect(selects[0]).toBeDisabled();
    expect(selects[1]).toBeDisabled();
  });

  it("uses inputs.model as selected value when present", () => {
    render(
      <NodeModelSelector
        category="text"
        currentProvider="ollama"
        uniqueProviders={["ollama"]}
        modelCatalog={catalogModels}
        inputs={{ model: "llama3:latest" }}
        isLocked={false}
        onInputChange={jest.fn()}
      />,
    );
    const selects = screen.getAllByRole("combobox");
    expect(selects[1]).toHaveValue("llama3:latest");
  });

  it("shows anthropic models for anthropic provider", () => {
    render(
      <NodeModelSelector
        category="text"
        currentProvider="anthropic"
        uniqueProviders={["anthropic"]}
        modelCatalog={[]}
        inputs={{}}
        isLocked={false}
        onInputChange={jest.fn()}
      />,
    );
    expect(screen.getByText("claude-3-5-sonnet-latest")).toBeInTheDocument();
  });

  it("shows openai speech models for speech category", () => {
    render(
      <NodeModelSelector
        category="speech"
        currentProvider="openai"
        uniqueProviders={["openai"]}
        modelCatalog={[]}
        inputs={{}}
        isLocked={false}
        onInputChange={jest.fn()}
      />,
    );
    expect(screen.getByText("tts-1")).toBeInTheDocument();
  });

  it("shows openai audio models for audio category", () => {
    render(
      <NodeModelSelector
        category="audio"
        currentProvider="openai"
        uniqueProviders={["openai"]}
        modelCatalog={[]}
        inputs={{}}
        isLocked={false}
        onInputChange={jest.fn()}
      />,
    );
    expect(screen.getByText("whisper-1")).toBeInTheDocument();
  });
});
