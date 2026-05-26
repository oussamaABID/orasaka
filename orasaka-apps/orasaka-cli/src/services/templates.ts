/**
 * @file templates.ts
 * @description Template engine for generating service skeletons, endpoints, and business logic.
 * Provides structured templates for common Orasaka module patterns.
 */

import * as fs from "node:fs";
import * as path from "node:path";
import { pascalCase, camelCase, kebabCase } from "./string-utils";

/**
 * Base interface for template context.
 */
export interface TemplateContext {
  readonly moduleName: string;
  readonly description: string;
  readonly year: number;
}

/**
 * Java service template.
 */
export function generateJavaService(ctx: TemplateContext): string {
  const className = `${pascalCase(ctx.moduleName)}Service`;
  const interfaceName = className;
  const implClassName = `${interfaceName}Impl`;

  const interfaceCode = `package com.orasaka.core.service;

/**
 * ${interfaceName}: ${ctx.description}
 * 
 * @author Orasaka
 * @since ${ctx.year}
 */
public interface ${interfaceName} {

    /**
     * Execute primary operation.
     * 
     * @param input the input parameter
     * @return result of the operation
     */
    String execute(String input);
}
`;

  const implCode = `package com.orasaka.core.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ${implClassName}: Implementation of ${interfaceName}.
 * 
 * @author Orasaka
 * @since ${ctx.year}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public final class ${implClassName} implements ${interfaceName} {

    @Override
    public String execute(final String input) {
        log.info("Executing {} with input: {}", "${className}", input);
        
        // TODO: Implement business logic
        return "Result: " + input;
    }
}
`;

  return { interface: interfaceCode, implementation: implCode } as any;
}

/**
 * Java REST controller template.
 */
export function generateJavaController(ctx: TemplateContext): string {
  const controllerName = `${pascalCase(ctx.moduleName)}Controller`;
  const serviceName = `${pascalCase(ctx.moduleName)}Service`;
  const serviceVarName = camelCase(ctx.moduleName);
  const endpoint = `/${kebabCase(ctx.moduleName)}`;

  return `package com.orasaka.gateway.adapter.rest;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.orasaka.core.service.${serviceName};

/**
 * ${controllerName}: REST API for ${ctx.description}.
 * 
 * @author Orasaka
 * @since ${new Date().getFullYear()}
 */
@Slf4j
@RestController
@RequestMapping("${endpoint}")
@RequiredArgsConstructor
public final class ${controllerName} {

    private final ${serviceName} ${serviceVarName}Service;

    /**
     * Execute operation via POST.
     * 
     * @param request the request body
     * @return the response
     */
    @PostMapping("/execute")
    public String execute(@RequestBody final String request) {
        log.info("POST ${endpoint}/execute - Input: {}", request);
        return ${serviceVarName}Service.execute(request);
    }

    /**
     * Health check endpoint.
     * 
     * @return status message
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
`;
}

/**
 * TypeScript React hook template.
 */
export function generateTypeScriptHook(ctx: TemplateContext): string {
  const hookName = `use${pascalCase(ctx.moduleName)}`;
  const endpoint = `/${kebabCase(ctx.moduleName)}`;

  return `import { useState, useCallback } from "react";

/**
 * ${hookName}: Custom hook for ${ctx.description}
 */
export function ${hookName}() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [data, setData] = useState<any>(null);

  const execute = useCallback(async (input: string) => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch("${endpoint}/execute", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ input }),
      });

      if (!response.ok) {
        throw new Error(\`HTTP Error: \${response.status}\`);
      }

      const result = await response.json();
      setData(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  }, []);

  return { execute, loading, error, data };
}
`;
}

/**
 * TypeScript Next.js server action template.
 */
export function generateNextServerAction(ctx: TemplateContext): string {
  const actionName = `execute${pascalCase(ctx.moduleName)}Action`;
  const endpoint = `/${kebabCase(ctx.moduleName)}`;

  return `"use server";

import { revalidatePath } from "next/cache";

/**
 * ${actionName}: Server action for ${ctx.description}
 */
export async function ${actionName}(input: string) {
  try {
    const response = await fetch(\`http://localhost:8080${endpoint}/execute\`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ input }),
    });

    if (!response.ok) {
      throw new Error(\`API Error: \${response.statusText}\`);
    }

    const result = await response.json();
    revalidatePath("/");
    return result;
  } catch (error) {
    console.error("Error executing action:", error);
    throw error;
  }
}
`;
}

/**
 * Maven POM module template.
 */
export function generateMavenPom(ctx: TemplateContext): string {
  const moduleName = kebabCase(ctx.moduleName);

  return `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.orasaka</groupId>
        <artifactId>orasaka-framework</artifactId>
        <version>\${project.version}</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>orasaka-${moduleName}</artifactId>
    <name>Orasaka ${pascalCase(moduleName)}</name>
    <description>${ctx.description}</description>

    <dependencies>
        <dependency>
            <groupId>\${project.groupId}</groupId>
            <artifactId>orasaka-core</artifactId>
        </dependency>

        <!-- Spring Boot Starter Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
`;
}

/**
 * Docker Compose service template.
 */
export function generateDockerComposeService(ctx: TemplateContext): string {
  const serviceName = kebabCase(ctx.moduleName);

  return `  ${serviceName}:
    image: node:24-slim
    container_name: orasaka-${serviceName}
    working_dir: /app
    environment:
      - NODE_ENV=production
    ports:
      - "3000:3000"
    volumes:
      - ./${serviceName}:/app
      - /app/node_modules
    command: npm run start
    restart: unless-stopped
    networks:
      - orasaka-network
`;
}

/**
 * Database migration template.
 */
export function generateSqlMigration(ctx: TemplateContext): string {
  const tableName = kebabCase(ctx.moduleName);

  return `-- Migration: Create ${tableName} table
-- Author: Orasaka
-- Date: ${new Date().toISOString()}

CREATE TABLE IF NOT EXISTS ${tableName} (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- TODO: Add your columns here
    name VARCHAR(255) NOT NULL,
    
    CONSTRAINT ${tableName}_unique_name UNIQUE (name)
);

-- Create index for common queries
CREATE INDEX idx_${tableName}_created_at ON ${tableName}(created_at DESC);

-- Add trigger to update updated_at
CREATE OR REPLACE FUNCTION update_${tableName}_updated_at()
RETURNS TRIGGER AS \$\$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
\$\$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_${tableName}_updated_at
BEFORE UPDATE ON ${tableName}
FOR EACH ROW
EXECUTE FUNCTION update_${tableName}_updated_at();
`;
}

/**
 * GraphQL resolver template.
 */
export function generateGraphQLResolver(ctx: TemplateContext): string {
  const queryName = `${camelCase(ctx.moduleName)}`;
  const typeName = pascalCase(ctx.moduleName);

  return `import { Query, Resolver } from "@nestjs/graphql";

/**
 * ${typeName}Resolver: GraphQL resolver for ${ctx.description}
 */
@Resolver()
export class ${typeName}Resolver {
  
  @Query(() => String)
  async ${queryName}(input: string): Promise<string> {
    // TODO: Implement resolver logic
    return \`Result from ${typeName}: \${input}\`;
  }
}
`;
}

/**
 * Template type definitions.
 */
export type TemplateType = 
  | "java-service" 
  | "java-controller" 
  | "typescript-hook" 
  | "nextjs-action" 
  | "maven-pom" 
  | "docker-compose" 
  | "sql-migration" 
  | "graphql-resolver";

/**
 * Generates template content by type.
 */
export function generateTemplate(type: TemplateType, ctx: TemplateContext): string {
  switch (type) {
    case "java-service":
      return JSON.stringify(generateJavaService(ctx));
    case "java-controller":
      return generateJavaController(ctx);
    case "typescript-hook":
      return generateTypeScriptHook(ctx);
    case "nextjs-action":
      return generateNextServerAction(ctx);
    case "maven-pom":
      return generateMavenPom(ctx);
    case "docker-compose":
      return generateDockerComposeService(ctx);
    case "sql-migration":
      return generateSqlMigration(ctx);
    case "graphql-resolver":
      return generateGraphQLResolver(ctx);
    default:
      throw new Error(\`Unknown template type: \${type}\`);
  }
}

/**
 * Writes template files to disk.
 */
export function writeTemplate(
  outputDir: string,
  type: TemplateType,
  ctx: TemplateContext,
  filename?: string,
): string {
  const content = generateTemplate(type, ctx);
  const defaultFilename = \`\${kebabCase(ctx.moduleName)}.\${getFileExtension(type)}\`;
  const finalFilename = filename || defaultFilename;
  const filepath = path.join(outputDir, finalFilename);

  fs.mkdirSync(outputDir, { recursive: true });
  fs.writeFileSync(filepath, content, "utf-8");

  return filepath;
}

/**
 * Returns file extension for template type.
 */
function getFileExtension(type: TemplateType): string {
  switch (type) {
    case "java-service":
    case "java-controller":
      return "java";
    case "typescript-hook":
    case "nextjs-action":
      return "ts";
    case "maven-pom":
      return "xml";
    case "docker-compose":
      return "yml";
    case "sql-migration":
      return "sql";
    case "graphql-resolver":
      return "ts";
    default:
      return "txt";
  }
}
