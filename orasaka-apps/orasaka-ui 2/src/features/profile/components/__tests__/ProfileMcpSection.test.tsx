import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import { ProfileMcpSection } from "@/features/profile/components/ProfileMcpSection";

jest.mock("@/components/ui/Card", () => ({
  Card: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  CardContent: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  CardDescription: ({ children }: { children: React.ReactNode }) => <p>{children}</p>,
  CardHeader: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  CardTitle: ({ children }: { children: React.ReactNode }) => <h3>{children}</h3>,
}));

jest.mock("@/components/ui/Button", () => ({
  Button: ({ children, ...props }: { children: React.ReactNode } & React.ButtonHTMLAttributes<HTMLButtonElement>) => (
    <button {...props}>{children}</button>
  ),
}));

jest.mock("@/components/ui/Input", () => ({
  Input: (props: React.InputHTMLAttributes<HTMLInputElement>) => <input {...props} />,
}));

const t = {
  settings: {
    mcpServersTitle: "MCP Servers",
    mcpServersDesc: "Connect external tool servers",
    mcpLabel: "Server Name",
    mcpUrl: "Server URL",
    mcpAuthToken: "Auth Token",
    mcpRegister: "Register",
    mcpDelete: "Delete",
    mcpNoServers: "No servers",
    mcpLoading: "Loading...",
    mcpLabelPlaceholder: "Name",
    mcpUrlPlaceholder: "https://",
    mcpAuthTokenPlaceholder: "Token",
    saving: "Saving...",
  },
} as never;

const baseProps = {
  servers: [],
  isLoadingServers: false,
  isSavingServer: false,
  serverMessage: null,
  onSaveServer: jest.fn(),
  onDeleteServer: jest.fn(),
  serverLabel: "",
  setServerLabel: jest.fn(),
  serverUrl: "",
  setServerUrl: jest.fn(),
  serverAuthToken: "",
  setServerAuthToken: jest.fn(),
  accentGradient: "from-zinc-500 to-zinc-600",
  t,
};

describe("ProfileMcpSection", () => {
  it("renders title", () => {
    render(<ProfileMcpSection {...baseProps} />);
    expect(screen.getByText("MCP Servers")).toBeInTheDocument();
  });

  it("renders register button", () => {
    render(<ProfileMcpSection {...baseProps} />);
    const els = screen.getAllByText("Register");
    expect(els.length).toBeGreaterThanOrEqual(1);
  });

  it("shows no servers message when empty", () => {
    render(<ProfileMcpSection {...baseProps} />);
    expect(screen.getByText("No servers")).toBeInTheDocument();
  });

  it("renders server list when servers exist", () => {
    const servers = [
      { id: 1, label: "Test Server", transportType: "REMOTE", url: "https://test.com", enabled: true },
    ] as never[];
    render(<ProfileMcpSection {...baseProps} servers={servers} />);
    expect(screen.getByText("Test Server")).toBeInTheDocument();
  });
});
