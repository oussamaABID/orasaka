import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import LoginPage from "@/app/login/page";

const mockPush = jest.fn();
const mockLoginWithGithub = jest.fn();
const mockLoginWithGoogle = jest.fn();

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush }),
}));

jest.mock("next/link", () => {
  return ({ children, ...props }: { children: React.ReactNode; href: string }) => (
    <a {...props}>{children}</a>
  );
});

jest.mock("next-auth/react", () => ({
  signIn: jest.fn(),
}));

jest.mock("@/features/auth/hooks/useAuth", () => ({
  useAuth: () => ({
    isAuthenticated: false,
    loginWithGithub: mockLoginWithGithub,
    loginWithGoogle: mockLoginWithGoogle,
  }),
}));

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      auth: {
        loginTitle: "Welcome Back",
        loginSubtitle: "Sign in to your account",
        emailLabel: "Email",
        passwordLabel: "Password",
        signInBtn: "Sign In",
        signingInBtn: "Signing In...",
        orContinueWith: "or continue with",
        noAccount: "No account?",
        registerLink: "Register",
        forgotPasswordLink: "Forgot password?",
        invalidCredentials: "Invalid credentials",
        unexpectedError: "Unexpected error",
      },
    },
    locale: "en",
  }),
}));

jest.mock("@/features/auth/components/AuthLayout", () => ({
  AuthLayout: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

jest.mock("@/components/ui/Card", () => ({
  Card: ({ children, ...props }: { children: React.ReactNode }) => <div {...props}>{children}</div>,
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

describe("LoginPage", () => {
  afterEach(() => jest.clearAllMocks());

  it("renders title and subtitle", () => {
    render(<LoginPage />);
    expect(screen.getByText("Welcome Back")).toBeInTheDocument();
    expect(screen.getByText("Sign in to your account")).toBeInTheDocument();
  });

  it("renders email and password fields", () => {
    render(<LoginPage />);
    expect(screen.getByLabelText("Email")).toBeInTheDocument();
    expect(screen.getByLabelText("Password")).toBeInTheDocument();
  });

  it("renders sign in button", () => {
    render(<LoginPage />);
    expect(screen.getByText("Sign In")).toBeInTheDocument();
  });

  it("renders OAuth buttons", () => {
    render(<LoginPage />);
    expect(screen.getByText("GitHub")).toBeInTheDocument();
    expect(screen.getByText("Google")).toBeInTheDocument();
  });

  it("renders register link", () => {
    render(<LoginPage />);
    expect(screen.getByText("Register")).toBeInTheDocument();
    expect(screen.getByText("Register").closest("a")).toHaveAttribute("href", "/register");
  });

  it("renders forgot password link", () => {
    render(<LoginPage />);
    expect(screen.getByText("Forgot password?")).toBeInTheDocument();
  });

  it("renders divider text", () => {
    render(<LoginPage />);
    expect(screen.getByText("or continue with")).toBeInTheDocument();
  });

  it("calls loginWithGithub", () => {
    render(<LoginPage />);
    fireEvent.click(screen.getByText("GitHub"));
    expect(mockLoginWithGithub).toHaveBeenCalled();
  });

  it("calls loginWithGoogle", () => {
    render(<LoginPage />);
    fireEvent.click(screen.getByText("Google"));
    expect(mockLoginWithGoogle).toHaveBeenCalled();
  });

  it("allows email input", () => {
    render(<LoginPage />);
    const emailInput = screen.getByLabelText("Email") as HTMLInputElement;
    fireEvent.change(emailInput, { target: { value: "test@orasaka.com" } });
    expect(emailInput.value).toBe("test@orasaka.com");
  });

  it("allows password input", () => {
    render(<LoginPage />);
    const passInput = screen.getByLabelText("Password") as HTMLInputElement;
    fireEvent.change(passInput, { target: { value: "secret123" } });
    expect(passInput.value).toBe("secret123");
  });
});
