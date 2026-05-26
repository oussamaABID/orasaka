import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { RagSearchCard } from "@/features/playground/components/RagSearchCard";

const mockSearch = jest.fn();
const mockSetRagQuery = jest.fn();
let mockRagQuery = "";
let mockResult: string | null = null;
let mockIsPending = false;
let mockError: string | null = null;

jest.mock("@/features/playground/hooks/useRagSearch", () => ({
  useRagSearch: () => ({
    search: mockSearch,
    result: mockResult,
    isPending: mockIsPending,
    error: mockError,
  }),
}));

jest.mock("@/core/context/JobStreamContext", () => ({
  useJobStream: () => ({
    ragQuery: mockRagQuery,
    setRagQuery: mockSetRagQuery,
  }),
}));

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      playground: {
        passiveRagTitle: "RAG Search",
        passiveRagPlaceholder: "Enter query...",
        search: "Search",
        examples: "Examples",
        retrievedContext: "Retrieved Context",
      },
    },
    locale: "en",
  }),
}));

describe("RagSearchCard", () => {
  beforeEach(() => {
    mockRagQuery = "";
    mockResult = null;
    mockIsPending = false;
    mockError = null;
    jest.clearAllMocks();
  });

  it("renders title", () => {
    render(<RagSearchCard />);
    expect(screen.getByText("RAG Search")).toBeInTheDocument();
  });

  it("renders search input", () => {
    render(<RagSearchCard />);
    expect(screen.getByPlaceholderText("Enter query...")).toBeInTheDocument();
  });

  it("renders example buttons", () => {
    render(<RagSearchCard />);
    expect(screen.getByText("Virtual thread concurrency")).toBeInTheDocument();
    expect(screen.getByText("OAuth2 token validation")).toBeInTheDocument();
    expect(screen.getByText("BFF proxy routes")).toBeInTheDocument();
    expect(screen.getByText("Tool callback registry")).toBeInTheDocument();
  });

  it("disables search button when query is empty", () => {
    render(<RagSearchCard />);
    expect(screen.getByText("Search").closest("button")).toBeDisabled();
  });

  it("calls setRagQuery on input change", () => {
    render(<RagSearchCard />);
    fireEvent.change(screen.getByPlaceholderText("Enter query..."), {
      target: { value: "test query" },
    });
    expect(mockSetRagQuery).toHaveBeenCalledWith("test query");
  });

  it("calls search on button click when query present", () => {
    mockRagQuery = "virtual threads";
    render(<RagSearchCard />);
    fireEvent.click(screen.getByText("Search"));
    expect(mockSearch).toHaveBeenCalledWith("virtual threads");
  });

  it("calls search on Enter key", () => {
    mockRagQuery = "test";
    render(<RagSearchCard />);
    fireEvent.keyDown(screen.getByPlaceholderText("Enter query..."), { key: "Enter" });
    expect(mockSearch).toHaveBeenCalledWith("test");
  });

  it("calls search and setRagQuery on example click", () => {
    render(<RagSearchCard />);
    fireEvent.click(screen.getByText("BFF proxy routes"));
    expect(mockSetRagQuery).toHaveBeenCalledWith("BFF proxy routes");
    expect(mockSearch).toHaveBeenCalledWith("BFF proxy routes");
  });

  it("shows error message", () => {
    mockError = "Connection failed";
    render(<RagSearchCard />);
    expect(screen.getByText("Connection failed")).toBeInTheDocument();
  });

  it("shows result when available", () => {
    mockResult = "Retrieved context data here";
    render(<RagSearchCard />);
    expect(screen.getByText("Retrieved Context")).toBeInTheDocument();
    expect(screen.getByText("Retrieved context data here")).toBeInTheDocument();
  });

  it("hides result when null", () => {
    render(<RagSearchCard />);
    expect(screen.queryByText("Retrieved Context")).not.toBeInTheDocument();
  });
});
