/**
 * @file commands/chat.ts
 * @description Interactive and single-shot chat command with multi-modal capabilities.
 * Uses GraphQL mutations for image/speech generation and SSE for text streaming.
 * Supports thread management via /thread meta-commands in interactive mode.
 */

import * as fs from 'fs';
import { CliClient } from '../client';
import {
  requireAuth,
  loadConfig,
  saveConfig,
  createThread,
  listThreads,
  switchThread,
  updateThreadTitle,
  appendMessage,
} from '../threads';
import { renderTimeline } from '../renderers';
import { promptInput } from './login';
import type {
  CapabilityDescriptor,
  ChatInput,
  OperationNode,
  CliConfig,
} from '../types';

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://localhost:8080';

// ── Capability Registry ──────────────────────────────────────────────────────

const CAPABILITIES: readonly CapabilityDescriptor[] = [
  {
    id: 'orasaka.core.chat.text',
    flag: '--text',
    argName: 'prompt',
    description: 'Execute standard text chat stream',
    renderKind: 'text',
    responseField: 'content',
  },
  {
    id: 'orasaka.core.media.vision',
    flag: '--image',
    argName: 'path',
    description: 'Provide poster image for vision analysis',
    renderKind: 'image',
    responseField: 'analysis',
    processExtraParams: async (filePath: string) => {
      if (!fs.existsSync(filePath)) {
        throw new Error(`Image file not found at ${filePath}`);
      }
      const base64 = fs.readFileSync(filePath).toString('base64');
      return { posterBase64: base64 };
    },
  },
  {
    id: 'orasaka.core.media.audio',
    flag: '--audio',
    argName: 'path',
    description: 'Provide audio clip path for analysis',
    renderKind: 'audio',
    responseField: 'analysis',
    processExtraParams: async (filePath: string, conversationId?: string) => {
      if (!fs.existsSync(filePath)) {
        throw new Error(`Audio file not found at ${filePath}`);
      }
      const base64 = fs.readFileSync(filePath).toString('base64');
      return { audioBase64: base64, threadId: conversationId || '' };
    },
  },
  {
    id: 'orasaka.core.chat.image',
    flag: '--gen-image',
    argName: 'prompt',
    description: 'Generate image from prompt',
    renderKind: 'image',
    responseField: 'content',
  },
  {
    id: 'orasaka.core.chat.speech',
    flag: '--speech',
    argName: 'text',
    description: 'Convert text to speech',
    renderKind: 'audio',
    responseField: 'content',
  },
];

// ── Parsers ──────────────────────────────────────────────────────────────────

function parseChatInput(args: string[]): ChatInput & { savePath?: string } {
  let flag: string | undefined;
  let flagValue: string | undefined;
  let savePath: string | undefined;
  const promptParts: string[] = [];

  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    if (arg === '--save' && args[i + 1]) {
      savePath = args[++i];
      continue;
    }
    const matchingCap = CAPABILITIES.find((c) => c.flag === arg);
    if (matchingCap) {
      flag = arg;
      flagValue = args[++i];
    } else {
      promptParts.push(arg);
    }
  }

  return { flag, flagValue, prompt: promptParts.join(' ').trim(), savePath };
}

// ── SSE Streaming ────────────────────────────────────────────────────────────

async function streamResponse(
  prompt: string,
  node: OperationNode,
  conversationId: string,
  token: string,
): Promise<void> {
  const url = `${GATEWAY_URL}${node.executionDetails.uriPath}/${conversationId}?prompt=${encodeURIComponent(prompt)}`;
  let accumulated = '';

  try {
    const response = await fetch(url, {
      headers: { Authorization: `Bearer ${token}` },
    });

    if (!response.ok) {
      console.error(`\x1b[31m\n[Stream Error: ${response.statusText}]\x1b[0m`);
      return;
    }
    if (!response.body) return;

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      for (const line of lines) {
        const trimmed = line.trim();
        if (trimmed.startsWith('data:')) {
          const jsonStr = trimmed.substring(5).trim();
          try {
            const parsed = JSON.parse(jsonStr);
            if (parsed.content) {
              process.stdout.write(parsed.content);
              accumulated += parsed.content;
            }
          } catch {
            process.stdout.write(jsonStr);
            accumulated += jsonStr;
          }
        }
      }
    }

    // Persist the streamed response to thread history
    if (accumulated) {
      appendMessage(conversationId, {
        role: 'assistant',
        content: accumulated,
        kind: 'text',
        timestamp: Date.now(),
      });
    }
  } catch (error: any) {
    console.error(`\x1b[31m\n[Connection Error: ${error.message}]\x1b[0m`);
  }
}

// ── Capability Execution ─────────────────────────────────────────────────────

async function executeViaGraphQL(
  cap: CapabilityDescriptor,
  prompt: string,
  token: string,
  conversationId: string,
  savePath?: string,
): Promise<void> {
  const client = new CliClient(`${GATEWAY_URL}/graphql`, token);

  if (cap.flag === '--gen-image') {
    const res = await client.generateImage(prompt);
    renderTimeline({ kind: 'image', content: res.content }, savePath);
    appendMessage(conversationId, { role: 'assistant', content: res.content, kind: 'image', timestamp: Date.now() });
    return;
  }

  if (cap.flag === '--speech') {
    const res = await client.generateSpeech(prompt);
    renderTimeline({ kind: 'audio', content: res.content }, savePath);
    appendMessage(conversationId, { role: 'assistant', content: res.content, kind: 'audio', timestamp: Date.now() });
    return;
  }
}

async function executeViaSdui(
  node: OperationNode,
  prompt: string,
  token: string,
  conversationId: string,
  cap: CapabilityDescriptor,
  extraParams: Record<string, string>,
  savePath?: string,
): Promise<void> {
  const { uriPath, httpMethod, payloadTemplate } = node.executionDetails;
  const url = `${GATEWAY_URL}${uriPath}`;

  let bodyObj: any;
  if (payloadTemplate) {
    let payload = payloadTemplate;
    const allParams = { prompt, ...extraParams };
    for (const [key, val] of Object.entries(allParams)) {
      payload = payload.replaceAll(`\${${key}}`, val);
    }
    bodyObj = JSON.parse(payload);
  }

  const response = await fetch(url, {
    method: httpMethod,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: bodyObj ? JSON.stringify(bodyObj) : undefined,
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status} ${response.statusText}`);
  }

  const res = await response.json();
  const content = res[cap.responseField];
  renderTimeline({ kind: cap.renderKind, content }, savePath);
  appendMessage(conversationId, { role: 'assistant', content, kind: cap.renderKind, timestamp: Date.now() });
}

async function handleChatExecution(
  parsed: ChatInput & { savePath?: string },
  conversationId: string,
  token: string,
  nodes: OperationNode[],
): Promise<void> {
  // Persist user message
  if (parsed.prompt) {
    appendMessage(conversationId, { role: 'user', content: parsed.prompt, kind: 'text', timestamp: Date.now() });
  }

  if (parsed.flag) {
    const cap = CAPABILITIES.find((c) => c.flag === parsed.flag);
    if (!cap) throw new Error(`Unknown option flag: ${parsed.flag}`);

    // Use GraphQL mutations directly for image and speech
    if (cap.flag === '--gen-image' || cap.flag === '--speech') {
      const prompt = cap.flag === '--gen-image' ? (parsed.flagValue || parsed.prompt) : (parsed.flagValue || parsed.prompt);
      await executeViaGraphQL(cap, prompt, token, conversationId, parsed.savePath);
      return;
    }

    // Use SDUI REST for media analysis capabilities
    const node = nodes.find((n) => n.id === cap.id);
    if (!node) throw new Error(`Capability "${cap.id}" not found in operation graph.`);

    if (node.state.type === 'LOCKED') {
      throw new Error(`Capability "${node.label}" is locked. Reason: ${(node.state as any).reason}`);
    }
    if (node.state.type === 'INVISIBLE') {
      throw new Error(`Capability "${node.label}" is disabled.`);
    }

    const extraParams = cap.processExtraParams && parsed.flagValue
      ? await cap.processExtraParams(parsed.flagValue, conversationId)
      : {};

    await executeViaSdui(node, parsed.prompt, token, conversationId, cap, extraParams, parsed.savePath);
  } else {
    // Default: text chat via SSE streaming
    const textCapNode = nodes.find((n) => n.id === 'orasaka.core.chat.text');
    if (!textCapNode) throw new Error('Text chat capability not found in operation graph.');

    if (textCapNode.state.type === 'ACTIVE') {
      await streamResponse(parsed.prompt, textCapNode, conversationId, token);
    } else if (textCapNode.state.type === 'LOCKED') {
      console.error(`\x1b[31mError: Text Chat is locked. Reason: ${(textCapNode.state as any).reason}\x1b[0m`);
    } else {
      console.error(`\x1b[31mError: Text Chat is currently disabled.\x1b[0m`);
    }
  }
}

// ── Thread Meta-Commands ─────────────────────────────────────────────────────

function handleThreadCommand(input: string, config: CliConfig): string {
  const parts = input.trim().split(/\s+/);
  const subCmd = parts[1];

  if (subCmd === 'new') {
    const thread = createThread();
    console.log(`\x1b[32m✓ Created new thread: ${thread.conversationId}\x1b[0m`);
    return thread.conversationId;
  }

  if (subCmd === 'list') {
    const threads = listThreads();
    if (threads.length === 0) {
      console.log('\x1b[90mNo threads found.\x1b[0m');
    } else {
      console.log(`\n\x1b[1m\x1b[36m--- Conversation Threads ---\x1b[0m`);
      for (const t of threads) {
        const active = t.conversationId === config.activeThreadId ? ' \x1b[32m← active\x1b[0m' : '';
        const date = new Date(t.updatedAt).toLocaleString();
        console.log(`  \x1b[33m${t.conversationId.substring(0, 8)}\x1b[0m  ${t.title.padEnd(30)}  \x1b[90m${date}\x1b[0m${active}`);
      }
      console.log();
    }
    return config.activeThreadId;
  }

  if (subCmd === 'switch' && parts[2]) {
    const threadId = parts[2];
    const threads = listThreads();
    const match = threads.find((t) => t.conversationId.startsWith(threadId));
    if (!match) {
      console.error(`\x1b[31mThread not found: ${threadId}\x1b[0m`);
      return config.activeThreadId;
    }
    switchThread(match.conversationId);
    console.log(`\x1b[32m✓ Switched to thread: ${match.conversationId}\x1b[0m`);
    return match.conversationId;
  }

  console.log(`\x1b[33mThread commands: /thread new | /thread list | /thread switch <id>\x1b[0m`);
  return config.activeThreadId;
}

// ── Main Handler ─────────────────────────────────────────────────────────────

/**
 * Handles the `chat` command in both single-shot and interactive modes.
 *
 * @param args - CLI arguments.
 */
export async function handleChat(args: string[]): Promise<void> {
  const config = requireAuth();
  const conversationId = config.activeThreadId;

  let nodes: OperationNode[];
  try {
    const client = new CliClient(`${GATEWAY_URL}/graphql`, config.token);
    nodes = await client.getOperationGraph();
  } catch (e: any) {
    console.error(`\x1b[31mFailed to load operations graph: ${e.message}\x1b[0m`);
    process.exit(1);
    return;
  }

  // Single-shot mode
  const initialArgs = parseChatInput(args);
  if (initialArgs.prompt || initialArgs.flag || initialArgs.flagValue) {
    try {
      await handleChatExecution(initialArgs, conversationId, config.token, nodes);
      console.log();
    } catch (e: any) {
      console.error(`\x1b[31mExecution Failed: ${e.message}\x1b[0m`);
    }
    return;
  }

  // Interactive mode
  let activeConversationId = conversationId;
  console.log(`\x1b[36mConnected to conversation thread: ${activeConversationId.substring(0, 8)}...\x1b[0m`);
  console.log('\x1b[32mInteractive Chat Session Started.\x1b[0m');
  console.log('\x1b[90mType "exit" to leave. Use /thread new|list|switch <id> to manage threads.\x1b[0m\n');

  while (true) {
    const input = await promptInput('\x1b[1m\x1b[32mOrasaka > \x1b[0m');

    if (input.toLowerCase() === 'exit' || input.toLowerCase() === 'quit') {
      console.log('\x1b[36mExited chat session.\x1b[0m');
      break;
    }

    if (!input) continue;

    // Thread meta-commands
    if (input.startsWith('/thread')) {
      const currentConfig = loadConfig()!;
      activeConversationId = handleThreadCommand(input, currentConfig);
      continue;
    }

    const replArgs = input.split(' ');
    const parsed = parseChatInput(replArgs);

    try {
      await handleChatExecution(parsed, activeConversationId, config.token, nodes);
      // Update thread title with first prompt
      updateThreadTitle(
        activeConversationId,
        parsed.prompt.length > 40 ? `${parsed.prompt.substring(0, 40)}...` : parsed.prompt,
      );
    } catch (e: any) {
      console.error(`\x1b[31mExecution Failed: ${e.message}\x1b[0m`);
    }
    console.log();
  }
}
