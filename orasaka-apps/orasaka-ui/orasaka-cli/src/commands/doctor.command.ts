/**
 * @file doctor.command.ts
 * @description System diagnostics command with rich table output and progressive status reporting.
 * Validates host capabilities, dependencies, port allocations, and AI model assets.
 */

import { Command } from "commander";
import * as fs from "node:fs";
import * as path from "node:path";
import * as net from "node:net";
import chalk from "chalk";
import {
  intro,
  outro,
  note,
  logSuccess,
  logWarning,
  logError,
  logInfo,
  createSpinner,
} from "../ui/prompts";
import { TableFormatter } from "../ui/table";
import {
  hasTool,
  getToolVersion,
  getSystemInfo,
  resolveModelsDir,
} from "../ui/platform";

function checkPort(port: number, host: string = "127.0.0.1"): Promise<boolean> {
  return new Promise((resolve) => {
    const socket = new net.Socket();
    const onError = () => {
      socket.destroy();
      resolve(false);
    };
    socket.setTimeout(300);
    socket.once("connect", () => {
      socket.destroy();
      resolve(true);
    });
    socket.once("error", onError);
    socket.once("timeout", onError);
    socket.connect(port, host);
  });
}

/** Unified diagnostic check result. */
interface DiagResult {
  readonly name: string;
  readonly status: "pass" | "warn" | "fail";
  readonly detail: string;
}

function statusIcon(status: "pass" | "warn" | "fail"): string {
  switch (status) {
    case "pass": return chalk.green("✔ Pass");
    case "warn": return chalk.yellow("⚠ Warning");
    case "fail": return chalk.red("✖ Missing");
  }
}

export const doctorCommand = new Command("doctor")
  .description("Full system health check — hardware, dependencies, ports, and AI assets")
  .option("-q, --quiet", "Only show issues, suppress passing checks")
  .action(async (options: { quiet?: boolean }) => {
    await intro(chalk.cyan.bold("🥷 Orasaka System Diagnostics"));

    const spinner = await createSpinner();

    // ─── 1. Hardware & System ────────────────────────────────
    spinner.start("Inspecting hardware & system...");
    const sys = getSystemInfo();

    const hwLines = [
      `${chalk.yellow("OS")}             ${sys.platform} (${sys.arch})`,
      `${chalk.yellow("Host")}           ${sys.hostname}`,
      `${chalk.yellow("CPU")}            ${sys.cpuModel}`,
      `${chalk.yellow("Memory")}         ${String(sys.totalMemoryGb)} GB`,
      `${chalk.yellow("Node.js")}        ${sys.nodeVersion}`,
    ];

    if (sys.isAppleSilicon) {
      hwLines.push(`${chalk.yellow("Accelerator")}    ${chalk.green("✔ Apple Silicon Metal MPS")}`);
    } else if (sys.platform === "darwin") {
      hwLines.push(`${chalk.yellow("Accelerator")}    ${chalk.yellow("⚠ Intel Mac — GPU inference limited")}`);
    }

    spinner.stop("Hardware inspected.");
    await note(hwLines.join("\n"), "📋 System Specifications");

    // ─── 2. Tooling ──────────────────────────────────────────
    spinner.start("Checking CLI toolchain...");

    interface ToolDef {
      readonly name: string;
      readonly required: boolean;
      readonly installHint?: string;
    }

    const toolDefs: ToolDef[] = [
      { name: "docker", required: true, installHint: "brew install --cask docker" },
      { name: "node", required: true, installHint: "brew install node" },
      { name: "npm", required: true, installHint: "Included with Node.js" },
      { name: "git", required: true, installHint: "brew install git" },
      { name: "java", required: true, installHint: "brew install openjdk@21" },
      { name: "mvn", required: true, installHint: "brew install maven" },
      { name: "python3", required: false, installHint: "brew install python@3.11" },
      { name: "ollama", required: false, installHint: "brew install ollama" },
      { name: "terraform", required: false, installHint: "brew tap hashicorp/tap && brew install hashicorp/tap/terraform" },
      { name: "aws", required: false, installHint: "brew install awscli" },
      { name: "ffmpeg", required: false, installHint: "brew install ffmpeg" },
    ];

    const toolResults: DiagResult[] = [];
    for (const t of toolDefs) {
      const available = hasTool(t.name);
      if (available) {
        const version = getToolVersion(t.name);
        toolResults.push({ name: t.name, status: "pass", detail: version });
      } else {
        toolResults.push({
          name: t.name,
          status: t.required ? "fail" : "warn",
          detail: t.installHint ?? "Not found",
        });
      }
    }

    spinner.stop("Toolchain checked.");

    const toolRows = toolResults
      .filter((r) => !options.quiet || r.status !== "pass")
      .map((r) => [
        r.name.padEnd(14),
        statusIcon(r.status),
        r.status === "pass" ? chalk.gray(r.detail) : chalk.yellow(r.detail),
      ]);

    if (toolRows.length > 0) {
      TableFormatter.render(["Tool", "Status", "Version / Hint"], toolRows, {
        colWidths: [16, 14, 50],
      });
    } else {
      await logSuccess("All tools verified.");
    }

    const criticalMissing = toolResults.filter((r) => r.status === "fail");
    if (criticalMissing.length > 0) {
      console.log("");
      await logError(
        `${String(criticalMissing.length)} required tool(s) missing: ${chalk.red(criticalMissing.map((r) => r.name).join(", "))}\n` +
        "  Install them before running " + chalk.cyan("npx orasaka start") + "."
      );
    }

    // ─── 3. Port Allocation ──────────────────────────────────
    spinner.start("Scanning network ports...");

    const services = [
      { name: "PostgreSQL", port: 5432, required: true },
      { name: "Redis", port: 6379, required: true },
      { name: "RabbitMQ AMQP", port: 5672, required: true },
      { name: "RabbitMQ Mgmt", port: 15672, required: false },
      { name: "Gateway", port: 8080, required: true },
      { name: "Automation Worker", port: 8082, required: false },
      { name: "Image Worker", port: 8085, required: false },
      { name: "Image Gen", port: 8086, required: false },
      { name: "Video Worker", port: 8188, required: false },
      { name: "Ollama", port: 11434, required: false },
    ];

    const portRows: string[][] = [];
    for (const s of services) {
      const active = await checkPort(s.port);
      const status = active
        ? chalk.green("⚡ Active")
        : chalk.gray("○ Free");
      portRows.push([
        s.name.padEnd(18),
        String(s.port).padStart(5),
        status,
      ]);
    }

    spinner.stop("Ports scanned.");
    console.log("");

    TableFormatter.render(["Service", "Port", "Status"], portRows, {
      colWidths: [20, 8, 16],
    });

    // ─── 4. AI Model Assets ──────────────────────────────────
    spinner.start("Checking local AI models...");

    const modelsDir = resolveModelsDir();
    const modelFiles = [
      { file: "svd_xt.safetensors", label: "SVD XT (Video)" },
      { file: "ggml-tiny.bin", label: "Whisper STT" },
      { file: "en/en_US/lessac/medium/en_US-lessac-medium.onnx", label: "Piper TTS (ONNX)" },
      { file: "v1-5-pruned-emaonly.safetensors", label: "SD 1.5 Weights" },
      { file: "stable-diffusion.cpp/build/bin/sd-server", label: "sd-server Binary" },
    ];

    const modelRows: string[][] = [];
    let modelsMissing = 0;

    if (fs.existsSync(modelsDir)) {
      for (const m of modelFiles) {
        const fullPath = path.join(modelsDir, m.file);
        const exists = fs.existsSync(fullPath);
        if (!exists) modelsMissing++;
        modelRows.push([
          m.label.padEnd(20),
          exists ? chalk.green("✔ Found") : chalk.yellow("○ Missing"),
          chalk.gray(m.file),
        ]);
      }
    } else {
      modelsMissing = modelFiles.length;
    }

    spinner.stop("Model assets checked.");
    console.log("");

    if (!fs.existsSync(modelsDir)) {
      await logWarning(`Model directory not found: ${chalk.gray(modelsDir)}`);
    } else {
      const modelDisplay = modelRows.filter((r) => !options.quiet || !r[1]?.includes("Found"));
      if (modelDisplay.length > 0) {
        TableFormatter.render(["Model", "Status", "Path"], modelDisplay, {
          colWidths: [22, 14, 50],
        });
      }
    }

    if (modelsMissing > 0 && modelsMissing < modelFiles.length) {
      await logInfo(
        `${String(modelsMissing)} model(s) missing. These are optional for basic chat.\n` +
        `  Download to: ${chalk.gray(modelsDir)}`
      );
    }

    // ─── Summary ─────────────────────────────────────────────
    console.log("");
    const totalIssues = criticalMissing.length;
    if (totalIssues === 0) {
      await logSuccess("All critical checks passed. Your system is ready!");
    } else {
      await logWarning(`${String(totalIssues)} critical issue(s) detected. Fix them before starting.`);
    }

    await outro(
      totalIssues === 0
        ? chalk.green("Diagnostics complete — all systems go ✨")
        : chalk.yellow("Diagnostics complete — resolve issues above.")
    );
  });
