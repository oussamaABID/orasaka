/**
 * @file executeFeature.test.ts
 * @description Tests for executeFeatureWithPrompt — payload template
 * compilation, UUID extraction, and API call delegation.
 */

import { executeFeatureWithPrompt } from "../executeFeature";
import type { BootstrapFeature } from "@/features/chat-session/components/ContextPlusMenu";

// ── Mocks ────────────────────────────────────────────────────────────────────

const mockFetchResponse = (
  body: unknown,
  ok = true,
  status = 200,
  contentType = "application/json",
) => {
  global.fetch = jest.fn().mockResolvedValue({
    ok,
    status,
    headers: new Map([["content-type", contentType]]),
    json: jest.fn().mockResolvedValue(body),
    text: jest
      .fn()
      .mockResolvedValue(
        typeof body === "string" ? body : JSON.stringify(body),
      ),
  } as unknown as Response);
};

const baseFeature: BootstrapFeature = {
  id: "orasaka.core.chat.image",
  label: "Image Gen",
  icon: "image",
  uriPath: "/api/v1/chat/image",
  httpMethod: "POST",
  payloadTemplate: '{"prompt":"${prompt}","model":"${model}"}',
};

afterEach(() => {
  jest.restoreAllMocks();
});

// ── Template compilation ─────────────────────────────────────────────────────

describe("executeFeatureWithPrompt", () => {
  test("compiles payload template with prompt and model", async () => {
    mockFetchResponse({ content: "generated image" });

    await executeFeatureWithPrompt({
      feature: baseFeature,
      prompt: "a sunset",
    });

    const call = (global.fetch as jest.Mock).mock.calls[0];
    const body = JSON.parse(call[1].body);
    expect(body.prompt).toBe("a sunset");
    expect(body.model).toBe("sdxl-turbo-gguf");
  });

  test("extracts UUID from prompt text", async () => {
    const uuid = "12345678-1234-1234-1234-123456789012";
    const feature: BootstrapFeature = {
      ...baseFeature,
      payloadTemplate: '{"assetId":"${assetId}"}',
    };
    mockFetchResponse({ content: "done" });

    await executeFeatureWithPrompt({
      feature,
      prompt: `Process asset ${uuid}`,
    });

    const call = (global.fetch as jest.Mock).mock.calls[0];
    const body = JSON.parse(call[1].body);
    expect(body.assetId).toBe(uuid);
  });

  test("uses assetId param when no UUID in prompt", async () => {
    const feature: BootstrapFeature = {
      ...baseFeature,
      payloadTemplate: '{"image":"${image}"}',
    };
    mockFetchResponse({ content: "done" });

    await executeFeatureWithPrompt({
      feature,
      prompt: "no uuid here",
      assetId: "custom-asset-id",
    });

    const call = (global.fetch as jest.Mock).mock.calls[0];
    const body = JSON.parse(call[1].body);
    expect(body.image).toBe("custom-asset-id");
  });

  test("falls back to null UUID when no UUID found", async () => {
    const feature: BootstrapFeature = {
      ...baseFeature,
      payloadTemplate: '{"assetId":"${assetId}"}',
    };
    mockFetchResponse({ content: "done" });

    await executeFeatureWithPrompt({
      feature,
      prompt: "no uuid here",
    });

    const call = (global.fetch as jest.Mock).mock.calls[0];
    const body = JSON.parse(call[1].body);
    expect(body.assetId).toBe("00000000-0000-0000-0000-000000000000");
  });

  // ── Response handling ────────────────────────────────────────────────────

  test("extracts content from JSON response", async () => {
    mockFetchResponse({ content: "beautiful sunset image" });

    const result = await executeFeatureWithPrompt({
      feature: baseFeature,
      prompt: "sunset",
    });

    expect(result.content).toBe("beautiful sunset image");
    expect(result.kind).toBe("image");
  });

  test("extracts analysis field from JSON response", async () => {
    mockFetchResponse({ analysis: "the image shows a cat" });

    const result = await executeFeatureWithPrompt({
      feature: baseFeature,
      prompt: "analyze",
    });

    expect(result.content).toBe("the image shows a cat");
  });

  test("formats queued job response", async () => {
    mockFetchResponse({
      jobId: "12345678-abcd-1234-1234-123456789012",
      status: "PENDING",
    });

    const result = await executeFeatureWithPrompt({
      feature: baseFeature,
      prompt: "generate",
    });

    expect(result.content).toContain("Task queued");
    expect(result.content).toContain("12345678");
  });

  test("handles plain text response", async () => {
    mockFetchResponse("plain text result", true, 200, "text/plain");

    const result = await executeFeatureWithPrompt({
      feature: baseFeature,
      prompt: "test",
    });

    expect(result.content).toBe("plain text result");
  });

  // ── Error handling ─────────────────────────────────────────────────────

  test("throws on 403 with access forbidden message", async () => {
    mockFetchResponse({}, false, 403);

    await expect(
      executeFeatureWithPrompt({ feature: baseFeature, prompt: "test" }),
    ).rejects.toThrow("Access Forbidden");
  });

  test("throws on non-ok response with status", async () => {
    mockFetchResponse({}, false, 500);

    await expect(
      executeFeatureWithPrompt({ feature: baseFeature, prompt: "test" }),
    ).rejects.toThrow("status 500");
  });

  // ── GET method ─────────────────────────────────────────────────────────

  test("does not send body for GET requests", async () => {
    const getFeature: BootstrapFeature = {
      ...baseFeature,
      httpMethod: "GET",
    };
    mockFetchResponse({ content: "data" });

    await executeFeatureWithPrompt({ feature: getFeature, prompt: "test" });

    const call = (global.fetch as jest.Mock).mock.calls[0];
    expect(call[1].body).toBeUndefined();
  });

  // ── Default payload template ───────────────────────────────────────────

  test("uses default template when none provided", async () => {
    const noTemplate: BootstrapFeature = {
      ...baseFeature,
      payloadTemplate: undefined,
    };
    mockFetchResponse({ content: "ok" });

    await executeFeatureWithPrompt({ feature: noTemplate, prompt: "hello" });

    const call = (global.fetch as jest.Mock).mock.calls[0];
    expect(call[1].body).toContain("hello");
  });
});
