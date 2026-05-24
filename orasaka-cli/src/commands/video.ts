/**
 * @file commands/video.ts
 * @description Text-to-Video generation via the gateway REST API.
 * Saves generated MP4 to disk with progress feedback.
 */

import { requireAuth } from '../threads';
import { renderVideo } from '../renderers';

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://localhost:8080';

/**
 * Generates a video from a text prompt and saves it to disk.
 *
 * @param args - CLI arguments: [prompt, --duration, seconds, --output, path].
 */
export async function handleVideo(args: string[]): Promise<void> {
  const config = requireAuth();
  const promptParts: string[] = [];
  let durationSeconds = 4;
  let outputPath: string | undefined;

  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--duration' && args[i + 1]) {
      durationSeconds = parseInt(args[++i], 10) || 4;
    } else if (args[i] === '--output' && args[i + 1]) {
      outputPath = args[++i];
    } else {
      promptParts.push(args[i]);
    }
  }

  const prompt = promptParts.join(' ').trim();
  if (!prompt) {
    console.error('\x1b[31mError: Video prompt is required.\x1b[0m');
    console.error('\x1b[90mUsage: orasaka video "A cat walking on the moon" --duration 4 --output ./video.mp4\x1b[0m');
    process.exit(1);
  }

  console.log(`\x1b[36m🎬 Generating video (${durationSeconds}s)...\x1b[0m`);
  console.log(`\x1b[90m   Prompt: "${prompt}"\x1b[0m`);
  console.log(`\x1b[90m   This may take a while depending on your hardware.\x1b[0m\n`);

  try {
    const response = await fetch(`${GATEWAY_URL}/api/v1/ai/video`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${config.token}`,
      },
      body: JSON.stringify({
        prompt,
        durationSeconds,
        settings: {},
      }),
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status} ${response.statusText}`);
    }

    const data = (await response.json()) as { url: string; format: string };
    renderVideo(data.url, outputPath);
  } catch (error: any) {
    console.error(`\x1b[31mVideo Generation Failed: ${error.message}\x1b[0m`);
    process.exit(1);
  }
}
