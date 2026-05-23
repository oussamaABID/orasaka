import { GraphQLClient, gql } from 'graphql-request';
import * as fs from 'fs';
import * as path from 'path';
import * as os from 'os';
import * as readline from 'readline';
import * as crypto from 'crypto';
import { OrasakaCliClient } from './client';

const CONFIG_PATH = path.join(os.homedir(), '.orasaka-cli.json');
const GATEWAY_URL = process.env.GATEWAY_URL || 'http://localhost:8080';

type NodeStateType = 'ACTIVE' | 'LOCKED' | 'INVISIBLE';

interface BaseNodeState {
    type: NodeStateType;
}

interface ActiveNodeState extends BaseNodeState {
    type: 'ACTIVE';
}

interface LockedNodeState extends BaseNodeState {
    type: 'LOCKED';
    reason: string;
    lockedAt?: string;
}

interface InvisibleNodeState extends BaseNodeState {
    type: 'INVISIBLE';
}

type NodeState = ActiveNodeState | LockedNodeState | InvisibleNodeState;

interface TargetExecutionUri {
    uriPath: string;
    httpMethod: string;
    payloadTemplate?: string;
}

interface OperationNode {
    id: string;
    label: string;
    icon: string;
    presentationContext: string;
    state: NodeState;
    executionDetails: TargetExecutionUri;
}

interface CliConfig {
    readonly token: string;
    readonly username: string;
    readonly conversationId: string;
}

interface TextTimelineMessage {
    readonly kind: 'text';
    readonly content: string;
}

interface ImageTimelineMessage {
    readonly kind: 'image';
    readonly content: string;
}

interface AudioTimelineMessage {
    readonly kind: 'audio';
    readonly content: string;
}

type TimelineMessage = TextTimelineMessage | ImageTimelineMessage | AudioTimelineMessage;

interface ChatInput {
    readonly flag?: string;
    readonly flagValue?: string;
    readonly prompt: string;
}

interface CapabilityDescriptor {
    readonly id: string;
    readonly flag: string;
    readonly argName: string;
    readonly description: string;
    readonly renderKind: 'text' | 'image' | 'audio';
    readonly responseField: 'content' | 'analysis';
    readonly processExtraParams?: (val: string, conversationId?: string) => Promise<Record<string, string>> | Record<string, string>;
}

const CAPABILITIES: readonly CapabilityDescriptor[] = [
    {
        id: 'orasaka.core.chat.text',
        flag: '--text',
        argName: 'prompt',
        description: 'Execute standard text chat stream',
        renderKind: 'text',
        responseField: 'content'
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
        }
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
            return {
                audioBase64: base64,
                threadId: conversationId || ''
            };
        }
    },
    {
        id: 'orasaka.core.chat.image',
        flag: '--gen-image',
        argName: 'prompt',
        description: 'Generate image from prompt',
        renderKind: 'image',
        responseField: 'content'
    },
    {
        id: 'orasaka.core.chat.speech',
        flag: '--speech',
        argName: 'text',
        description: 'Convert text to speech',
        renderKind: 'audio',
        responseField: 'content'
    }
];

function saveConfig(config: CliConfig): void {
    fs.writeFileSync(CONFIG_PATH, JSON.stringify(config, null, 2), 'utf-8');
}

function loadConfig(): CliConfig | null {
    if (!fs.existsSync(CONFIG_PATH)) {
        return null;
    }
    try {
        const parsed = JSON.parse(fs.readFileSync(CONFIG_PATH, 'utf-8'));
        if (parsed && typeof parsed.token === 'string' && typeof parsed.username === 'string') {
            return {
                token: parsed.token,
                username: parsed.username,
                conversationId: parsed.conversationId || crypto.randomUUID()
            };
        }
        return null;
    } catch {
        return null;
    }
}

async function promptInput(query: string, secret = false): Promise<string> {
    const rl = readline.createInterface({
        input: process.stdin,
        output: process.stdout
    });

    return new Promise((resolve) => {
        if (secret) {
            (rl as any)._writeToOutput = (stringToWrite: string) => {
                const isNewline = stringToWrite === '\r' || stringToWrite === '\n' || stringToWrite === '\r\n';
                process.stdout.write(isNewline ? stringToWrite : '*');
            };
        }
        rl.question(query, (answer: string) => {
            rl.close();
            resolve(answer.trim());
        });
    });
}

async function fetchOperationGraph(token: string): Promise<OperationNode[]> {
    const client = new OrasakaCliClient(`${GATEWAY_URL}/graphql`, token);
    return client.getOperationGraph();
}

async function fetchGraphAndPrintMenu(token?: string): Promise<void> {
    if (!token) {
        console.log(`  \x1b[33mlogin [email] [password]\x1b[0m      Authenticate session and cache user token.`);
        return;
    }
    try {
        const nodes = await fetchOperationGraph(token);
        console.log(`\x1b[1m\x1b[36m--- DYNAMIC CAPABILITY MATRIX ---\x1b[0m`);
        for (const cap of CAPABILITIES) {
            const node = nodes.find(n => n.id === cap.id);
            if (!node) continue;
            switch (node.state.type) {
                case 'ACTIVE':
                    console.log(`  \x1b[32mActive\x1b[0m    - \x1b[1m${cap.flag} <${cap.argName}>\x1b[0m: ${node.label} (${cap.description})`);
                    break;
                case 'LOCKED':
                    console.log(`  \x1b[90mLocked    - ${cap.flag} <${cap.argName}>: ${node.label} (Reason: ${node.state.reason})\x1b[0m`);
                    break;
                case 'INVISIBLE':
                    break;
                default:
                    const _exhaustive: never = node.state;
                    return _exhaustive;
            }
        }
        console.log();
    } catch (e) {
        console.log(`\x1b[31mFailed to retrieve dynamic operations graph from server.\x1b[0m`);
    }
}

async function printDynamicUsage(): Promise<void> {
    console.log(`
\x1b[1m\x1b[36m🥷 ORASAKA COMMAND-LINE INTERFACE (CLI)\x1b[0m
Usage:
  \x1b[32mnpx ts-node src/index.ts <command> [args]\x1b[0m

Commands:
  \x1b[33mlogin [email] [password]\x1b[0m      Authenticate session and cache user token.
  \x1b[33mchat [prompt] [options]\x1b[0m       Execute dynamic workspace chat capabilities.
  \x1b[33msettings get\x1b[0m                  View current user profile preferences.
  \x1b[33msettings set <key> <value>\x1b[0m    Update preference property (e.g. language, aiPersona).
`);
    const config = loadConfig();
    await fetchGraphAndPrintMenu(config?.token);
}

async function handleLogin(args: string[]): Promise<void> {
    const emailArg = args[0];
    const passwordArg = args[1];
    const email = emailArg || (await promptInput('\x1b[1mEnter Email: \x1b[0m'));
    const password = passwordArg || (await promptInput('\x1b[1mEnter Password: \x1b[0m', true));
    if (!email || !password) {
        console.error('\x1b[31mError: Email and password are required.\x1b[0m');
        process.exit(1);
    }
    console.log(`\x1b[36mAuthenticating user "${email}" against ${GATEWAY_URL}...\x1b[0m`);
    try {
        const response = await fetch(`${GATEWAY_URL}/api/v1/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        if (!response.ok) {
            const errBody = (await response.json().catch(() => ({}))) as any;
            throw new Error(errBody.error || `HTTP ${response.status}`);
        }
        const data = (await response.json()) as { token: string; username: string };
        saveConfig({
            token: data.token,
            username: data.username,
            conversationId: crypto.randomUUID()
        });
        console.log(`\x1b[32m✓ Login successful! Token cached for user: ${data.username}\x1b[0m`);
    } catch (error: any) {
        console.error(`\x1b[31mAuthentication Failed: ${error.message}\x1b[0m`);
        process.exit(1);
    }
}

function parseChatInput(args: string[]): ChatInput {
    let flag: string | undefined = undefined;
    let flagValue: string | undefined = undefined;
    const promptParts: string[] = [];
    for (let i = 0; i < args.length; i++) {
        const arg = args[i];
        const matchingCap = CAPABILITIES.find(c => c.flag === arg);
        if (matchingCap) {
            flag = arg;
            flagValue = args[++i];
        } else {
            promptParts.push(arg);
        }
    }
    return {
        flag,
        flagValue,
        prompt: promptParts.join(' ').trim()
    };
}

function renderText(content: string): void {
    console.log(content);
}

function renderImageFrame(content: string): void {
    console.log(`\x1b[35m┌────────────────────────────────────────────────────────┐\x1b[0m`);
    console.log(`\x1b[35m│ [IMAGE OUT]                                            │\x1b[0m`);
    console.log(`\x1b[35m├────────────────────────────────────────────────────────┤\x1b[0m`);
    const lines = content.split('\n');
    for (const line of lines) {
        console.log(`\x1b[35m│\x1b[0m ${line.padEnd(54).substring(0, 54)} \x1b[35m│\x1b[0m`);
    }
    console.log(`\x1b[35m└────────────────────────────────────────────────────────┘\x1b[0m`);
}

function renderAudioIndicator(content: string): void {
    console.log(`\x1b[33m🔊 [AUDIO OUT] -> Asset Track link resolved:\x1b[0m \x1b[4m${content}\x1b[0m`);
}

function renderTimeline(message: TimelineMessage): void {
    switch (message.kind) {
        case 'text':
            renderText(message.content);
            break;
        case 'image':
            renderImageFrame(message.content);
            break;
        case 'audio':
            renderAudioIndicator(message.content);
            break;
        default:
            const _exhaustiveCheck: never = message;
            return _exhaustiveCheck;
    }
}

async function executeCapability(
    node: OperationNode,
    prompt: string,
    token: string,
    extraParams: Record<string, string> = {}
): Promise<any> {
    const { uriPath, httpMethod, payloadTemplate } = node.executionDetails;
    const url = `${GATEWAY_URL}${uriPath}`;
    let bodyObj: any = undefined;
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
            'Authorization': `Bearer ${token}`
        },
        body: bodyObj ? JSON.stringify(bodyObj) : undefined
    });
    if (!response.ok) {
        throw new Error(`HTTP ${response.status} ${response.statusText}`);
    }
    return response.json();
}

async function runCapability(
    cap: CapabilityDescriptor,
    nodes: OperationNode[],
    prompt: string,
    token: string,
    flagValue?: string,
    conversationId?: string
): Promise<void> {
    const node = nodes.find(n => n.id === cap.id);
    if (!node) {
        throw new Error(`Capability "${cap.id}" not found in operation graph.`);
    }
    switch (node.state.type) {
        case 'ACTIVE': {
            const extraParams =
                cap.processExtraParams && flagValue ? await cap.processExtraParams(flagValue, conversationId) : {};
            const res = await executeCapability(node, prompt, token, extraParams);
            const content = res[cap.responseField];
            renderTimeline({ kind: cap.renderKind, content });
            break;
        }
        case 'LOCKED':
            throw new Error(`Capability "${node.label}" is locked. Reason: ${node.state.reason}`);
        case 'INVISIBLE':
            throw new Error(`Capability "${node.label}" is disabled/invisible.`);
        default:
            const _exhaustive: never = node.state;
            return _exhaustive;
    }
}

async function streamResponse(
    prompt: string,
    node: OperationNode,
    conversationId: string,
    token: string
): Promise<void> {
    const url = `${GATEWAY_URL}${node.executionDetails.uriPath}/${conversationId}?prompt=${encodeURIComponent(prompt)}`;
    try {
        const response = await fetch(url, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
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
                        }
                    } catch {
                        process.stdout.write(jsonStr);
                    }
                }
            }
        }
    } catch (error: any) {
        console.error(`\x1b[31m\n[Connection Error: ${error.message}]\x1b[0m`);
    }
}

async function handleChatExecution(
    parsed: ChatInput,
    conversationId: string,
    token: string,
    nodes: OperationNode[]
): Promise<void> {
    if (parsed.flag) {
        const cap = CAPABILITIES.find(c => c.flag === parsed.flag);
        if (!cap) {
            throw new Error(`Unknown option flag: ${parsed.flag}`);
        }
        await runCapability(cap, nodes, parsed.prompt, token, parsed.flagValue, conversationId);
    } else {
        const textCapNode = nodes.find(n => n.id === 'orasaka.core.chat.text');
        if (!textCapNode) {
            throw new Error('Text chat capability not found in operation graph.');
        }
        switch (textCapNode.state.type) {
            case 'ACTIVE':
                await streamResponse(parsed.prompt, textCapNode, conversationId, token);
                break;
            case 'LOCKED':
                console.error(`\x1b[31mError: Text Chat is locked. Reason: ${textCapNode.state.reason}\x1b[0m`);
                break;
            case 'INVISIBLE':
                console.error(`\x1b[31mError: Text Chat is currently disabled.\x1b[0m`);
                break;
            default:
                const _exhaustive: never = textCapNode.state;
                return _exhaustive;
        }
    }
}

async function handleChat(args: string[]): Promise<void> {
    const config = loadConfig();
    if (!config || !config.token) {
        console.error('\x1b[31mError: Unauthorized. Please run "login" command first.\x1b[0m');
        process.exit(1);
    }
    const conversationId = config.conversationId;
    let nodes: OperationNode[];
    try {
        nodes = await fetchOperationGraph(config.token);
    } catch (e: any) {
        console.error(`\x1b[31mFailed to load operations graph: ${e.message}\x1b[0m`);
        process.exit(1);
        return;
    }
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
    console.log(`\x1b[36mConnected to conversation thread: ${conversationId}\x1b[0m`);
    console.log('\x1b[32mInteractive Chat Session Started. Type "exit" or "quit" to leave.\x1b[0m\n');
    while (true) {
        const input = await promptInput('\x1b[1m\x1b[32mOrasaka > \x1b[0m');
        if (input.toLowerCase() === 'exit' || input.toLowerCase() === 'quit') {
            console.log('\x1b[36mExited chat session.\x1b[0m');
            break;
        }
        if (!input) continue;
        const replArgs = input.split(' ');
        const parsed = parseChatInput(replArgs);
        try {
            await handleChatExecution(parsed, conversationId, config.token, nodes);
        } catch (e: any) {
            console.error(`\x1b[31mExecution Failed: ${e.message}\x1b[0m`);
        }
        console.log();
    }
}

async function handleSettings(args: string[]): Promise<void> {
    const config = loadConfig();
    if (!config || !config.token) {
        console.error('\x1b[31mError: Unauthorized. Please run "login" command first.\x1b[0m');
        process.exit(1);
    }
    const client = new GraphQLClient(`${GATEWAY_URL}/graphql`, {
        headers: {
            'Authorization': `Bearer ${config.token}`
        }
    });
    const subCommand = args[0];
    if (subCommand === 'get') {
        const query = gql`
            query GetMe {
                me {
                    id
                    username
                    email
                    authorities
                    preferences
                }
            }
        `;
        try {
            const data: any = await client.request(query);
            console.log(`\n\x1b[1m\x1b[36m--- Orasaka Settings for User: ${data.me.username} ---\x1b[0m`);
            console.log(`\x1b[33mUser ID:\x1b[0m     ${data.me.id}`);
            console.log(`\x1b[33mEmail:\x1b[0m       ${data.me.email}`);
            console.log(`\x1b[33mAuthorities:\x1b[0m ${data.me.authorities ? data.me.authorities.join(', ') : 'None'}`);
            console.log(`\x1b[33mPreferences:\x1b[0m`);
            const prefs = data.me.preferences || {};
            for (const [k, v] of Object.entries(prefs)) {
                console.log(`  \x1b[32m${k}:\x1b[0m ${v}`);
            }
            console.log();
        } catch (error: any) {
            console.error(`\x1b[31mFailed to retrieve settings: ${error.message}\x1b[0m`);
        }
    } else if (subCommand === 'set') {
        const key = args[1];
        const rawValue = args[2];
        if (!key || rawValue === undefined) {
            console.error('\x1b[31mUsage: settings set <key> <value>\x1b[0m');
            process.exit(1);
        }
        const typedValue = rawValue.toLowerCase() === 'true'
            ? true
            : rawValue.toLowerCase() === 'false'
            ? false
            : !isNaN(Number(rawValue))
            ? Number(rawValue)
            : rawValue;
        try {
            const getQuery = gql`
                query GetMe {
                    me {
                        preferences
                    }
                }
            `;
            const getData: any = await client.request(getQuery);
            const currentPrefs = getData.me?.preferences || {};
            const newPrefs = { ...currentPrefs, [key]: typedValue };
            const mutation = gql`
                mutation UpdatePrefs($prefs: Map!) {
                    updatePreferences(preferences: $prefs) {
                        preferences
                    }
                }
            `;
            await client.request(mutation, { prefs: newPrefs });
            console.log(`\x1b[32m✓ Preference "${key}" updated to: ${rawValue}\x1b[0m`);
        } catch (error: any) {
            console.error(`\x1b[31mFailed to update preference: ${error.message}\x1b[0m`);
        }
    } else {
        console.error('\x1b[31mInvalid settings command. Use "get" or "set".\x1b[0m');
        await printDynamicUsage();
    }
}

async function main(): Promise<void> {
    const [, , command, ...args] = process.argv;
    if (!command) {
        await printDynamicUsage();
        process.exit(0);
    }
    switch (command.toLowerCase()) {
        case 'login':
            await handleLogin(args);
            break;
        case 'chat':
            await handleChat(args);
            break;
        case 'settings':
            await handleSettings(args);
            break;
        case 'help':
        default:
            await printDynamicUsage();
            break;
    }
}

main().catch(console.error);
