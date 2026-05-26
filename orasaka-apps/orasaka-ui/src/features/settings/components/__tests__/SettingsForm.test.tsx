import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import { SettingsForm } from "@/features/settings/components/SettingsForm";

jest.mock("next-auth/react", () => ({
  getSession: jest.fn().mockResolvedValue({ user: { id: "u1" } }),
}));

jest.mock("@/features/settings/hooks/useSettings", () => ({
  useSettings: () => ({
    settings: {
      language: "en",
      aiPersona: "standard",
      themeName: "Orasaka",
      themeTagline: "Decoupled Intelligence",
      themeAccent: "zinc",
      themeLayout: "standard",
      theme: "system",
      tenantId: "orasaka-default",
    },
    isLoading: false,
    updateSettings: jest.fn(),
    isUpdating: false,
  }),
}));

jest.mock("@/core/context/TenantContext", () => ({
  useTenant: () => ({
    config: { displayName: "Orasaka" },
    accentClasses: { bg: "bg-zinc-600", hoverBg: "hover:bg-zinc-700" },
  }),
}));

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      settings: {
        title: "Settings",
        description: "Manage your preferences",
        language: "Language",
        english: "English",
        french: "French",
        aiPersona: "AI Persona",
        tenantBranding: "Branding",
        appName: "App Name",
        appNamePlaceholder: "Enter name",
        tagline: "Tagline",
        taglinePlaceholder: "Enter tagline",
        tenantId: "Tenant ID",
        tenantIdPlaceholder: "Enter tenant ID",
        colorAccent: "Accent",
        layoutScale: "Layout",
        saveTheme: "Save Changes",
        saving: "Saving...",
        standard: "Standard",
        technical: "Technical",
        creative: "Creative",
        zinc: "Zinc",
        violet: "Violet",
        emerald: "Emerald",
        compact: "Compact",
        credentialsTitle: "Credentials",
        credentialsDesc: "Desc",
        credentialsSaved: "Saved",
        credentialsPlaceholder: "Enter key",
        geminiKey: "Gemini",
        anthropicKey: "Anthropic",
        openaiKey: "OpenAI",
        mcpTitle: "MCP",
        mcpDesc: "Desc",
        mcpAdd: "Add",
        mcpNoServers: "No servers",
      },
      errors: { generic: "Error" },
    },
    locale: "en",
    setLocale: jest.fn(),
  }),
}));

jest.mock("@/core/providers/ThemeProvider", () => ({
  useTheme: () => ({ theme: "system", setTheme: jest.fn() }),
}));

jest.mock("@/constants/settings.constants", () => ({
  AI_PERSONAS: ["standard", "technical", "creative"],
  AI_PERSONA_LABELS: {
    standard: "settings.standard",
    technical: "settings.technical",
    creative: "settings.creative",
  },
  THEME_ACCENTS: ["zinc", "violet", "emerald"],
  THEME_ACCENT_LABELS: {
    zinc: "settings.zinc",
    violet: "settings.violet",
    emerald: "settings.emerald",
  },
  THEME_LAYOUTS: ["standard", "compact"],
  THEME_LAYOUT_LABELS: {
    standard: "settings.standard",
    compact: "settings.compact",
  },
}));

jest.mock("@/core/constants/http.constants", () => ({
  THEME_MODE: { LIGHT: "light", DARK: "dark", SYSTEM: "system" },
}));

jest.mock("@/components/ui/Card", () => ({
  Card: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  CardContent: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  CardDescription: ({ children }: { children: React.ReactNode }) => <p>{children}</p>,
  CardFooter: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  CardHeader: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  CardTitle: ({ children }: { children: React.ReactNode }) => <h2>{children}</h2>,
}));

jest.mock("@/components/ui/Input", () => ({
  Input: (props: React.InputHTMLAttributes<HTMLInputElement>) => <input {...props} />,
}));

jest.mock("@/components/ui/Button", () => ({
  Button: ({ children, ...props }: { children: React.ReactNode } & React.ButtonHTMLAttributes<HTMLButtonElement>) => (
    <button {...props}>{children}</button>
  ),
}));

jest.mock("@/features/settings/components/CredentialsSection", () => ({
  CredentialsSection: () => <div data-testid="credentials-section" />,
}));

jest.mock("@/features/settings/components/McpServersSection", () => ({
  McpServersSection: () => <div data-testid="mcp-section" />,
}));

jest.mock("@/features/settings/components/ThemeModeSelector", () => ({
  ThemeModeSelector: () => <div data-testid="theme-mode" />,
}));

jest.mock("@/features/settings/components/AiProvidersSection", () => ({
  AiProvidersSection: () => <div data-testid="ai-providers" />,
}));

describe("SettingsForm", () => {
  beforeEach(() => {
    jest.useFakeTimers();
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve([]),
    }) as jest.Mock;
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  it("renders title and description", () => {
    render(<SettingsForm />);
    expect(screen.getByText("Settings")).toBeInTheDocument();
    expect(screen.getByText("Manage your preferences")).toBeInTheDocument();
  });

  it("renders language select", () => {
    render(<SettingsForm />);
    expect(screen.getByText("Language")).toBeInTheDocument();
    expect(screen.getByText("English")).toBeInTheDocument();
    expect(screen.getByText("French")).toBeInTheDocument();
  });

  it("renders AI persona select", () => {
    render(<SettingsForm />);
    expect(screen.getByText("AI Persona")).toBeInTheDocument();
  });

  it("renders branding section", () => {
    render(<SettingsForm />);
    expect(screen.getByText("Branding")).toBeInTheDocument();
    expect(screen.getByText("App Name")).toBeInTheDocument();
    expect(screen.getByText("Tagline")).toBeInTheDocument();
  });

  it("renders save button", () => {
    render(<SettingsForm />);
    expect(screen.getByText("Save Changes")).toBeInTheDocument();
  });

  it("renders sub-sections", () => {
    render(<SettingsForm />);
    expect(screen.getByTestId("credentials-section")).toBeInTheDocument();
    expect(screen.getByTestId("mcp-section")).toBeInTheDocument();
    expect(screen.getByTestId("theme-mode")).toBeInTheDocument();
    expect(screen.getByTestId("ai-providers")).toBeInTheDocument();
  });
});
