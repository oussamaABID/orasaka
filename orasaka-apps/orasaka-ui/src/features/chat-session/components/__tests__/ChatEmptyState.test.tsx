import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { ChatEmptyState } from "@/features/chat-session/components/ChatEmptyState";

const mockT = {
  dashboard: {
    startNewChat: "New Chat",
  },
  greeting: {
    welcome: "Welcome",
    subtext: "Hello",
  }
} as never;

// Mock WelcomeHero to simplify testing
jest.mock("@/features/chat-session/components/WelcomeHero", () => ({
  WelcomeHero: () => <div>WelcomeHero</div>
}));

describe("ChatEmptyState", () => {
  const onCreateThread = jest.fn().mockResolvedValue(undefined);
  const onChipClick = jest.fn().mockResolvedValue(undefined);

  afterEach(() => jest.clearAllMocks());

  it("renders WelcomeHero", () => {
    render(<ChatEmptyState t={mockT} onCreateThread={onCreateThread} onChipClick={onChipClick} />);
    expect(screen.getByText("WelcomeHero")).toBeInTheDocument();
  });

  it("renders new chat button", () => {
    render(<ChatEmptyState t={mockT} onCreateThread={onCreateThread} onChipClick={onChipClick} />);
    expect(screen.getByText("New Chat")).toBeInTheDocument();
  });

  it("calls onCreateThread when new chat button is clicked", () => {
    render(<ChatEmptyState t={mockT} onCreateThread={onCreateThread} onChipClick={onChipClick} />);
    fireEvent.click(screen.getByText("New Chat"));
    expect(onCreateThread).toHaveBeenCalled();
  });
});
