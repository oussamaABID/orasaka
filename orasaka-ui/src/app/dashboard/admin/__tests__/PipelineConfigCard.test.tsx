import "@testing-library/jest-dom";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { PipelineConfigCard } from "@/app/dashboard/admin/PipelineConfigCard";

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      admin: {
        pipeline: {
          title: "Interceptor Pipeline",
          subtitle: "Configure execution order",
          resetToDefault: "Reset",
          saveOrder: "Save Order",
          saving: "Saving...",
          dragHint: "Drag to reorder",
          errorLoading: "Failed to load",
          errorSaving: "Failed to save",
          savedSuccess: "Saved",
          resetConfirm: "Reset to defaults?",
          disabled: "Disabled",
          enabledLabel: "Toggle",
        },
      },
    },
    locale: "en",
  }),
}));

const mockInterceptors = [
  { interceptorKey: "rag", displayLabel: "RAG Interceptor", executionOrder: 1, enabled: true, description: "Vector search" },
  { interceptorKey: "memory", displayLabel: "Memory Interceptor", executionOrder: 2, enabled: true, description: "Conversation history" },
  { interceptorKey: "router", displayLabel: "Router Interceptor", executionOrder: 3, enabled: false, description: "" },
];

function makeFetch(data: unknown = mockInterceptors, ok = true) {
  return jest.fn(() =>
    Promise.resolve({ ok, json: () => Promise.resolve(data) }),
  );
}

describe("PipelineConfigCard", () => {
  it("renders interceptors after load", async () => {
    render(<PipelineConfigCard fetchWithAuth={makeFetch()} />);
    expect(await screen.findByText("RAG Interceptor")).toBeInTheDocument();
    expect(screen.getByText("Memory Interceptor")).toBeInTheDocument();
    expect(screen.getByText("Router Interceptor")).toBeInTheDocument();
  });

  it("renders title and subtitle", async () => {
    render(<PipelineConfigCard fetchWithAuth={makeFetch()} />);
    expect(await screen.findByText("Interceptor Pipeline")).toBeInTheDocument();
    expect(screen.getByText("Configure execution order")).toBeInTheDocument();
  });

  it("renders drag hint", async () => {
    render(<PipelineConfigCard fetchWithAuth={makeFetch()} />);
    expect(await screen.findByText("Drag to reorder")).toBeInTheDocument();
  });

  it("shows disabled badge", async () => {
    render(<PipelineConfigCard fetchWithAuth={makeFetch()} />);
    expect(await screen.findByText("Disabled")).toBeInTheDocument();
  });

  it("renders execution orders", async () => {
    render(<PipelineConfigCard fetchWithAuth={makeFetch()} />);
    await screen.findByText("RAG Interceptor");
    expect(screen.getByText("1")).toBeInTheDocument();
    expect(screen.getByText("2")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
  });

  it("shows descriptions", async () => {
    render(<PipelineConfigCard fetchWithAuth={makeFetch()} />);
    expect(await screen.findByText("Vector search")).toBeInTheDocument();
    expect(screen.getByText("Conversation history")).toBeInTheDocument();
  });

  it("save button disabled when clean", async () => {
    render(<PipelineConfigCard fetchWithAuth={makeFetch()} />);
    await screen.findByText("Save Order");
    expect(screen.getByText("Save Order")).toBeDisabled();
  });

});

