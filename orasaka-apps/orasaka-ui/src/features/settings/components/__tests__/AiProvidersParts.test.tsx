import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { ProviderCard, PROVIDERS } from "@/features/settings/components/AiProvidersParts";
import type { ProviderState } from "@/features/settings/components/AiProvidersParts";

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      providers: {
        connected: "Connected",
        notConfigured: "Not configured",
        deleteProvider: "Delete",
        keyHide: "Hide",
        keyReveal: "Reveal",
        testing: "Testing...",
        testConnection: "Test",
        testSuccess: "Connection OK",
        testFailed: "Connection failed",
      },
      settings: {
        saveCredentials: "Save",
      },
    },
    locale: "en",
  }),
}));

const gemini = PROVIDERS[0]; // Google Gemini

const baseState: ProviderState = {
  key: "",
  isRevealed: false,
  isTesting: false,
  testResult: null,
  isSaved: false,
};

describe("ProviderCard", () => {
  const defaultProps = {
    provider: gemini,
    state: baseState,
    onKeyChange: jest.fn(),
    onToggleReveal: jest.fn(),
    onTest: jest.fn(),
    onSave: jest.fn(),
    onDelete: jest.fn(),
    disabled: false,
  };

  afterEach(() => jest.clearAllMocks());

  it("renders provider name", () => {
    render(<ProviderCard {...defaultProps} />);
    expect(screen.getByText("Google Gemini")).toBeInTheDocument();
  });

  it("shows 'Not configured' when key is empty", () => {
    render(<ProviderCard {...defaultProps} />);
    expect(screen.getByText("Not configured")).toBeInTheDocument();
  });

  it("shows 'Connected' when saved with key", () => {
    render(
      <ProviderCard
        {...defaultProps}
        state={{ ...baseState, key: "AIza123", isSaved: true }}
      />,
    );
    expect(screen.getByText(/Connected/)).toBeInTheDocument();
  });

  it("renders input with password type by default", () => {
    render(
      <ProviderCard {...defaultProps} state={{ ...baseState, key: "secret" }} />,
    );
    const input = screen.getByPlaceholderText(gemini.placeholder);
    expect(input).toHaveAttribute("type", "password");
  });

  it("reveals key when isRevealed is true", () => {
    render(
      <ProviderCard
        {...defaultProps}
        state={{ ...baseState, key: "secret", isRevealed: true }}
      />,
    );
    const input = screen.getByPlaceholderText(gemini.placeholder);
    expect(input).toHaveAttribute("type", "text");
  });

  it("calls onKeyChange when input value changes", () => {
    render(<ProviderCard {...defaultProps} />);
    fireEvent.change(screen.getByPlaceholderText(gemini.placeholder), {
      target: { value: "new-key" },
    });
    expect(defaultProps.onKeyChange).toHaveBeenCalledWith("new-key");
  });

  it("calls onToggleReveal when eye button clicked", () => {
    render(
      <ProviderCard {...defaultProps} state={{ ...baseState, key: "s" }} />,
    );
    fireEvent.click(screen.getByLabelText("Reveal"));
    expect(defaultProps.onToggleReveal).toHaveBeenCalled();
  });

  it("disables save when no key", () => {
    render(<ProviderCard {...defaultProps} />);
    expect(screen.getByText("Save")).toBeDisabled();
  });

  it("enables save when key is present", () => {
    render(
      <ProviderCard {...defaultProps} state={{ ...baseState, key: "key" }} />,
    );
    expect(screen.getByText("Save")).not.toBeDisabled();
  });

  it("calls onSave when save clicked", () => {
    render(
      <ProviderCard {...defaultProps} state={{ ...baseState, key: "key" }} />,
    );
    fireEvent.click(screen.getByText("Save"));
    expect(defaultProps.onSave).toHaveBeenCalled();
  });

  it("calls onTest when test clicked", () => {
    render(
      <ProviderCard {...defaultProps} state={{ ...baseState, key: "key" }} />,
    );
    fireEvent.click(screen.getByText("Test"));
    expect(defaultProps.onTest).toHaveBeenCalled();
  });

  it("shows testing state", () => {
    render(
      <ProviderCard
        {...defaultProps}
        state={{ ...baseState, key: "key", isTesting: true }}
      />,
    );
    expect(screen.getByText("Testing...")).toBeInTheDocument();
  });

  it("shows test success result", () => {
    render(
      <ProviderCard
        {...defaultProps}
        state={{ ...baseState, key: "key", testResult: "success" }}
      />,
    );
    expect(screen.getByText("Connection OK")).toBeInTheDocument();
  });

  it("shows test error result", () => {
    render(
      <ProviderCard
        {...defaultProps}
        state={{ ...baseState, key: "key", testResult: "error" }}
      />,
    );
    expect(screen.getByText("Connection failed")).toBeInTheDocument();
  });

  it("shows delete button when connected", () => {
    render(
      <ProviderCard
        {...defaultProps}
        state={{ ...baseState, key: "key", isSaved: true }}
      />,
    );
    expect(screen.getByLabelText("Delete")).toBeInTheDocument();
  });

  it("calls onDelete when delete clicked", () => {
    render(
      <ProviderCard
        {...defaultProps}
        state={{ ...baseState, key: "key", isSaved: true }}
      />,
    );
    fireEvent.click(screen.getByLabelText("Delete"));
    expect(defaultProps.onDelete).toHaveBeenCalled();
  });
});

describe("PROVIDERS config", () => {
  it("has 6 providers", () => {
    expect(PROVIDERS).toHaveLength(6);
  });

  it("includes expected provider ids", () => {
    const ids = PROVIDERS.map((p) => p.id);
    expect(ids).toEqual(["gemini", "claude", "openai", "mistral", "groq", "ollama"]);
  });
});
