/**
 * @file env.ts
 * @description Centralized environment configuration for the CLI.
 * Loads the workspace root .env at first import and exports typed accessors.
 * Every module reads env vars through this single entry point — zero hardcoded URLs.
 */

import * as path from 'node:path';
import dotenv from "dotenv";
import { resolveWorkspaceRootSmart } from "./utils/platform";

// Load .env from the workspace root (single source of truth)
dotenv.config({ path: path.join(resolveWorkspaceRootSmart(), ".env"), quiet: true });

/** Base URL for the Orasaka Gateway (REST, GraphQL, SSE). */
export const GATEWAY_URL: string = process.env.GATEWAY_URL || "http://localhost:8080";
