/**
 * @file renderers.ts
 * @description Output rendering pipeline for multi-modal CLI responses.
 * Handles text, image (Data URL → file), audio (Data URL → file), and video (Base64 → file).
 */

import * as fs from 'fs';
import * as path from 'path';
import { format } from 'date-fns';
import type { TimelineMessage } from './types';

const DATA_URL_REGEX = /^data:([^;]+);base64,(.+)$/;

/**
 * Extracts MIME type and raw binary bytes from an RFC 2397 Data URL.
 *
 * @param dataUrl - The full data URL string.
 * @returns Decoded buffer and MIME type, or null if the format is invalid.
 */
function decodeDataUrl(dataUrl: string): { mime: string; buffer: Buffer } | null {
  const match = dataUrl.match(DATA_URL_REGEX);
  if (!match) return null;
  return { mime: match[1], buffer: Buffer.from(match[2], 'base64') };
}

/**
 * Determines a file extension from a MIME type string.
 *
 * @param mime - The MIME type (e.g., `image/png`, `audio/mp3`).
 * @returns A file extension including the dot.
 */
function extensionFromMime(mime: string): string {
  const map: Record<string, string> = {
    'image/png': '.png',
    'image/jpeg': '.jpg',
    'image/webp': '.webp',
    'audio/mp3': '.mp3',
    'audio/mpeg': '.mp3',
    'audio/wav': '.wav',
    'video/mp4': '.mp4',
  };
  return map[mime] || '.bin';
}

/**
 * Writes raw binary data to disk and prints a confirmation.
 *
 * @param buffer - The binary payload.
 * @param savePath - Absolute or relative path to the target file.
 */
function writeToDisk(buffer: Buffer, savePath: string): void {
  const resolved = path.resolve(savePath);
  fs.mkdirSync(path.dirname(resolved), { recursive: true });
  fs.writeFileSync(resolved, buffer);
  const sizeKb = (buffer.length / 1024).toFixed(1);
  console.log(`\x1b[32m✓ Saved to ${resolved} (${sizeKb} KB)\x1b[0m`);
}

/**
 * Generates a timestamped filename for auto-saved media.
 *
 * @param prefix - A descriptive prefix (e.g., `orasaka-image`).
 * @param ext - The file extension including the dot.
 * @returns A filename string.
 */
function autoFileName(prefix: string, ext: string): string {
  const ts = format(new Date(), "yyyy-MM-dd'T'HH-mm-ss");
  return `${prefix}-${ts}${ext}`;
}

// ── Public Renderers ─────────────────────────────────────────────────────────

/**
 * Renders plain text content to stdout.
 *
 * @param content - The text content.
 */
export function renderText(content: string): void {
  console.log(content);
}

/**
 * Renders an image response. If the content is a Data URL, it is decoded and saved to disk.
 *
 * @param content - The image content (Data URL or plain text description).
 * @param savePath - Optional explicit save path; auto-generates if omitted.
 */
export function renderImage(content: string, savePath?: string): void {
  const decoded = decodeDataUrl(content);
  if (decoded) {
    const ext = extensionFromMime(decoded.mime);
    const target = savePath || autoFileName('orasaka-image', ext);
    writeToDisk(decoded.buffer, target);
    console.log(`\x1b[35m┌─ IMAGE OUTPUT ─────────────────────────────────┐\x1b[0m`);
    console.log(`\x1b[35m│\x1b[0m Format: ${decoded.mime.padEnd(40)} \x1b[35m│\x1b[0m`);
    console.log(`\x1b[35m│\x1b[0m Size:   ${(decoded.buffer.length / 1024).toFixed(1).padEnd(37)} KB \x1b[35m│\x1b[0m`);
    console.log(`\x1b[35m│\x1b[0m File:   ${path.basename(target).padEnd(40)} \x1b[35m│\x1b[0m`);
    console.log(`\x1b[35m└────────────────────────────────────────────────┘\x1b[0m`);
  } else {
    console.log(`\x1b[35m[IMAGE]\x1b[0m ${content}`);
  }
}

/**
 * Renders an audio response. If the content is a Data URL, it is decoded and saved to disk.
 *
 * @param content - The audio content (Data URL or plain link).
 * @param savePath - Optional explicit save path; auto-generates if omitted.
 */
export function renderAudio(content: string, savePath?: string): void {
  const decoded = decodeDataUrl(content);
  if (decoded) {
    const ext = extensionFromMime(decoded.mime);
    const target = savePath || autoFileName('orasaka-speech', ext);
    writeToDisk(decoded.buffer, target);
    console.log(`\x1b[33m🔊 Audio saved → ${path.resolve(target)}\x1b[0m`);
    console.log(`\x1b[90m   Hint: afplay "${path.resolve(target)}" (macOS)\x1b[0m`);
  } else {
    console.log(`\x1b[33m🔊 [AUDIO]\x1b[0m ${content}`);
  }
}

/**
 * Renders a video response by decoding the Base64 payload and saving to disk.
 *
 * @param content - The video content (Data URL with Base64 MP4).
 * @param savePath - Optional explicit save path; auto-generates if omitted.
 */
export function renderVideo(content: string, savePath?: string): void {
  const decoded = decodeDataUrl(content);
  if (decoded) {
    const ext = extensionFromMime(decoded.mime);
    const target = savePath || autoFileName('orasaka-video', ext);
    writeToDisk(decoded.buffer, target);
    const sizeMb = (decoded.buffer.length / (1024 * 1024)).toFixed(2);
    console.log(`\x1b[36m🎬 Video saved → ${path.resolve(target)} (${sizeMb} MB)\x1b[0m`);
    console.log(`\x1b[90m   Hint: open "${path.resolve(target)}" (macOS QuickTime)\x1b[0m`);
  } else {
    console.log(`\x1b[36m🎬 [VIDEO]\x1b[0m ${content}`);
  }
}

/**
 * Dispatches a timeline message to the appropriate media renderer.
 *
 * @param message - The timeline message with its discriminated kind.
 * @param savePath - Optional explicit save path for media output.
 */
export function renderTimeline(message: TimelineMessage, savePath?: string): void {
  switch (message.kind) {
    case 'text':
      renderText(message.content);
      break;
    case 'image':
      renderImage(message.content, savePath);
      break;
    case 'audio':
      renderAudio(message.content, savePath);
      break;
    case 'video':
      renderVideo(message.content, savePath);
      break;
    default: {
      const _exhaustive: never = message;
      return _exhaustive;
    }
  }
}
