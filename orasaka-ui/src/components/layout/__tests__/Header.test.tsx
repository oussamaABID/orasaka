import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { Header } from "@/components/layout/Header";

const mockLogout = jest.fn();
const mockOpen = jest.fn();
const mockSetLocale = jest.fn();

jest.mock("next/link", () => {
  return ({ children, ...props }: { children: React.ReactNode; href: string }) => (
    <a {...props}>{children}</a>
  );
});

jest.mock("@/features/auth/hooks/useAuth", () => ({
  useAuth: () => ({
    user: { name: "Oussama", email: "admin@orasaka.io" },
    isAuthenticated: true,
    logout: mockLogout,
  }),
}));

jest.mock("@/core/context/SidebarContext", () => ({
  useSidebar: () => ({ open: mockOpen }),
}));

jest.mock("@/features/tenant/context/TenantContext", () => ({
  useTenant: () => ({
    accentClasses: { text: "text-amber-500", bgSoft: "bg-amber-500/10", bg: "bg-amber-500" },
  }),
}));

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    locale: "en",
    setLocale: mockSetLocale,
    t: {
      settings: { english: "English", french: "French" },
      header: { profile: "Profile", settings: "Settings", logout: "Logout" },
      notifications: {
        title: "Notifications",
        active: "active",
        noTasks: "No tasks",
        viewAll: "View All",
        videoGen: "Video",
        imageGen: "Image",
        speechGen: "Speech",
        textGen: "Text",
      },
    },
  }),
}));

jest.mock("@/components/ui/ThemeToggle", () => ({
  ThemeToggle: () => <button data-testid="theme-toggle">Theme</button>,
}));

jest.mock("@/components/layout/NotificationBell", () => ({
  NotificationBell: ({ onToggle }: { bellOpen: boolean; onToggle: (v: boolean) => void }) => (
    <button data-testid="notif-bell" onClick={() => onToggle(true)}>Bell</button>
  ),
}));

describe("Header", () => {
  afterEach(() => jest.clearAllMocks());

  it("renders user name", () => {
    render(<Header />);
    expect(screen.getByText("Oussama")).toBeInTheDocument();
  });

  it("renders user initial avatar", () => {
    render(<Header />);
    expect(screen.getByText("O")).toBeInTheDocument();
  });

  it("renders language switcher with locale", () => {
    render(<Header />);
    expect(screen.getByText("en")).toBeInTheDocument();
  });

  it("renders theme toggle", () => {
    render(<Header />);
    expect(screen.getByTestId("theme-toggle")).toBeInTheDocument();
  });

  it("renders notification bell", () => {
    render(<Header />);
    expect(screen.getByTestId("notif-bell")).toBeInTheDocument();
  });

  it("opens language dropdown", () => {
    render(<Header />);
    fireEvent.click(screen.getByLabelText("Change Language"));
    expect(screen.getByText("English")).toBeInTheDocument();
    expect(screen.getByText("French")).toBeInTheDocument();
  });

  it("switches to French locale", () => {
    render(<Header />);
    fireEvent.click(screen.getByLabelText("Change Language"));
    fireEvent.click(screen.getByText("French"));
    expect(mockSetLocale).toHaveBeenCalledWith("fr");
  });

  it("opens user dropdown on avatar click", () => {
    render(<Header />);
    fireEvent.click(screen.getByText("O")); // avatar initial
    expect(screen.getByText("Profile")).toBeInTheDocument();
    expect(screen.getByText("Settings")).toBeInTheDocument();
    expect(screen.getByText("Logout")).toBeInTheDocument();
  });

  it("calls logout", () => {
    render(<Header />);
    fireEvent.click(screen.getByText("O")); // open dropdown
    fireEvent.click(screen.getByText("Logout"));
    expect(mockLogout).toHaveBeenCalled();
  });

  it("opens sidebar on menu button", () => {
    render(<Header />);
    fireEvent.click(screen.getByLabelText("Open Sidebar"));
    expect(mockOpen).toHaveBeenCalled();
  });

  it("displays user email in dropdown", () => {
    render(<Header />);
    fireEvent.click(screen.getByText("O"));
    expect(screen.getByText("admin@orasaka.io")).toBeInTheDocument();
  });
});
