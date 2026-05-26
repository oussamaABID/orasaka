import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { ChatEmptyState } from "@/features/chat-session/components/ChatEmptyState";

const mockT = {
  chat: {
    startConversation: "Start a Conversation",
    startConversationDesc: "Type a message to get started",
    suggestionImage: "Generate an image",
    suggestionCode: "Write code",
    suggestionAsk: "Ask a question",
  },
  dashboard: {
    startNewChat: "New Chat",
  },
} as never;

describe("ChatEmptyState", () => {
  const onCreateThread = jest.fn().mockResolvedValue(undefined);
  const onChipClick = jest.fn().mockResolvedValue(undefined);

  afterEach(() => jest.clearAllMocks());

  it("renders heading and description", () => {
    render(<ChatEmptyState t={mockT} onCreateThread={onCreateThread} onChipClick={onChipClick} />);
    expect(screen.getByText("Start a Conversation")).toBeInTheDocument();
    expect(screen.getByText("Type a message to get started")).toBeInTheDocument();
  });

  it("renders suggestion chips", () => {
    render(<ChatEmptyState t={mockT} onCreateThread={onCreateThread} onChipClick={onChipClick} />);
    expect(screen.getByText("Generate an image")).toBeInTheDocument();
    expect(screen.getByText("Write code")).toBeInTheDocument();
    expect(screen.getByText("Ask a question")).toBeInTheDocument();
  });

  it("calls onChipClick when a chip is clicked", () => {
    render(<ChatEmptyState t={mockT} onCreateThread={onCreateThread} onChipClick={onChipClick} />);
    fireEvent.click(screen.getByText("Write code"));
    expect(onChipClick).toHaveBeenCalledWith("Write code");
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

  it("renders all chip icons", () => {
    render(<ChatEmptyState t={mockT} onCreateThread={onCreateThread} onChipClick={onChipClick} />);
    expect(screen.getByText("🎨")).toBeInTheDocument();
    expect(screen.getByText("⚡")).toBeInTheDocument();
    expect(screen.getByText("💬")).toBeInTheDocument();
  });
});
