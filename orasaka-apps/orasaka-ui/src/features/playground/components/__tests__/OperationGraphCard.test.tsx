import "@testing-library/jest-dom";
import { render, screen, fireEvent, act } from "@testing-library/react";
import { OperationGraphCard } from "@/features/playground/components/OperationGraphCard";

const mockInvalidate = jest.fn().mockResolvedValue(undefined);
let mockNodes: unknown[] = [];
let mockIsLoading = false;

jest.mock("@/features/playground/hooks/useOperationGraph", () => ({
  useOperationGraph: () => ({
    nodes: mockNodes,
    isLoading: mockIsLoading,
    invalidate: mockInvalidate,
  }),
}));

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      operationGraph: {
        title: "Operation Graph",
        subtitle: "SDUI capability matrix",
        noNodes: "No nodes available",
        stateActive: "Active",
        stateLocked: "Locked",
        stateInvisible: "Invisible",
        nodeCount: (n: number) => `${n} nodes`,
        colFeature: "Feature",
        colLabel: "Label",
        colState: "State",
        colEndpoint: "Endpoint",
      },
    },
    locale: "en",
  }),
}));

describe("OperationGraphCard", () => {
  beforeEach(() => {
    mockNodes = [];
    mockIsLoading = false;
    jest.clearAllMocks();
  });

  it("renders loading spinner", () => {
    mockIsLoading = true;
    const { container } = render(<OperationGraphCard />);
    expect(container.querySelector(".animate-spin")).toBeInTheDocument();
  });

  it("renders empty state", () => {
    render(<OperationGraphCard />);
    expect(screen.getByText("No nodes available")).toBeInTheDocument();
  });

  it("renders title and subtitle", () => {
    render(<OperationGraphCard />);
    expect(screen.getByText("Operation Graph")).toBeInTheDocument();
    expect(screen.getByText("SDUI capability matrix")).toBeInTheDocument();
  });

  it("renders node table when nodes present", () => {
    mockNodes = [
      {
        id: "chat.text",
        label: "Text Chat",
        state: { type: "ACTIVE" },
        executionDetails: { httpMethod: "POST", uriPath: "/api/chat" },
      },
    ];
    render(<OperationGraphCard />);
    expect(screen.getByText("chat.text")).toBeInTheDocument();
    expect(screen.getByText("Text Chat")).toBeInTheDocument();
    expect(screen.getByText("Active")).toBeInTheDocument();
  });

  it("renders locked state badge", () => {
    mockNodes = [
      {
        id: "media.video",
        label: "Video Gen",
        state: { type: "LOCKED" },
        executionDetails: { httpMethod: "POST", uriPath: "/api/video" },
      },
    ];
    render(<OperationGraphCard />);
    expect(screen.getByText("Locked")).toBeInTheDocument();
  });

  it("renders invisible state badge", () => {
    mockNodes = [
      {
        id: "hidden.feature",
        label: "Hidden",
        state: { type: "INVISIBLE" },
        executionDetails: { httpMethod: "GET", uriPath: "/api/hidden" },
      },
    ];
    render(<OperationGraphCard />);
    expect(screen.getByText("Invisible")).toBeInTheDocument();
  });

  it("renders node count", () => {
    mockNodes = [
      {
        id: "a",
        label: "A",
        state: { type: "ACTIVE" },
        executionDetails: { httpMethod: "GET", uriPath: "/" },
      },
      {
        id: "b",
        label: "B",
        state: { type: "ACTIVE" },
        executionDetails: { httpMethod: "GET", uriPath: "/" },
      },
    ];
    render(<OperationGraphCard />);
    expect(screen.getByText("2 nodes")).toBeInTheDocument();
  });

  it("renders table headers", () => {
    mockNodes = [
      {
        id: "a",
        label: "A",
        state: { type: "ACTIVE" },
        executionDetails: { httpMethod: "GET", uriPath: "/" },
      },
    ];
    render(<OperationGraphCard />);
    expect(screen.getByText("Feature")).toBeInTheDocument();
    expect(screen.getByText("Label")).toBeInTheDocument();
    expect(screen.getByText("State")).toBeInTheDocument();
    expect(screen.getByText("Endpoint")).toBeInTheDocument();
  });

  it("calls invalidate on refresh click", async () => {
    await act(async () => {
      render(<OperationGraphCard />);
    });
    const btn = screen.getByRole("button");
    await act(async () => {
      fireEvent.click(btn);
    });
    expect(mockInvalidate).toHaveBeenCalled();
  });

  it("renders endpoint info", () => {
    mockNodes = [
      {
        id: "chat.text",
        label: "Chat",
        state: { type: "ACTIVE" },
        executionDetails: { httpMethod: "POST", uriPath: "/api/v1/chat" },
      },
    ];
    render(<OperationGraphCard />);
    expect(screen.getByText(/POST/)).toBeInTheDocument();
    expect(screen.getByText(/\/api\/v1\/chat/)).toBeInTheDocument();
  });
});
