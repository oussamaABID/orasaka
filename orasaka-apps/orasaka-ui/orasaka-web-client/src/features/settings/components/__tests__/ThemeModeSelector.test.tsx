import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { ThemeModeSelector } from "@/features/settings/components/ThemeModeSelector";

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      settings: {
        themeMode: "Theme Mode (theme)",
        themeChangesInstant: "Changes apply instantly",
        themeSystem: "System",
        themeLight: "Light",
        themeDark: "Dark",
        themeCustom: "Custom",
        themeCyberpunk: "Cyberpunk",
        themeSolarized: "Solarized",
        themeSystemDesc: "Follows OS",
        themeLightDesc: "Bright mode",
        themeDarkDesc: "Dark mode",
        themeCustomDesc: "Custom dark",
        themeCyberpunkDesc: "Neon style",
        themeSolarizedDesc: "Warm tones",
        themeClickToApply: "Click to apply",
        themeApplied: "applied",
      },
    },
    locale: "en",
  }),
}));

jest.mock("@/features/settings/components/ThemePreviewCard", () => ({
  ThemePreviewCard: ({
    label,
    isActive,
    onClick,
  }: {
    label: string;
    isActive: boolean;
    onClick: () => void;
  }) => (
    <button onClick={onClick} data-active={isActive}>
      {label}
    </button>
  ),
}));

describe("ThemeModeSelector", () => {
  const onThemeChange = jest.fn();

  afterEach(() => jest.clearAllMocks());

  it("renders section header", () => {
    render(<ThemeModeSelector theme="system" onThemeChange={onThemeChange} />);
    expect(screen.getByText("Theme Mode")).toBeInTheDocument();
    expect(screen.getByText("Changes apply instantly")).toBeInTheDocument();
  });

  it("renders all six theme options", () => {
    render(<ThemeModeSelector theme="system" onThemeChange={onThemeChange} />);
    expect(screen.getByText("System")).toBeInTheDocument();
    expect(screen.getByText("Light")).toBeInTheDocument();
    expect(screen.getByText("Dark")).toBeInTheDocument();
    expect(screen.getByText("Custom")).toBeInTheDocument();
    expect(screen.getByText("Cyberpunk")).toBeInTheDocument();
    expect(screen.getByText("Solarized")).toBeInTheDocument();
  });

  it("marks active theme", () => {
    render(<ThemeModeSelector theme="dark" onThemeChange={onThemeChange} />);
    expect(screen.getByText("Dark")).toHaveAttribute("data-active", "true");
    expect(screen.getByText("Light")).toHaveAttribute("data-active", "false");
  });

  it("calls onThemeChange when clicked", () => {
    render(<ThemeModeSelector theme="system" onThemeChange={onThemeChange} />);
    fireEvent.click(screen.getByText("Cyberpunk"));
    expect(onThemeChange).toHaveBeenCalledWith("cyberpunk");
  });

  it("shows status feedback after selection", () => {
    jest.useFakeTimers();
    render(<ThemeModeSelector theme="system" onThemeChange={onThemeChange} />);
    fireEvent.click(screen.getByText("Dark"));
    expect(screen.getByText("applied")).toBeInTheDocument();
    jest.useRealTimers();
  });
});
