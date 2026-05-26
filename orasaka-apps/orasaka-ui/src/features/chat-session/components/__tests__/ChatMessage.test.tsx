import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import { ChatMessage } from "@/features/chat-session/components/ChatMessage";

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      chat: { user: "You", ai: "AI" },
    },
    locale: "en",
  }),
}));

jest.mock("react-markdown", () => ({
  __esModule: true,
  default: ({ children }: { children: string }) => <p>{children}</p>,
}));

const baseMsg = {
  id: "m1",
  role: "user" as const,
  content: "Hello world",
  kind: "text" as const,
  timestamp: new Date("2026-01-01T14:30:00Z"),
};

describe("ChatMessage", () => {
  it("renders text content", () => {
    render(<ChatMessage message={baseMsg} />);
    expect(screen.getByText("Hello world")).toBeInTheDocument();
  });

  it("renders user avatar with first letter", () => {
    render(<ChatMessage message={baseMsg} />);
    expect(screen.getByText("Y")).toBeInTheDocument();
  });

  it("renders AI avatar for assistant messages", () => {
    render(<ChatMessage message={{ ...baseMsg, role: "assistant" }} />);
    expect(screen.getByText("AI")).toBeInTheDocument();
  });

  it("renders timestamp", () => {
    render(<ChatMessage message={baseMsg} />);
    // format(timestamp, "HH:mm") should produce the time
    expect(screen.getByText(/\d{2}:\d{2}/)).toBeInTheDocument();
  });

  it("renders image content for image kind", () => {
    render(
      <ChatMessage message={{ ...baseMsg, kind: "image", content: "data:image/png;base64,abc" }} />,
    );
    const img = screen.getByAltText("Generated Asset");
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute("src", "data:image/png;base64,abc");
  });

  it("renders audio element for audio kind", () => {
    const { container } = render(
      <ChatMessage message={{ ...baseMsg, kind: "audio", content: "data:audio/mp3;base64,abc" }} />,
    );
    const audio = container.querySelector("audio");
    expect(audio).toBeInTheDocument();
    expect(audio).toHaveAttribute("src", "data:audio/mp3;base64,abc");
  });
});
