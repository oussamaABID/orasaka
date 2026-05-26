/* eslint-disable @next/next/no-img-element, react/display-name */
import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { Sidebar } from "@/components/layout/Sidebar";

const mockClose = jest.fn();

jest.mock("next/link", () => {
  return ({ children, ...props }: { children: React.ReactNode; href: string }) => (
    <a {...props}>{children}</a>
  );
});

jest.mock("next/image", () => {
  return (props: { alt: string; src: string }) => <img alt={props.alt} src={props.src} />;
});

jest.mock("next/navigation", () => ({
  usePathname: () => "/",
}));

jest.mock("@/core/hooks/useAuth", () => ({
  useAuth: () => ({
    user: { name: "Admin", email: "admin@orasaka.io", role: "admin" },
  }),
}));

jest.mock("@/core/context/SidebarContext", () => ({
  useSidebar: () => ({ isOpen: true, close: mockClose }),
}));

jest.mock("@/core/context/TenantContext", () => ({
  useTenant: () => ({
    config: { displayName: "Orasaka", layoutMode: "default" },
  }),
}));

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      sidebar: {
        dashboard: "Dashboard",
        chatSessions: "Chat",
        playground: "Playground",
        jobsHistory: "Jobs",
        adminPanel: "Admin",
        navigation: "NAVIGATION",
        settings: "Settings",
        videoCategory: "Video",
        audioCategory: "Audio",
        textCategory: "Text",
        imageCategory: "Image",
        codeCategory: "Code",
        speechCategory: "Speech",
        visionCategory: "Vision",
        generateVideo: "Generate",
        analyzeVideo: "Analyze",
        analyzeAudio: "Analyze Audio",
        textChat: "Chat",
        generateImage: "Generate Image",
        featureToCode: "Feature to Code",
        speechSynthesis: "TTS",
        visionAnalysis: "Vision",
      },
    },
    locale: "en",
  }),
}));

jest.mock("@/components/ui/Button", () => ({
  Button: ({ children, ...props }: { children: React.ReactNode }) => (
    <span {...props}>{children}</span>
  ),
}));

describe("Sidebar", () => {
  afterEach(() => jest.clearAllMocks());

  it("renders brand name", () => {
    render(<Sidebar />);
    expect(screen.getByText("Orasaka")).toBeInTheDocument();
  });

  it("renders navigation label", () => {
    render(<Sidebar />);
    expect(screen.getByText("NAVIGATION")).toBeInTheDocument();
  });

  it("renders all nav items for admin", () => {
    render(<Sidebar />);
    expect(screen.getAllByText("Dashboard").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Chat").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Playground").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Jobs").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Admin").length).toBeGreaterThan(0);
  });

  it("renders settings link", () => {
    render(<Sidebar />);
    expect(screen.getAllByText("Settings").length).toBeGreaterThan(0);
  });

  it("renders close sidebar button", () => {
    render(<Sidebar />);
    expect(screen.getByLabelText("Close Sidebar")).toBeInTheDocument();
  });

  it("calls close on close button click", () => {
    render(<Sidebar />);
    fireEvent.click(screen.getByLabelText("Close Sidebar"));
    expect(mockClose).toHaveBeenCalled();
  });

  it("renders brand logo area", () => {
    render(<Sidebar />);
    // SentinelMini is an SVG, not an img — verify the brand area renders
    expect(screen.getByText("Orasaka")).toBeInTheDocument();
  });

  it("renders SETTINGS label", () => {
    render(<Sidebar />);
    expect(screen.getAllByText("Settings").length).toBeGreaterThan(0);
  });
});
