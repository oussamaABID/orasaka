import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { ChatDrawer } from "@/features/chat-session/components/ChatDrawer";

jest.mock("@/features/chat-session/components/ThreadList", () => ({
  ThreadList: () => <div data-testid="thread-list">threads</div>,
}));

const baseProps = {
  isOpen: true,
  onClose: jest.fn(),
  threads: [],
  activeConversationId: "c1",
  onSelectThread: jest.fn(),
  isLoadingThreads: false,
  onCreateThread: jest.fn(),
  onDeleteThread: jest.fn(),
  t: { chat: { memoryBlocks: "Memory Blocks" } },
};

describe("ChatDrawer", () => {
  afterEach(() => jest.clearAllMocks());

  it("renders nothing when closed", () => {
    const { container } = render(<ChatDrawer {...baseProps} isOpen={false} />);
    expect(container.innerHTML).toBe("");
  });

  it("renders title when open", () => {
    render(<ChatDrawer {...baseProps} />);
    expect(screen.getByText("Memory Blocks")).toBeInTheDocument();
  });

  it("renders thread list", () => {
    render(<ChatDrawer {...baseProps} />);
    expect(screen.getByTestId("thread-list")).toBeInTheDocument();
  });

  it("calls onClose when backdrop clicked", () => {
    render(<ChatDrawer {...baseProps} />);
    fireEvent.click(screen.getByLabelText("Close drawer"));
    expect(baseProps.onClose).toHaveBeenCalled();
  });
});
