import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { ChatInputBar } from "@/features/chat-session/components/ChatInputBar";

jest.mock("@/components/ui/Button", () => ({
  Button: ({ children, ...props }: { children: React.ReactNode } & React.ButtonHTMLAttributes<HTMLButtonElement>) => (
    <button {...props}>{children}</button>
  ),
}));

jest.mock("@/features/chat-session/components/ContextPlusMenu", () => ({
  ContextPlusMenu: () => <div data-testid="context-plus-menu" />,
}));

const defaultT = {
  chat: {
    typeMessage: "Type a message...",
    typing: "Typing...",
    send: "Send",
    sending: "Sending...",
    uploadingFile: "Uploading...",
    addCapability: "Add Capability",
    cmdEnterHint: "⌘ Enter",
  },
} as never;

const baseProps = {
  input: "",
  onInputChange: jest.fn(),
  onSubmit: jest.fn(),
  isSending: false,
  isGenerating: false,
  isUploadingAttachment: false,
  selectedFeature: null,
  attachment: null,
  onClearFeature: jest.fn(),
  onClearAttachment: jest.fn(),
  isPlusMenuOpen: false,
  onTogglePlusMenu: jest.fn(),
  onClosePlusMenu: jest.fn(),
  onExecuteNode: jest.fn(),
  bootstrapFeatures: [],
  fileInputRef: { current: null },
  onFileChange: jest.fn(),
  t: defaultT,
};

describe("ChatInputBar", () => {
  afterEach(() => jest.clearAllMocks());

  it("renders textarea placeholder", () => {
    render(<ChatInputBar {...baseProps} />);
    expect(screen.getByPlaceholderText("Type a message...")).toBeInTheDocument();
  });

  it("renders send button", () => {
    render(<ChatInputBar {...baseProps} />);
    expect(screen.getByText("Send")).toBeInTheDocument();
  });

  it("submit is disabled when input is empty", () => {
    render(<ChatInputBar {...baseProps} />);
    const submitBtn = screen.getByText("Send").closest("button")!;
    expect(submitBtn).toBeDisabled();
  });

  it("submit is enabled when input has text", () => {
    render(<ChatInputBar {...baseProps} input="hello" />);
    const submitBtn = screen.getByText("Send").closest("button")!;
    expect(submitBtn).not.toBeDisabled();
  });

  it("calls onInputChange on textarea change", () => {
    render(<ChatInputBar {...baseProps} />);
    const textarea = screen.getByPlaceholderText("Type a message...");
    fireEvent.change(textarea, { target: { value: "test" } });
    expect(baseProps.onInputChange).toHaveBeenCalledWith("test");
  });

  it("shows typing text when generating", () => {
    render(<ChatInputBar {...baseProps} isGenerating={true} />);
    expect(screen.getByPlaceholderText("Typing...")).toBeInTheDocument();
  });

  it("shows sending text when sending", () => {
    render(<ChatInputBar {...baseProps} isSending={true} />);
    expect(screen.getByText("Sending...")).toBeInTheDocument();
  });

  it("shows feature indicator when selected", () => {
    render(
      <ChatInputBar
        {...baseProps}
        selectedFeature={{ key: "chat.text", label: "Text Generation", type: "text" } as never}
      />,
    );
    expect(screen.getByText(/Text Generation/)).toBeInTheDocument();
  });

  it("shows attachment indicator", () => {
    render(
      <ChatInputBar
        {...baseProps}
        attachment={{ assetId: "a1", name: "doc.pdf" }}
      />,
    );
    expect(screen.getByText(/doc.pdf/)).toBeInTheDocument();
  });

  it("shows uploading indicator", () => {
    render(<ChatInputBar {...baseProps} isUploadingAttachment={true} />);
    expect(screen.getByText("Uploading...")).toBeInTheDocument();
  });

  it("renders ContextPlusMenu", () => {
    render(<ChatInputBar {...baseProps} />);
    expect(screen.getByTestId("context-plus-menu")).toBeInTheDocument();
  });

  it("renders attach file button", () => {
    render(<ChatInputBar {...baseProps} />);
    expect(screen.getByLabelText("Attach File")).toBeInTheDocument();
  });

  it("renders keyboard hint", () => {
    render(<ChatInputBar {...baseProps} />);
    expect(screen.getByText("⌘ Enter")).toBeInTheDocument();
  });
});
