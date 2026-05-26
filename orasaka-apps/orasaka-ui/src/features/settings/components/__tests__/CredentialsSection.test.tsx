import "@testing-library/jest-dom";
import { render, screen, waitFor } from "@testing-library/react";
import { CredentialsSection } from "@/features/settings/components/CredentialsSection";

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      settings: {
        credentialsTitle: "API Keys",
        credentialsDesc: "Configure your provider API keys",
        credentialsSaved: "Saved successfully",
        credentialsPlaceholder: "Enter API key",
        geminiKey: "Gemini API Key",
        anthropicKey: "Anthropic API Key",
        openaiKey: "OpenAI API Key",
      },
      errors: { generic: "An error occurred" },
    },
    locale: "en",
  }),
}));

jest.mock("@/components/ui/Input", () => ({
  Input: (props: React.InputHTMLAttributes<HTMLInputElement>) => <input {...props} />,
}));

jest.mock("@/components/ui/Button", () => ({
  Button: ({ children, ...props }: { children: React.ReactNode } & React.ButtonHTMLAttributes<HTMLButtonElement>) => (
    <button {...props}>{children}</button>
  ),
}));

const mockCredentials = [
  { providerName: "gemini", configured: true },
  { providerName: "anthropic", configured: false },
  { providerName: "openai", configured: true },
];

describe("CredentialsSection", () => {
  beforeEach(() => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockCredentials),
    }) as jest.Mock;
  });

  afterEach(() => jest.restoreAllMocks());

  it("renders title and description", async () => {
    const fetchHeaders = jest.fn().mockResolvedValue({ Authorization: "Bearer x" });
    render(<CredentialsSection fetchHeaders={fetchHeaders} />);
    expect(screen.getByText("API Keys")).toBeInTheDocument();
    expect(screen.getByText("Configure your provider API keys")).toBeInTheDocument();
  });

  it("renders all three provider labels", async () => {
    const fetchHeaders = jest.fn().mockResolvedValue({});
    render(<CredentialsSection fetchHeaders={fetchHeaders} />);
    expect(screen.getByText("Gemini API Key")).toBeInTheDocument();
    expect(screen.getByText("Anthropic API Key")).toBeInTheDocument();
    expect(screen.getByText("OpenAI API Key")).toBeInTheDocument();
  });

  it("shows Clear button for configured providers", async () => {
    const fetchHeaders = jest.fn().mockResolvedValue({});
    render(<CredentialsSection fetchHeaders={fetchHeaders} />);
    await waitFor(() => {
      const clearBtns = screen.getAllByText("Clear");
      expect(clearBtns).toHaveLength(2); // gemini + openai
    });
  });

  it("shows Save buttons for unconfigured providers", async () => {
    const fetchHeaders = jest.fn().mockResolvedValue({});
    render(<CredentialsSection fetchHeaders={fetchHeaders} />);
    await waitFor(() => {
      const saveBtns = screen.getAllByText("Save");
      expect(saveBtns.length).toBeGreaterThanOrEqual(1); // anthropic at minimum
    });
  });

  it("renders placeholder text in inputs", () => {
    const fetchHeaders = jest.fn().mockResolvedValue({});
    render(<CredentialsSection fetchHeaders={fetchHeaders} />);
    const inputs = screen.getAllByPlaceholderText("Enter API key");
    expect(inputs).toHaveLength(3);
  });

  it("calls fetchHeaders on mount", async () => {
    const fetchHeaders = jest.fn().mockResolvedValue({});
    render(<CredentialsSection fetchHeaders={fetchHeaders} />);
    await waitFor(() => {
      expect(fetchHeaders).toHaveBeenCalled();
    });
  });
});
