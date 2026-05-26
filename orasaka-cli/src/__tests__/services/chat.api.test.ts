/**
 * @file chat.api.test.ts
 * @description Tests for ChatApi — chat mutations, streaming, and SSE consumption.
 */

import { ChatApi } from "../../services/chat.api";
import { ApiClient } from "../../services/api-client";

jest.mock("../../services/api-client");
jest.mock("../../env", () => ({ GATEWAY_URL: "http://test-gateway:8080" }));

const mockRequestGql = ApiClient.requestGql as jest.MockedFunction<typeof ApiClient.requestGql>;

beforeEach(() => jest.clearAllMocks());

// ── chat ─────────────────────────────────────────────────────────────────────

describe("ChatApi.chat", () => {
  test("executes chat mutation via GraphQL", async () => {
    const response = { content: "Hello!", conversationId: "conv-1" };
    mockRequestGql.mockResolvedValue({ chat: response });

    const result = await ChatApi.chat("Hi", "conv-1");

    expect(mockRequestGql).toHaveBeenCalledWith(
      expect.stringContaining("Chat"),
      { prompt: "Hi", conversationId: "conv-1" },
    );
    expect(result).toEqual(response);
  });

  test("works without conversationId", async () => {
    mockRequestGql.mockResolvedValue({ chat: { content: "ok" } });

    await ChatApi.chat("test");

    expect(mockRequestGql).toHaveBeenCalledWith(
      expect.stringContaining("Chat"),
      { prompt: "test", conversationId: undefined },
    );
  });
});

// ── generateImage ────────────────────────────────────────────────────────────

describe("ChatApi.generateImage", () => {
  test("executes image mutation", async () => {
    const response = { content: "base64-data", conversationId: "c1" };
    mockRequestGql.mockResolvedValue({ image: response });

    const result = await ChatApi.generateImage("sunset", "sdxl");

    expect(mockRequestGql).toHaveBeenCalledWith(
      expect.stringContaining("GenerateImage"),
      { prompt: "sunset", model: "sdxl" },
    );
    expect(result).toEqual(response);
  });
});

// ── generateSpeech ───────────────────────────────────────────────────────────

describe("ChatApi.generateSpeech", () => {
  test("executes speech mutation", async () => {
    const response = { content: "audio-data" };
    mockRequestGql.mockResolvedValue({ speech: response });

    const result = await ChatApi.generateSpeech("hello world", "piper", "ryan");

    expect(mockRequestGql).toHaveBeenCalledWith(
      expect.stringContaining("GenerateSpeech"),
      { prompt: "hello world", model: "piper", voice: "ryan" },
    );
    expect(result).toEqual(response);
  });
});

// ── streamRest ───────────────────────────────────────────────────────────────

describe("ChatApi.streamRest", () => {
  function makeStream(chunks: string[]): ReadableStream<Uint8Array> {
    const encoder = new TextEncoder();
    return new ReadableStream({
      start(controller) {
        for (const chunk of chunks) {
          controller.enqueue(encoder.encode(chunk));
        }
        controller.close();
      },
    });
  }

  test("parses SSE tokens and calls onNext", async () => {
    const tokens: string[] = [];
    const body = makeStream([
      'data: {"content":"Hello"}\n\n',
      'data: {"content":" World"}\n\n',
      "data: [DONE]\n\n",
    ]);

    globalThis.fetch = jest.fn().mockResolvedValue({
      ok: true,
      status: 200,
      body,
    });

    const onComplete = jest.fn();

    await ChatApi.streamRest(
      "/api/v1/chat/stream",
      "conv-1",
      "Hi",
      "tok-123",
      (c) => tokens.push(c),
      jest.fn(),
      onComplete,
    );

    expect(tokens).toEqual(["Hello", " World"]);
    expect(onComplete).toHaveBeenCalled();
  });

  test("calls onError on HTTP failure", async () => {
    globalThis.fetch = jest.fn().mockResolvedValue({
      ok: false,
      status: 500,
      statusText: "Internal Server Error",
    });

    const onError = jest.fn();

    await ChatApi.streamRest(
      "/api/v1/chat/stream",
      "conv-1",
      "Hi",
      "tok-123",
      jest.fn(),
      onError,
      jest.fn(),
    );

    expect(onError).toHaveBeenCalledWith(expect.any(Error));
    expect(onError.mock.calls[0][0].message).toContain("500");
  });

  test("calls onError when body is null", async () => {
    globalThis.fetch = jest.fn().mockResolvedValue({
      ok: true,
      status: 200,
      body: null,
    });

    const onError = jest.fn();

    await ChatApi.streamRest(
      "/api/v1/chat/stream",
      "conv-1",
      "Hi",
      "tok-123",
      jest.fn(),
      onError,
      jest.fn(),
    );

    expect(onError).toHaveBeenCalledWith(expect.any(Error));
    expect(onError.mock.calls[0][0].message).toContain("No response body");
  });

  test("handles split SSE chunks correctly", async () => {
    const tokens: string[] = [];
    const body = makeStream([
      'data: {"conte',
      'nt":"split"}\n\ndata: [DONE]\n\n',
    ]);

    globalThis.fetch = jest.fn().mockResolvedValue({ ok: true, status: 200, body });

    await ChatApi.streamRest(
      "/api/v1/chat/stream",
      "conv-1",
      "Hi",
      "tok-123",
      (c) => tokens.push(c),
      jest.fn(),
      jest.fn(),
    );

    expect(tokens).toEqual(["split"]);
  });

  test("handles plain text SSE data", async () => {
    const tokens: string[] = [];
    const body = makeStream([
      "data: plain text\n\n",
      "data: [DONE]\n\n",
    ]);

    globalThis.fetch = jest.fn().mockResolvedValue({ ok: true, status: 200, body });

    await ChatApi.streamRest(
      "/api/v1/chat/stream",
      "conv-1",
      "Hi",
      "tok-123",
      (c) => tokens.push(c),
      jest.fn(),
      jest.fn(),
    );

    expect(tokens).toEqual(["plain text"]);
  });
});

// ── streamCodeRest ───────────────────────────────────────────────────────────

describe("ChatApi.streamCodeRest", () => {
  function makeStream(chunks: string[]): ReadableStream<Uint8Array> {
    const encoder = new TextEncoder();
    return new ReadableStream({
      start(controller) {
        for (const chunk of chunks) {
          controller.enqueue(encoder.encode(chunk));
        }
        controller.close();
      },
    });
  }

  test("streams code generation via POST", async () => {
    const tokens: string[] = [];
    const body = makeStream([
      'data: {"content":"function"}\n\n',
      'data: {"content":" hello()"}\n\n',
      "data: [DONE]\n\n",
    ]);

    globalThis.fetch = jest.fn().mockResolvedValue({ ok: true, status: 200, body });

    await ChatApi.streamCodeRest(
      "write a function",
      "gpt-4o",
      "tok",
      (c) => tokens.push(c),
      jest.fn(),
      jest.fn(),
    );

    expect(tokens).toEqual(["function", " hello()"]);
    expect(globalThis.fetch).toHaveBeenCalledWith(
      "http://test-gateway:8080/api/v1/chat/code",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ prompt: "write a function", model: "gpt-4o" }),
      }),
    );
  });

  test("calls onError on HTTP failure", async () => {
    globalThis.fetch = jest.fn().mockResolvedValue({
      ok: false,
      status: 502,
      statusText: "Bad Gateway",
    });

    const onError = jest.fn();

    await ChatApi.streamCodeRest("prompt", undefined, "tok", jest.fn(), onError, jest.fn());

    expect(onError).toHaveBeenCalledWith(expect.any(Error));
  });

  test("calls onError when body is null", async () => {
    globalThis.fetch = jest.fn().mockResolvedValue({ ok: true, status: 200, body: null });

    const onError = jest.fn();

    await ChatApi.streamCodeRest("prompt", undefined, "tok", jest.fn(), onError, jest.fn());

    expect(onError).toHaveBeenCalledWith(expect.any(Error));
  });
});

// ── SSE edge cases ───────────────────────────────────────────────────────────

describe("SSE edge cases", () => {
  function makeStream(chunks: string[]): ReadableStream<Uint8Array> {
    const encoder = new TextEncoder();
    return new ReadableStream({
      start(controller) {
        for (const chunk of chunks) {
          controller.enqueue(encoder.encode(chunk));
        }
        controller.close();
      },
    });
  }

  test("flushes remaining buffer when stream ends without [DONE]", async () => {
    const tokens: string[] = [];
    // Stream ends with data in buffer (no trailing \n\n, no [DONE])
    const body = makeStream([
      'data: {"content":"first"}\n\n',
      'data: {"content":"last"}',
    ]);

    globalThis.fetch = jest.fn().mockResolvedValue({ ok: true, status: 200, body });
    const onComplete = jest.fn();

    await ChatApi.streamRest(
      "/api/v1/chat/stream",
      "conv-1",
      "Hi",
      "tok",
      (c) => tokens.push(c),
      jest.fn(),
      onComplete,
    );

    expect(tokens).toContain("first");
    expect(tokens).toContain("last");
    expect(onComplete).toHaveBeenCalled();
  });

  test("stream error triggers onError callback", async () => {
    const body = new ReadableStream<Uint8Array>({
      start(controller) {
        controller.error(new Error("stream broke"));
      },
    });

    globalThis.fetch = jest.fn().mockResolvedValue({ ok: true, status: 200, body });
    const onError = jest.fn();

    await ChatApi.streamRest(
      "/api/v1/chat/stream",
      "conv-1",
      "Hi",
      "tok",
      jest.fn(),
      onError,
      jest.fn(),
    );

    expect(onError).toHaveBeenCalledWith(expect.any(Error));
  });

  test("fetch network error triggers onError", async () => {
    globalThis.fetch = jest.fn().mockRejectedValue(new Error("network down"));
    const onError = jest.fn();

    await ChatApi.streamRest(
      "/api/v1/chat/stream",
      "conv-1",
      "Hi",
      "tok",
      jest.fn(),
      onError,
      jest.fn(),
    );

    expect(onError).toHaveBeenCalledWith(expect.any(Error));
    expect(onError.mock.calls[0][0].message).toBe("network down");
  });

  test("handles non-Error thrown object in stream", async () => {
    globalThis.fetch = jest.fn().mockRejectedValue("string error");
    const onError = jest.fn();

    await ChatApi.streamRest(
      "/api/v1/chat/stream",
      "conv-1",
      "Hi",
      "tok",
      jest.fn(),
      onError,
      jest.fn(),
    );

    expect(onError).toHaveBeenCalledWith(expect.any(Error));
    expect(onError.mock.calls[0][0].message).toBe("string error");
  });

  test("handles empty SSE data lines gracefully", async () => {
    const tokens: string[] = [];
    const body = makeStream([
      "data: \n\n",
      'data: {"content":"real"}\n\n',
      "data: [DONE]\n\n",
    ]);

    globalThis.fetch = jest.fn().mockResolvedValue({ ok: true, status: 200, body });

    await ChatApi.streamRest(
      "/api/v1/chat/stream",
      "conv-1",
      "Hi",
      "tok",
      (c) => tokens.push(c),
      jest.fn(),
      jest.fn(),
    );

    expect(tokens).toEqual(["real"]);
  });

  test("ignores non-data SSE lines", async () => {
    const tokens: string[] = [];
    const body = makeStream([
      "event: message\n",
      'data: {"content":"hello"}\n\n',
      "id: 42\n",
      "data: [DONE]\n\n",
    ]);

    globalThis.fetch = jest.fn().mockResolvedValue({ ok: true, status: 200, body });

    await ChatApi.streamRest(
      "/api/v1/chat/stream",
      "conv-1",
      "Hi",
      "tok",
      (c) => tokens.push(c),
      jest.fn(),
      jest.fn(),
    );

    expect(tokens).toEqual(["hello"]);
  });
});

