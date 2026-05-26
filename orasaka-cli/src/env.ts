/**
 * @file env.ts
 * @description Centralized environment configuration for the CLI.
 * Loads .env.local via dotenv at first import and exports typed accessors.
 * Every module reads env vars through this single entry point — zero hardcoded URLs.
 */

import * as path from "path";
import dotenv from "dotenv";

// Load .env.local relative to the CLI package root (one level up from src/)
dotenv.config({ path: path.resolve(__dirname, "..", ".env.local") });

/** Base URL for the Orasaka Gateway (REST, GraphQL, SSE). */
export const GATEWAY_URL: string = process.env.GATEWAY_URL || "http://localhost:8080";
