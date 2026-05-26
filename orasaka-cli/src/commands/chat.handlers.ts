import * as fs from "node:fs";
import { createSpinner } from "../ui/prompts";
import { ChatApi } from "../services/chat.api";
import { MediaApi } from "../services/media.api";
import { ApiClient } from "../services/api-client";
import { renderTimeline } from "../renderers";
import { Logger } from "../ui/logger";
import { appendMessage } from "../threads";
import type { OperationNode } from "../types/api.types";
import type { ParsedArgs } from "./chat.types";

/**
 * Handles image generation via the ChatApi.
 */
export async function handleImageGeneration(
  parsed: ParsedArgs,
  conversationId: string,
): Promise<void> {
  const prompt = parsed.flagValue || parsed.prompt;
  const res = await ChatApi.generateImage(prompt, parsed.model);
  await renderTimeline({ kind: "image", content: res.content }, parsed.savePath);
  appendMessage(conversationId, { role: "assistant", content: res.content, kind: "image", timestamp: Date.now() });
}

/**
 * Handles speech synthesis via the ChatApi.
 */
export async function handleSpeechGeneration(
  parsed: ParsedArgs,
  conversationId: string,
): Promise<void> {
  const prompt = parsed.flagValue || parsed.prompt;
  const res = await ChatApi.generateSpeech(prompt, parsed.model, parsed.voice);
  await renderTimeline({ kind: "audio", content: res.content }, parsed.savePath);
  appendMessage(conversationId, { role: "assistant", content: res.content, kind: "audio", timestamp: Date.now() });
}

/**
 * Polls a media job until completion or timeout.
 * @returns The analysis result string.
 */
async function pollJobUntilComplete(
  jobId: string,
  spinner: { message: (msg: string) => void },
  label: string,
): Promise<string> {
  const maxAttempts = 240;
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    await new Promise((resolve) => setTimeout(resolve, 3000));
    const job = await MediaApi.getJobStatus(jobId);
    spinner.message(`Analyzing ${label}... Status: ${job.status}`);

    if (job.status === "COMPLETED") {
      return job.result?.analysis || "";
    }
    if (job.status === "FAILED") {
      throw new Error(job.errorMessage || `${label} analysis failed`);
    }
  }
  throw new Error(`${label} analysis timed out`);
}

/**
 * Handles vision (image) analysis via upload + polling.
 */
export async function handleVisionAnalysis(
  parsed: ParsedArgs,
  conversationId: string,
): Promise<void> {
  if (!parsed.flagValue || !fs.existsSync(parsed.flagValue)) {
    throw new Error(`File not found: ${parsed.flagValue}`);
  }

  const s = await createSpinner();
  s.start("Uploading image...");
  const uploadRes = await ApiClient.uploadFile(parsed.flagValue);
  s.message("Submitting vision analysis task...");
  const submitRes = await MediaApi.analyzeImage(parsed.prompt || "Analyze this image", uploadRes.assetId, parsed.model);
  const analysisResult = await pollJobUntilComplete(submitRes.jobId, s, "image");
  s.stop("Analysis complete");
  console.log(analysisResult);
  appendMessage(conversationId, { role: "assistant", content: analysisResult, kind: "image", timestamp: Date.now() });
}

/**
 * Handles audio analysis via upload + polling.
 */
export async function handleAudioAnalysis(
  parsed: ParsedArgs,
  conversationId: string,
): Promise<void> {
  if (!parsed.flagValue || !fs.existsSync(parsed.flagValue)) {
    throw new Error(`File not found: ${parsed.flagValue}`);
  }

  const s = await createSpinner();
  s.start("Uploading audio...");
  const uploadRes = await ApiClient.uploadFile(parsed.flagValue);
  s.message("Submitting audio analysis task...");
  const submitRes = await MediaApi.analyzeAudio(uploadRes.assetId, conversationId, parsed.model);
  const analysisResult = await pollJobUntilComplete(submitRes.jobId, s, "audio");
  s.stop("Analysis complete");
  console.log(analysisResult);
  appendMessage(conversationId, { role: "assistant", content: analysisResult, kind: "audio", timestamp: Date.now() });
}

/**
 * Handles text streaming via SSE REST.
 */
export async function handleTextStream(
  parsed: ParsedArgs,
  conversationId: string,
  token: string,
  node: OperationNode,
): Promise<void> {
  let accumulated = "";
  await ChatApi.streamRest(
    node.executionDetails.uriPath,
    conversationId,
    parsed.prompt,
    token,
    (content) => {
      process.stdout.write(content);
      accumulated += content;
    },
    (err) => {
      Logger.error(`Stream error: ${err.message}`);
    },
    () => {
      if (accumulated) {
        appendMessage(conversationId, { role: "assistant", content: accumulated, kind: "text", timestamp: Date.now() });
      }
    },
  );
}
