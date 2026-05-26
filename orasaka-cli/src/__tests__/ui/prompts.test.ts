/**
 * @file prompts.test.ts
 * @description Tests for prompts.ts using jest.isolateModules() for clean
 * module isolation — prevents cross-test cache contamination.
 */

// ── Build the mock @clack/prompts ────────────────────────────────────────────

const mockIntro = jest.fn();
const mockOutro = jest.fn();
const mockText = jest.fn();
const mockPassword = jest.fn();
const mockSpinner = jest.fn(() => ({
  start: jest.fn(),
  stop: jest.fn(),
  message: jest.fn(),
}));

type ClackPrompts = typeof import(
  "@clack/prompts",
  { with: { "resolution-mode": "import" } }
);

const mockModule = {
  intro: mockIntro,
  outro: mockOutro,
  text: mockText,
  password: mockPassword,
  spinner: mockSpinner,
  isCancel: (v: unknown): v is symbol => typeof v === "symbol",
} as unknown as ClackPrompts;

// ── Helper: get isolated module with injected mock ───────────────────────────

function getIsolatedPrompts() {
  let mod: typeof import("../../ui/prompts");

  jest.isolateModules(() => {
    mod = require("../../ui/prompts");
  });

  mod!._setTestLoader(async () => mockModule);
  return mod!;
}

beforeEach(() => {
  jest.clearAllMocks();
});

// ── intro ────────────────────────────────────────────────────────────────────

describe("intro", () => {
  test("delegates to @clack/prompts intro", async () => {
    const { intro } = getIsolatedPrompts();
    await intro("Welcome");
    expect(mockIntro).toHaveBeenCalledWith("Welcome");
  });
});

// ── outro ────────────────────────────────────────────────────────────────────

describe("outro", () => {
  test("delegates to @clack/prompts outro", async () => {
    const { outro } = getIsolatedPrompts();
    await outro("Goodbye");
    expect(mockOutro).toHaveBeenCalledWith("Goodbye");
  });
});

// ── text ─────────────────────────────────────────────────────────────────────

describe("text", () => {
  test("delegates with options", async () => {
    mockText.mockResolvedValue("user-input");
    const { text } = getIsolatedPrompts();
    const result = await text({ message: "Enter name" });

    expect(mockText).toHaveBeenCalledWith({ message: "Enter name" });
    expect(result).toBe("user-input");
  });

  test("passes placeholder and validate", async () => {
    const validate = (v: string) => (v.length < 3 ? "Too short" : undefined);
    mockText.mockResolvedValue("abc");
    const { text } = getIsolatedPrompts();

    await text({ message: "Name", placeholder: "John", validate });

    expect(mockText).toHaveBeenCalledWith({
      message: "Name",
      placeholder: "John",
      validate,
    });
  });
});

// ── password ─────────────────────────────────────────────────────────────────

describe("password", () => {
  test("delegates to @clack/prompts password", async () => {
    mockPassword.mockResolvedValue("secret123");
    const { password } = getIsolatedPrompts();
    const result = await password({ message: "Enter password" });

    expect(mockPassword).toHaveBeenCalledWith({ message: "Enter password" });
    expect(result).toBe("secret123");
  });
});

// ── createSpinner ────────────────────────────────────────────────────────────

describe("createSpinner", () => {
  test("returns spinner with start/stop/message", async () => {
    const { createSpinner } = getIsolatedPrompts();
    const spinner = await createSpinner();

    expect(spinner).toHaveProperty("start");
    expect(spinner).toHaveProperty("stop");
    expect(spinner).toHaveProperty("message");
    expect(typeof spinner.start).toBe("function");
  });
});

// ── isCancel ─────────────────────────────────────────────────────────────────

describe("isCancel", () => {
  test("returns true for symbols", () => {
    const { isCancel } = getIsolatedPrompts();
    expect(isCancel(Symbol("cancel"))).toBe(true);
  });

  test("returns false for strings", () => {
    const { isCancel } = getIsolatedPrompts();
    expect(isCancel("hello")).toBe(false);
  });

  test("returns false for null and undefined", () => {
    const { isCancel } = getIsolatedPrompts();
    expect(isCancel(null)).toBe(false);
    expect(isCancel(undefined)).toBe(false);
  });

  test("returns false for numbers", () => {
    const { isCancel } = getIsolatedPrompts();
    expect(isCancel(42)).toBe(false);
  });
});
