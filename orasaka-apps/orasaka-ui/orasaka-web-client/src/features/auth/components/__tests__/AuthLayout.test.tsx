import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import { AuthLayout } from "@/features/auth/components/AuthLayout";

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      auth: {
        heroHeadline: "Welcome to Orasaka",
        heroTagline: "AI orchestration platform",
        heroFeature1: "Enterprise Security",
        heroFeature2: "Multi-Model",
        heroFeature3: "Plugin Ecosystem",
        legalPrivacy: "Privacy Policy",
        legalTerms: "Terms of Service",
        legalContact: "Contact",
        langSwitchLabel: "Language",
      },
    },
    locale: "en",
    setLocale: jest.fn(),
  }),
}));

describe("AuthLayout", () => {
  it("renders brand name", () => {
    render(<AuthLayout><div>form</div></AuthLayout>);
    expect(screen.getByText("Orasaka")).toBeInTheDocument();
  });

  it("renders hero headline", () => {
    render(<AuthLayout><div>form</div></AuthLayout>);
    expect(screen.getByText("Welcome to Orasaka")).toBeInTheDocument();
  });

  it("renders hero tagline", () => {
    render(<AuthLayout><div>form</div></AuthLayout>);
    expect(screen.getByText("AI orchestration platform")).toBeInTheDocument();
  });

  it("renders feature bullets", () => {
    render(<AuthLayout><div>form</div></AuthLayout>);
    expect(screen.getByText("Enterprise Security")).toBeInTheDocument();
    expect(screen.getByText("Multi-Model")).toBeInTheDocument();
    expect(screen.getByText("Plugin Ecosystem")).toBeInTheDocument();
  });

  it("renders children in form panel", () => {
    render(<AuthLayout><div>My Form Content</div></AuthLayout>);
    expect(screen.getByText("My Form Content")).toBeInTheDocument();
  });

  it("renders copyright and link", () => {
    render(<AuthLayout><div>form</div></AuthLayout>);
    expect(screen.getByText(/Orasaka · MIT License - by/)).toBeInTheDocument();
    const link = screen.getByRole("link", { name: "krizaka" });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute("href", "https://www.krizaka.com/");
  });

  it("renders language switcher", () => {
    render(<AuthLayout><div>form</div></AuthLayout>);
    expect(screen.getByText("EN")).toBeInTheDocument();
    expect(screen.getByText("FR")).toBeInTheDocument();
  });

  it("renders legal links", () => {
    render(<AuthLayout><div>form</div></AuthLayout>);
    expect(screen.getByText("Privacy Policy")).toBeInTheDocument();
    expect(screen.getByText("Terms of Service")).toBeInTheDocument();
    expect(screen.getByText("Contact")).toBeInTheDocument();
  });
});
