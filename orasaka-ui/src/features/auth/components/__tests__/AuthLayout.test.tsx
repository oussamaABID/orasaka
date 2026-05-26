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
      },
    },
    locale: "en",
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

  it("renders copyright", () => {
    render(<AuthLayout><div>form</div></AuthLayout>);
    expect(screen.getByText(/Orasaka · MIT License/)).toBeInTheDocument();
  });
});
