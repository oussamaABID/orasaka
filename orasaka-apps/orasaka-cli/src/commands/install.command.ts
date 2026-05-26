import { Command } from "commander";
import * as fs from "node:fs";
import * as path from "node:path";
import * as crypto from "node:crypto";
import chalk from "chalk";
import {
  intro,
  outro,
  select,
  text,
  note,
  createSpinner,
  handleCancel,
} from "../ui/prompts";
import {
  resolveWorkspaceRoot,
  resolveEnvFile,
  writeEnvFile,
} from "../ui/platform";

/** Generates a cryptographically secure hex string. */
function secureHex(bytes: number): string {
  return crypto.randomBytes(bytes).toString("hex");
}

export const installCommand = new Command("install")
  .description("Interactive install wizard — configure target environments and dynamic topologies")
  .action(async () => {
    const workspaceRoot = resolveWorkspaceRoot();
    await intro(chalk.cyan.bold("🥷 Orasaka Installer Wizard"));

    // ─── Step 1: Prompt Target Environment ───────────────────
    const targetEnv = await select({
      message: "Select target environment profile:",
      options: [
        {
          value: "local_dev",
          label: "Local Dev (Apple Silicon ARM64)",
          hint: "Bypasses in-Docker Ollama to leverage macOS Metal acceleration",
        },
        {
          value: "production",
          label: "Production (Linux x86_64)",
          hint: "Hardened, containerized standard setup",
        },
      ],
      initialValue: "local_dev",
    });
    handleCancel(targetEnv);

    // ─── Step 2: Prompt Infrastructure Mode ───────────────────
    const infraMode = await select({
      message: "Select infrastructure topology:",
      options: [
        {
          value: "bundled",
          label: "Bundled (provisioned via Docker)",
          hint: "Postgres, Redis, and RabbitMQ spin up as local containers",
        },
        {
          value: "external",
          label: "External (connecting to enterprise clusters)",
          hint: "Bypasses local middleware container provisioning (BYO-Infra)",
        },
      ],
      initialValue: "bundled",
    });
    handleCancel(infraMode);

    // ─── Step 3: Collect External Config if selected ──────────
    const externalEndpoints: {
      dbHost?: string;
      redisHost?: string;
      rabbitmqHost?: string;
      ollamaHost?: string;
    } = {};

    if (infraMode === "external") {
      const dbHost = await text({
        message: "Enter external PostgreSQL hostname/IP:",
        placeholder: "e.g., pg-cluster.corp.internal",
        defaultValue: "external-db-host",
      });
      handleCancel(dbHost);
      externalEndpoints.dbHost = dbHost as string;

      const redisHost = await text({
        message: "Enter external Redis hostname/IP:",
        placeholder: "e.g., redis-cluster.corp.internal",
        defaultValue: "external-redis-host",
      });
      handleCancel(redisHost);
      externalEndpoints.redisHost = redisHost as string;

      const rabbitmqHost = await text({
        message: "Enter external RabbitMQ hostname/IP:",
        placeholder: "e.g., mq-cluster.corp.internal",
        defaultValue: "external-rabbitmq-host",
      });
      handleCancel(rabbitmqHost);
      externalEndpoints.rabbitmqHost = rabbitmqHost as string;

      if (targetEnv !== "local_dev") {
        const ollamaHost = await text({
          message: "Enter external Ollama hostname/IP:",
          placeholder: "e.g., ollama-server.corp.internal",
          defaultValue: "external-ollama-host",
        });
        handleCancel(ollamaHost);
        externalEndpoints.ollamaHost = ollamaHost as string;
      }
    }

    const spinner = await createSpinner();
    spinner.start("Generating deployment artifacts...");

    // ─── Step 4: Write Environment File (.env) ───────────────
    const envPath = resolveEnvFile();
    let envContent = "";
    const templatePaths = [
      path.join(workspaceRoot, "exemple.env.txt"),
      path.join(workspaceRoot, ".env.example"),
      path.join(workspaceRoot, ".env.template"),
    ];

    let templateFound = false;
    for (const tp of templatePaths) {
      if (fs.existsSync(tp)) {
        envContent = fs.readFileSync(tp, "utf-8");
        templateFound = true;
        break;
      }
    }

    if (!templateFound) {
      // Basic fallback env
      envContent = [
        "PORT=8080",
        "SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/orasaka_db",
        "SPRING_DATASOURCE_USERNAME=orasaka_admin",
        "SPRING_DATASOURCE_PASSWORD=placeholder",
        "REDIS_URL=redis://localhost:6379",
        "SPRING_RABBITMQ_HOST=localhost",
        "SPRING_RABBITMQ_PORT=5672",
        "DEFAULT_PROVIDER=ollama",
        "SPRING_AI_OLLAMA_BASE_URL=http://localhost:11434",
        "CRYPTO_KEY=placeholder",
        "CRYPTO_SALT=placeholder",
      ].join("\n");
    }

    // Write initial .env if not exists
    if (!fs.existsSync(envPath)) {
      fs.writeFileSync(envPath, envContent, "utf-8");
    }

    // Update .env fields based on selections
    const envUpdates: Record<string, string> = {
      DEFAULT_PROVIDER: "ollama",
    };

    // Inject secure random values if present in base template
    if (envContent.includes("SPRING_DATASOURCE_PASSWORD=")) {
      envUpdates["SPRING_DATASOURCE_PASSWORD"] = secureHex(16);
    }
    if (envContent.includes("CRYPTO_KEY=")) {
      envUpdates["CRYPTO_KEY"] = secureHex(16);
    }
    if (envContent.includes("CRYPTO_SALT=")) {
      envUpdates["CRYPTO_SALT"] = secureHex(8);
    }

    // Handle environment and infra mode variable changes
    if (targetEnv === "local_dev") {
      envUpdates["SPRING_AI_OLLAMA_BASE_URL"] = "http://host.docker.internal:11434";
      envUpdates["OLLAMA_BASE_URL"] = "http://host.docker.internal:11434";
    } else {
      if (infraMode === "external" && externalEndpoints.ollamaHost) {
        envUpdates["SPRING_AI_OLLAMA_BASE_URL"] = `http://${externalEndpoints.ollamaHost}:11434`;
        envUpdates["OLLAMA_BASE_URL"] = `http://${externalEndpoints.ollamaHost}:11434`;
      } else {
        envUpdates["SPRING_AI_OLLAMA_BASE_URL"] = "http://ollama:11434";
        envUpdates["OLLAMA_BASE_URL"] = "http://ollama:11434";
      }
    }

    if (infraMode === "external") {
      const dbH = externalEndpoints.dbHost || "external-db-host";
      const redisH = externalEndpoints.redisHost || "external-redis-host";
      const rabbitmqH = externalEndpoints.rabbitmqHost || "external-rabbitmq-host";

      envUpdates["SPRING_DATASOURCE_URL"] = `jdbc:postgresql://${dbH}:5432/orasaka_db`;
      envUpdates["REDIS_URL"] = `redis://${redisH}:6379`;
      envUpdates["SPRING_RABBITMQ_HOST"] = rabbitmqH;
    } else {
      // Bundled local targets
      envUpdates["SPRING_DATASOURCE_URL"] = "jdbc:postgresql://db-vector:5432/orasaka_db";
      envUpdates["REDIS_URL"] = "redis://redis:6379";
      envUpdates["SPRING_RABBITMQ_HOST"] = "rabbitmq";
    }

    writeEnvFile(envPath, envUpdates);

    // ─── Step 5: Generate docker-compose.override.yml ──────
    const overrideLines: string[] = [
      "# ==============================================================================",
      "# ORASAKA — Docker Compose Override (Dynamic Configuration)",
      "# Generated by: npx orasaka install",
      `# Environment: ${targetEnv === "local_dev" ? "Local Dev (Apple Silicon)" : "Production (Linux)"}`,
      `# Infrastructure: ${infraMode === "bundled" ? "Bundled (Docker)" : "External Clusters"}`,
      "# ==============================================================================",
      "version: '3.8'",
      "services:",
    ];

    if (targetEnv === "local_dev") {
      overrideLines.push(
        "  ollama:",
        "    scale: 0",
        "",
        "  gateway:",
        "    environment:",
        "      - SPRING_AI_OLLAMA_BASE_URL=http://host.docker.internal:11434",
        "    extra_hosts:",
        "      - \"host.docker.internal:host-gateway\"",
        ""
      );
    }

    if (infraMode === "external") {
      overrideLines.push(
        "  db-vector:",
        "    scale: 0",
        "",
        "  redis:",
        "    scale: 0",
        "",
        "  rabbitmq:",
        "    scale: 0",
        ""
      );

      if (targetEnv !== "local_dev") {
        overrideLines.push("  ollama:", "    scale: 0", "");
      }

      const dbH = externalEndpoints.dbHost || "external-db-host";
      const redisH = externalEndpoints.redisHost || "external-redis-host";
      const rabbitmqH = externalEndpoints.rabbitmqHost || "external-rabbitmq-host";

      overrideLines.push(
        "  gateway:",
        "    environment:",
        `      - SPRING_DATASOURCE_URL=jdbc:postgresql://${dbH}:5432/\${DB_NAME:-orasaka_db}`,
        `      - REDIS_URL=redis://${redisH}:6379`,
        `      - SPRING_RABBITMQ_HOST=${rabbitmqH}`
      );

      if (targetEnv !== "local_dev") {
        const ollamaH = externalEndpoints.ollamaHost || "external-ollama-host";
        overrideLines.push(
          `      - SPRING_AI_OLLAMA_BASE_URL=http://${ollamaH}:11434`
        );
      } else {
        overrideLines.push(
          "      - SPRING_AI_OLLAMA_BASE_URL=http://host.docker.internal:11434",
          "    extra_hosts:",
          "      - \"host.docker.internal:host-gateway\""
        );
      }
      overrideLines.push("");
    }

    const overrideContent = overrideLines.join("\n");
    const infraOverridePath = path.join(
      workspaceRoot,
      "infra",
      "docker-compose.override.yml"
    );

    // Ensure infra directory exists before writing to it
    const infraDir = path.dirname(infraOverridePath);
    if (!fs.existsSync(infraDir)) {
      fs.mkdirSync(infraDir, { recursive: true });
    }
    fs.writeFileSync(infraOverridePath, overrideContent, "utf-8");

    spinner.stop(
      "Generated environment config and docker-compose.override.yml successfully!"
    );

    // ─── Summary ───
    const summary = [
      `${chalk.green("✔")} Target: ${chalk.cyan(
        targetEnv === "local_dev"
          ? "Local Dev (Apple Silicon)"
          : "Production (Linux)"
      )}`,
      `${chalk.green("✔")} Infra:  ${chalk.cyan(
        infraMode === "bundled" ? "Bundled (Docker)" : "External Clusters"
      )}`,
      `${chalk.green("✔")} Generated: docker-compose.override.yml`,
      `${chalk.green("✔")} Configured: .env`,
      "",
      `To launch Orasaka with this topology, run:`,
      `  ${chalk.cyan("npx orasaka start")}`,
    ];

    await note(summary.join("\n"), "✨ Installer Configuration Complete");
    outro(chalk.green("Orasaka ready. Happy building! 🚀"));
  });
