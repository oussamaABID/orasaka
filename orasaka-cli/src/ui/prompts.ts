/**
 * @file prompts.ts
 * @description Dynamic loader wrapper for ESM-only @clack/prompts in a CommonJS project.
 * Uses native TypeScript `typeof import()` with resolution-mode for zero-cast type safety.
 */

type ClackPrompts = typeof import(
  "@clack/prompts",
  { with: { "resolution-mode": "import" } }
);

let cache: ClackPrompts | null = null;
let loader: () => Promise<ClackPrompts> = () => import("@clack/prompts");

async function getPrompts(): Promise<ClackPrompts> {
  if (!cache) {
    cache = await loader();
  }
  return cache;
}

/**
 * Test-only: inject a mock loader and reset the module cache.
 * Production code must never call this.
 */
export function _setTestLoader(mockLoader: () => Promise<ClackPrompts>): void {
  cache = null;
  loader = mockLoader;
}

/**
 * Test-only: reset to the real @clack/prompts loader.
 */
export function _resetLoader(): void {
  cache = null;
  loader = () => import("@clack/prompts");
}

export async function intro(message: string): Promise<void> {
  const p = await getPrompts();
  p.intro(message);
}

export async function outro(message: string): Promise<void> {
  const p = await getPrompts();
  p.outro(message);
}

export async function text(options: {
  message: string;
  placeholder?: string;
  validate?: (value: string) => string | undefined;
}): Promise<string | symbol> {
  const p = await getPrompts();
  return p.text(options);
}

export async function password(options: {
  message: string;
  validate?: (value: string) => string | undefined;
}): Promise<string | symbol> {
  const p = await getPrompts();
  return p.password(options);
}

export interface ClackSpinner {
  start(msg?: string): void;
  stop(msg?: string, code?: number): void;
  message(msg?: string): void;
}

export async function createSpinner(): Promise<ClackSpinner> {
  const p = await getPrompts();
  return p.spinner();
}

export function isCancel(value: unknown): value is symbol {
  return typeof value === "symbol";
}
