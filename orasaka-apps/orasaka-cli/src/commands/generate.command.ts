/**
 * @file generate.command.ts
 * @description Template generation command for creating service skeletons,
 * endpoints, migrations, and other boilerplate code.
 */

import { Command } from "commander";
import * as path from "node:path";
import chalk from "chalk";
import {
  intro,
  outro,
  select,
  text,
  logSuccess,
  logError,
  logStep,
  note,
  confirm,
} from "../ui/prompts";
import { generateTemplate, writeTemplate, type TemplateType } from "../services/templates";
import { isValidKebabCase } from "../services/string-utils";

export const generateCommand = new Command("generate")
  .aliases(["gen", "scaffold"])
  .description("Generate service skeletons, endpoints, and boilerplate code")
  .action(async () => {
    await intro(chalk.cyan.bold("🥷 Orasaka Code Generator"));

    // Step 1: Select template type
    const templateType = await select({
      message: "What would you like to generate?",
      options: [
        {
          value: "java-service" as const,
          label: "Java Service & Implementation",
          hint: "Create a new Spring service with interface",
        },
        {
          value: "java-controller" as const,
          label: "Java REST Controller",
          hint: "Create a @RestController for your service",
        },
        {
          value: "typescript-hook" as const,
          label: "TypeScript React Hook",
          hint: "Create a custom React hook with API integration",
        },
        {
          value: "nextjs-action" as const,
          label: "Next.js Server Action",
          hint: "Create a server action for Next.js App Router",
        },
        {
          value: "maven-pom" as const,
          label: "Maven Module (pom.xml)",
          hint: "Create a new Maven module structure",
        },
        {
          value: "docker-compose" as const,
          label: "Docker Compose Service",
          hint: "Create a containerized service definition",
        },
        {
          value: "sql-migration" as const,
          label: "Database Migration (SQL)",
          hint: "Create a PostgreSQL migration script",
        },
        {
          value: "graphql-resolver" as const,
          label: "GraphQL Resolver",
          hint: "Create a GraphQL resolver with NestJS",
        },
      ],
      initialValue: "java-service",
    });

    // Step 2: Get module name
    const moduleName = await text({
      message: "Enter module/feature name (kebab-case):",
      placeholder: "e.g., user-auth, payment-processor",
      validate: (value) => {
        if (!value.trim()) return "Name cannot be empty";
        if (!isValidKebabCase(value)) {
          return "Must be lowercase with hyphens only (e.g., my-module)";
        }
        return true;
      },
    });

    // Step 3: Get description
    const description = await text({
      message: "Brief description of the module:",
      placeholder: "e.g., User authentication and session management",
      defaultValue: `Module for ${moduleName}`,
    });

    // Step 4: Show summary
    await logStep(`Generating template: ${chalk.bold(templateType)}`);
    await note(
      [
        `${chalk.yellow("Name")}        ${moduleName}`,
        `${chalk.yellow("Description")} ${description}`,
        `${chalk.yellow("Type")}        ${templateType}`,
      ].join("\n"),
      "Template Details"
    );

    // Step 5: Confirm generation
    const shouldGenerate = await confirm({
      message: "Generate template?",
      initialValue: true,
    });

    if (!shouldGenerate) {
      await outro(chalk.yellow("Generation cancelled."));
      return;
    }

    // Step 6: Generate and write template
    try {
      const outputDir = process.cwd();
      const ctx = {
        moduleName,
        description,
        year: new Date().getFullYear(),
      };

      const filepath = writeTemplate(outputDir, templateType, ctx);

      await logSuccess(
        `Template generated successfully!\n` +
        `  Location: ${chalk.cyan(path.relative(process.cwd(), filepath))}`
      );

      // Show next steps based on template type
      const nextSteps = getNextSteps(templateType, moduleName);
      if (nextSteps.length > 0) {
        await note(nextSteps.join("\n"), "Next Steps");
      }

      await outro(chalk.cyan("✨ Happy coding!"));
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Unknown error";
      await logError(`Failed to generate template: ${msg}`);
      process.exit(1);
    }
  });

/**
 * Returns next steps based on template type.
 */
function getNextSteps(type: TemplateType, moduleName: string): string[] {
  switch (type) {
    case "java-service":
      return [
        `1. Place files in: ${chalk.cyan("orasaka-framework/orasaka-core/src/main/java/com/orasaka/core/service/")}`,
        `2. Update ${chalk.cyan("pom.xml")} if adding new dependencies`,
        `3. Run tests: ${chalk.gray("npm run test")}`,
      ];

    case "java-controller":
      return [
        `1. Place in: ${chalk.cyan("orasaka-apps/orasaka-gateway/src/main/java/com/orasaka/gateway/adapter/rest/")}`,
        `2. Add service dependency injection`,
        `3. Test endpoint: ${chalk.gray("curl -X POST http://localhost:8080/api/v1/...")}`,
      ];

    case "typescript-hook":
      return [
        `1. Place in: ${chalk.cyan("orasaka-apps/orasaka-ui/src/hooks/")}`,
        `2. Update exports in ${chalk.cyan("hooks/index.ts")}`,
        `3. Use in components: ${chalk.gray(`import { use${moduleName} } from '@/hooks'`)}`,
      ];

    case "nextjs-action":
      return [
        `1. Place in: ${chalk.cyan("orasaka-apps/orasaka-ui/src/app/actions/")}`,
        `2. Import in client component with ${chalk.gray("'use client'")}`,
        `3. Call from form or button handlers`,
      ];

    case "maven-pom":
      return [
        `1. Create directory: ${chalk.cyan("orasaka-framework/orasaka-" + moduleName)}`,
        `2. Place pom.xml in the new directory`,
        `3. Add to parent pom.xml: ${chalk.gray("<module>orasaka-" + moduleName + "</module>")}`,
        `4. Build: ${chalk.gray("./mvnw clean install")}`,
      ];

    case "docker-compose":
      return [
        `1. Add to ${chalk.cyan("infra/docker-compose.yml")} services section`,
        `2. Add network configuration if needed`,
        `3. Test: ${chalk.gray("docker-compose up -d " + moduleName)}`,
      ];

    case "sql-migration":
      return [
        `1. Place in: ${chalk.cyan("infra/local-db/")}`,
        `2. Follow naming: ${chalk.gray("NN-description.sql")}`,
        `3. Reload database: ${chalk.gray("npx orasaka start --reinit-db")}`,
      ];

    case "graphql-resolver":
      return [
        `1. Place in: ${chalk.cyan("orasaka-apps/orasaka-gateway/src/graphql/resolvers/")}`,
        `2. Register in module's resolver provider list`,
        `3. Test with GraphQL playground on port 8080/graphql`,
      ];

    default:
      return [];
  }
}
