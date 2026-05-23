import { GraphQLClient, gql } from 'graphql-request';
import * as fs from 'fs';
import * as path from 'path';
import * as os from 'os';
import * as readline from 'readline';

const CONFIG_PATH = path.join(os.homedir(), '.orasaka-cli.json');
const GATEWAY_URL = process.env.GATEWAY_URL || 'http://localhost:' + '8080';

interface CliConfig {
    token: string;
    username: string;
    conversationId?: string;
}

function saveConfig(config: CliConfig) {
    fs.writeFileSync(CONFIG_PATH, JSON.stringify(config, null, 2), 'utf-8');
}

function loadConfig(): CliConfig | null {
    if (!fs.existsSync(CONFIG_PATH)) return null;
    try {
        return JSON.parse(fs.readFileSync(CONFIG_PATH, 'utf-8'));
    } catch {
        return null;
    }
}

function printUsage() {
    console.log(`
\x1b[1m\x1b[36m🥷 ORASAKA COMMAND-LINE INTERFACE (CLI)\x1b[0m
Usage:
  \x1b[32mnpx ts-node src/index.ts <command> [args]\x1b[0m

Commands:
  \x1b[33mlogin [email] [password]\x1b[0m      Authenticate session and cache user token.
  \x1b[33mchat [prompt]\x1b[0m                 Stream responses from AI. Starts interactive REPL if prompt is omitted.
  \x1b[33msettings get\x1b[0m                  View current user profile preferences.
  \x1b[33msettings set <key> <value>\x1b[0m    Update preference property (e.g. language, aiPersona).
`);
}

async function promptInput(query: string, secret = false): Promise<string> {
    const rl = readline.createInterface({
        input: process.stdin,
        output: process.stdout
    });

    return new Promise((resolve) => {
        rl.question(query, (answer: string) => {
            rl.close();
            resolve(answer.trim());
        });
        if (secret) {
            // Mask password typing
            (rl as any)._writeToOutput = (stringToWrite: string) => {
                if (stringToWrite === '\r' || stringToWrite === '\n' || stringToWrite === '\r\n') {
                    process.stdout.write(stringToWrite);
                } else {
                    process.stdout.write('*');
                }
            };
        }
    });
}

async function handleLogin(args: string[]) {
    let email = args[0];
    let password = args[1];

    if (!email) {
        email = await promptInput('\x1b[1mEnter Email: \x1b[0m');
    }
    if (!password) {
        password = await promptInput('\x1b[1mEnter Password: \x1b[0m', true);
        console.log(); // print newline after password mask
    }

    if (!email || !password) {
        console.error('\x1b[31mError: Email and password are required.\x1b[0m');
        process.exit(1);
        return;
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

        const data = await response.json() as { token: string; username: string };
        const conversationId = crypto.randomUUID();

        saveConfig({
            token: data.token,
            username: data.username,
            conversationId
        });

        console.log(`\x1b[32m✓ Login successful! Token cached for user: ${data.username}\x1b[0m`);
    } catch (error: any) {
        console.error(`\x1b[31mAuthentication Failed: ${error.message}\x1b[0m`);
        process.exit(1);
    }
}

async function handleChat(args: string[]) {
    const config = loadConfig();
    if (!config || !config.token) {
        console.error('\x1b[31mError: Unauthorized. Please run "login" command first.\x1b[0m');
        process.exit(1);
        return;
    }

    const conversationId = config.conversationId || crypto.randomUUID();
    if (!config.conversationId) {
        saveConfig({
            token: config.token,
            username: config.username,
            conversationId
        });
    }

    const initialPrompt = args.join(' ');
    if (initialPrompt) {
        await streamResponse(initialPrompt, conversationId, config.token);
        console.log();
        return;
    }

    // Start Interactive REPL Loop
    console.log(`\x1b[36mConnected to conversation thread: ${conversationId}\x1b[0m`);
    console.log('\x1b[32mInteractive Chat Session Started. Type "exit" or "quit" to leave.\x1b[0m\n');

    while (true) {
        const input = await promptInput('\x1b[1m\x1b[32mOrasaka > \x1b[0m');
        if (input.toLowerCase() === 'exit' || input.toLowerCase() === 'quit') {
            console.log('\x1b[36mExited chat session.\x1b[0m');
            break;
        }
        if (!input) continue;

        process.stdout.write('\x1b[36mAgent > \x1b[0m');
        await streamResponse(input, conversationId, config.token);
        console.log('\n');
    }
}

async function streamResponse(prompt: string, conversationId: string, token: string) {
    const url = `${GATEWAY_URL}/api/v1/chat/stream/${conversationId}?prompt=${encodeURIComponent(prompt)}`;
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
                        // Fallback in case raw SSE payload is text
                        process.stdout.write(jsonStr);
                    }
                }
            }
        }
    } catch (error: any) {
        console.error(`\x1b[31m\n[Connection Error: ${error.message}]\x1b[0m`);
    }
}

async function handleSettings(args: string[]) {
    const config = loadConfig();
    if (!config || !config.token) {
        console.error('\x1b[31mError: Unauthorized. Please run "login" command first.\x1b[0m');
        process.exit(1);
        return;
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
            Object.keys(prefs).forEach(k => {
                console.log(`  \x1b[32m${k}:\x1b[0m ${prefs[k]}`);
            });
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
            return;
        }

        let typedValue: any = rawValue;
        if (rawValue.toLowerCase() === 'true') typedValue = true;
        else if (rawValue.toLowerCase() === 'false') typedValue = false;
        else if (!isNaN(Number(rawValue))) typedValue = Number(rawValue);

        try {
            // First fetch current prefs to preserve them
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
        printUsage();
    }
}

async function main() {
    const [, , command, ...args] = process.argv;

    if (!command) {
        printUsage();
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
            printUsage();
            break;
    }
}

main().catch(console.error);
