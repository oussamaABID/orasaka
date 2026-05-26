import { Command } from "commander";
import { createSpinner } from "../ui/prompts";
import { requireAuth } from "../threads";
import { MediaApi } from "../services/media.api";
import { renderVideo } from "../renderers";
import { Logger } from "../ui/logger";

export const videoCommand = new Command("video")
  .description("Generate video from a text prompt")
  .argument("<prompt>", "Text prompt describing the desired video")
  .option("-d, --duration <seconds>", "Duration of the video in seconds", "4")
  .option("-m, --model <model>", "Specify AI model name")
  .option("-o, --output <path>", "Specific file path to save the video")
  .action(async (prompt: string, options: { duration: string; output?: string; model?: string }) => {
    requireAuth();

    const durationSeconds = parseInt(options.duration, 10) || 4;

    Logger.info(`Generating video (${durationSeconds}s)...`);
    Logger.hint(`Prompt: "${prompt}"`);
    if (options.model) {
      Logger.hint(`Model: "${options.model}"`);
    }
    Logger.hint("This may take a while depending on your hardware.\n");

    const s = await createSpinner();
    s.start("Submitting video generation task...");

    try {
      const submitRes = await MediaApi.generateVideo(prompt, durationSeconds, options.model);
      const jobId = submitRes.jobId;
      s.message(`Generating video... Status: ${submitRes.status}`);

      let attempts = 0;
      const maxAttempts = 240; // 20 minutes (5s interval)
      let finished = false;
      let finalUrl = "";

      while (!finished && attempts < maxAttempts) {
        await new Promise((resolve) => setTimeout(resolve, 5000));
        attempts++;

        const job = await MediaApi.getJobStatus(jobId);
        s.message(`Generating video... Status: ${job.status}`);

        if (job.status === "COMPLETED") {
          finished = true;
          finalUrl = job.result?.url || "";
        } else if (job.status === "FAILED") {
          throw new Error(job.errorMessage || "Video generation failed");
        }
      }

      if (!finished) {
        throw new Error("Video generation timed out");
      }

      s.stop("Generation complete");
      await renderVideo(finalUrl, options.output);
    } catch (err: unknown) {
      s.stop("Video generation failed");
      const msg = err instanceof Error ? err.message : "Unknown error";
      Logger.error(`Video Generation Failed: ${msg}`);
      process.exit(1);
    }
  });
