import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import { ProfileView } from "@/features/profile/components/ProfileView";

const mockPush = jest.fn();

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush }),
}));

jest.mock("@/core/context/TenantContext", () => ({
  useTenant: () => ({
    accentClasses: {
      accentGradient: "from-zinc-500 to-zinc-600",
      bg: "bg-zinc-600",
    },
  }),
}));

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      profile: {
        failedLoad: "Failed to load profile",
        unauthError: "Not authenticated",
        enterpriseTier: "Enterprise",
        premiumTier: "Premium",
        freeTier: "Free",
        accountDetails: "Account Details",
        detailsDesc: "Your account information",
        userId: "User ID",
        username: "Username",
        email: "Email",
        assignedAuth: "Authorities",
        noAuth: "None",
        prefMetadata: "Preferences",
        prefDesc: "Raw preferences JSON",
        mcpTitle: "MCP",
        mcpDesc: "Servers",
      },
      header: { settings: "Settings" },
    },
    locale: "en",
  }),
}));

jest.mock("@/features/profile/hooks/useProfile", () => ({
  useProfile: () => ({
    profile: {
      id: "user-123",
      username: "testadmin",
      email: "admin@orasaka.io",
      authorities: ["ROLE_ADMIN"],
      preferences: { theme: "dark", aiPersona: "standard" },
    },
    isLoading: false,
    error: null,
  }),
}));

jest.mock("@/features/profile/components/ProfileViewParts", () => ({
  CopyableField: ({ label, value }: { label: string; value: string }) => (
    <div><span>{label}</span><span>{value}</span></div>
  ),
  useMcpServers: () => ({
    servers: [],
    isLoadingServers: false,
    isSavingServer: false,
    serverMessage: null,
    handleSaveServer: jest.fn(),
    handleDeleteServer: jest.fn(),
    serverLabel: "",
    setServerLabel: jest.fn(),
    serverUrl: "",
    setServerUrl: jest.fn(),
    serverAuthToken: "",
    setServerAuthToken: jest.fn(),
  }),
}));

jest.mock("@/features/profile/components/ProfileMcpSection", () => ({
  ProfileMcpSection: () => <div data-testid="mcp-section" />,
}));

jest.mock("@/components/ui/Card", () => ({
  Card: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  CardContent: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  CardDescription: ({ children }: { children: React.ReactNode }) => <p>{children}</p>,
  CardHeader: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  CardTitle: ({ children }: { children: React.ReactNode }) => <h3>{children}</h3>,
}));

describe("ProfileView", () => {
  it("renders username", () => {
    render(<ProfileView />);
    const els = screen.getAllByText("testadmin");
    expect(els.length).toBeGreaterThanOrEqual(1);
  });

  it("renders email", () => {
    render(<ProfileView />);
    const els = screen.getAllByText("admin@orasaka.io");
    expect(els.length).toBeGreaterThanOrEqual(1);
  });

  it("renders enterprise tier for admin", () => {
    render(<ProfileView />);
    expect(screen.getByText("Enterprise")).toBeInTheDocument();
  });

  it("renders initials avatar", () => {
    render(<ProfileView />);
    expect(screen.getByText("TE")).toBeInTheDocument();
  });

  it("renders account details card", () => {
    render(<ProfileView />);
    expect(screen.getByText("Account Details")).toBeInTheDocument();
  });

  it("renders preferences JSON", () => {
    render(<ProfileView />);
    expect(screen.getByText(/aiPersona/)).toBeInTheDocument();
  });

  it("renders settings button", () => {
    render(<ProfileView />);
    expect(screen.getByLabelText("Settings")).toBeInTheDocument();
  });

  it("renders ROLE_ADMIN authority", () => {
    render(<ProfileView />);
    const auths = screen.getAllByText("ROLE_ADMIN");
    expect(auths.length).toBeGreaterThanOrEqual(1);
  });
});
