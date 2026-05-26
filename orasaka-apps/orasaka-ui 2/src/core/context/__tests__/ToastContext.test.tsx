/* eslint-disable @typescript-eslint/no-explicit-any */
import "@testing-library/jest-dom";
import * as React from "react";
import { render, screen, act, fireEvent } from "@testing-library/react";
import { ToastProvider, useToast } from "@/core/context/ToastContext";

// Polyfill randomUUID for JSDOM / Node environments if not present
if (typeof window !== "undefined" && !window.crypto) {
  Object.defineProperty(window, "crypto", {
    value: {
      randomUUID: () => "mock-uuid-1234",
    },
    writable: true,
  });
} else if (typeof global !== "undefined" && !(global as any).crypto) {
  (global as any).crypto = {
    randomUUID: () => "mock-uuid-1234",
  };
}

function TestConsumer() {
  const { addToast } = useToast();
  return (
    <div>
      <button onClick={() => addToast("Success Message", "success")}>
        Add Success
      </button>
      <button onClick={() => addToast("Info Message")}>
        Add Info
      </button>
    </div>
  );
}

function InvalidConsumer() {
  useToast();
  return <div>Invalid</div>;
}

describe("ToastContext & useToast", () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  it("throws error when useToast is used outside of ToastProvider", () => {
    // Suppress console.error for clean test output
    const consoleError = jest.spyOn(console, "error").mockImplementation(() => {});
    
    expect(() => render(<InvalidConsumer />)).toThrow(
      "useToast must be used within a ToastProvider"
    );
    
    consoleError.mockRestore();
  });

  it("renders children correctly", () => {
    render(
      <ToastProvider>
        <div data-testid="child">Hello World</div>
      </ToastProvider>
    );
    expect(screen.getByTestId("child")).toBeInTheDocument();
  });

  it("adds and dismisses toasts correctly", () => {
    render(
      <ToastProvider>
        <TestConsumer />
      </ToastProvider>
    );

    // No toast initially
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();

    // Click button to add toast wrapped in act
    const button = screen.getByText("Add Success");
    act(() => {
      fireEvent.click(button);
    });

    // Toast should appear
    expect(screen.getByRole("alert")).toBeInTheDocument();
    expect(screen.getByText("Success Message")).toBeInTheDocument();

    // Find the dismiss button
    const dismissBtn = screen.getByLabelText("Dismiss notification");
    expect(dismissBtn).toBeInTheDocument();

    // Click dismiss button wrapped in act
    act(() => {
      fireEvent.click(dismissBtn);
    });

    // It should trigger exiting state immediately
    const toast = screen.getByRole("alert");
    expect(toast.className).toContain("toast-exit");

    // Fast-forward exit animation time (200ms) wrapped in act
    act(() => {
      jest.advanceTimersByTime(200);
    });

    // Toast should be removed
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("automatically dismisses toasts after AUTO_DISMISS_MS", () => {
    render(
      <ToastProvider>
        <TestConsumer />
      </ToastProvider>
    );

    // Add info toast wrapped in act
    const button = screen.getByText("Add Info");
    act(() => {
      fireEvent.click(button);
    });

    expect(screen.getByText("Info Message")).toBeInTheDocument();

    // Fast-forward before auto dismiss (e.g. 3900ms) wrapped in act
    act(() => {
      jest.advanceTimersByTime(3900);
    });
    expect(screen.getByText("Info Message")).toBeInTheDocument();

    // Fast-forward to triggers AUTO_DISMISS_MS (4000ms) wrapped in act
    act(() => {
      jest.advanceTimersByTime(100);
    });
    // Now it should be marked as exiting
    expect(screen.getByRole("alert").className).toContain("toast-exit");

    // Fast forward exit animation (200ms) wrapped in act
    act(() => {
      jest.advanceTimersByTime(200);
    });
    expect(screen.queryByText("Info Message")).not.toBeInTheDocument();
  });
});
