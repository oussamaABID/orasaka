/**
 * @file renderers.test.ts
 * @description Tests for the CLI output rendering pipeline — text, image, audio, video.
 */

import * as fs from "fs";
import * as path from "path";
import * as os from "os";
import {
  renderText,
  renderImage,
  renderAudio,
  renderVideo,
  renderTimeline,
} from "../renderers";
import type { TimelineMessage } from "../types";

// Capture console.log output
let logOutput: string[] = [];
const originalLog = console.log;
beforeEach(() => {
  logOutput = [];
  console.log = (...args: unknown[]) => {
    logOutput.push(args.map(String).join(" "));
  };
});
afterEach(() => {
  console.log = originalLog;
});

describe("renderText", () => {
  test("prints content to stdout", () => {
    renderText("Hello, world!");
    expect(logOutput).toContain("Hello, world!");
  });

  test("handles empty string", () => {
    renderText("");
    expect(logOutput).toContain("");
  });
});

describe("renderImage", () => {
  test("plain text fallback when not a Data URL", () => {
    renderImage("A description of an image");
    expect(logOutput.some((l) => l.includes("[IMAGE]"))).toBe(true);
    expect(logOutput.some((l) => l.includes("A description of an image"))).toBe(true);
  });

  test("decodes and saves valid PNG Data URL", () => {
    const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "render-test-"));
    const savePath = path.join(tmpDir, "test.png");

    // Minimal 1x1 PNG in base64
    const pngBase64 =
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
    renderImage(`data:image/png;base64,${pngBase64}`, savePath);

    expect(fs.existsSync(savePath)).toBe(true);
    expect(logOutput.some((l) => l.includes("IMAGE OUTPUT"))).toBe(true);
    // Cleanup
    fs.rmSync(tmpDir, { recursive: true });
  });
});

describe("renderAudio", () => {
  test("plain text fallback when not a Data URL", () => {
    renderAudio("https://example.com/audio.mp3");
    expect(logOutput.some((l) => l.includes("[AUDIO]"))).toBe(true);
  });
});

describe("renderVideo", () => {
  test("plain text fallback when not a Data URL", () => {
    renderVideo("https://example.com/video.mp4");
    expect(logOutput.some((l) => l.includes("[VIDEO]"))).toBe(true);
  });
});

describe("renderTimeline", () => {
  test("dispatches text message to renderText", () => {
    const msg: TimelineMessage = { kind: "text", content: "Hello" };
    renderTimeline(msg);
    expect(logOutput).toContain("Hello");
  });

  test("dispatches image message to renderImage", () => {
    const msg: TimelineMessage = { kind: "image", content: "An image" };
    renderTimeline(msg);
    expect(logOutput.some((l) => l.includes("[IMAGE]"))).toBe(true);
  });

  test("dispatches audio message to renderAudio", () => {
    const msg: TimelineMessage = { kind: "audio", content: "audio link" };
    renderTimeline(msg);
    expect(logOutput.some((l) => l.includes("[AUDIO]"))).toBe(true);
  });

  test("dispatches video message to renderVideo", () => {
    const msg: TimelineMessage = { kind: "video", content: "video link" };
    renderTimeline(msg);
    expect(logOutput.some((l) => l.includes("[VIDEO]"))).toBe(true);
  });
});
