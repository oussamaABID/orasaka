import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import { AiProvidersSection } from "@/features/settings/components/AiProvidersSection";

jest.mock("next-auth/react", () => ({
  getSession: jest.fn().mockResolvedValue({ user: { id: "u1" } }),
}));

jest.mock("@/features/tenant/context/TenantContext", () => ({
  useTenant: () => ({
    accentClasses: { accentGradient: "from-zinc-500 to-zinc-600" },
  }),
}));

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      providers: {
        title: "AI Providers",
        subtitle: "Connect your AI providers",
        addCustom: "Add Custom Provider",
      },
    },
    locale: "en",
  }),
}));

jest.mock("@/components/ui/Card", () => ({
  Card: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  CardContent: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  CardDescription: ({ children }: { children: React.ReactNode }) => <p>{children}</p>,
  CardHeader: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  CardTitle: ({ children }: { children: React.ReactNode }) => <h3>{children}</h3>,
}));

jest.mock("@/features/settings/components/AiProvidersParts", () => ({
  PROVIDERS: [
    { id: "gemini", name: "Gemini", icon: "G", gradient: "from-blue-500 to-blue-600" },
    { id: "claude", name: "Claude", icon: "C", gradient: "from-orange-500 to-orange-600" },
    { id: "openai", name: "OpenAI", icon: "O", gradient: "from-green-500 to-green-600" },
  ],
  ProviderCard: ({ provider }: { provider: { name: string } }) => (
    <div data-testid={`provider-${provider.name.toLowerCase()}`}>{provider.name}</div>
  ),
}));

describe("AiProvidersSection", () => {
  beforeEach(() => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({}),
    }) as jest.Mock;
  });

  afterEach(() => jest.restoreAllMocks());

  it("renders title and subtitle", () => {
    render(<AiProvidersSection />);
    expect(screen.getByText("AI Providers")).toBeInTheDocument();
    expect(screen.getByText("Connect your AI providers")).toBeInTheDocument();
  });

  it("renders all provider cards", () => {
    render(<AiProvidersSection />);
    expect(screen.getByText("Gemini")).toBeInTheDocument();
    expect(screen.getByText("Claude")).toBeInTheDocument();
    expect(screen.getByText("OpenAI")).toBeInTheDocument();
  });

  it("renders add custom provider button", () => {
    render(<AiProvidersSection />);
    expect(screen.getByText("Add Custom Provider")).toBeInTheDocument();
  });
});
