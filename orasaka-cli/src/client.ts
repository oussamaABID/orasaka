/**
 * @file client.ts
 * @module orasaka-cli/client
 * @description Unified GraphQL client for all Orasaka gateway operations.
 *
 * This module provides the {@link CliClient} class — the single facade
 * through which the CLI communicates with the `orasaka-gateway` GraphQL API.
 * It exposes typed methods for:
 *
 * - **Queries**: `getMe`, `getOperationGraph`, `getInterceptionSchema`
 * - **Mutations**: `chat`, `generateImage`, `generateSpeech`, `register`,
 *   `updatePreferences`, `resolveInterception`
 * - **Subscriptions**: `chatStream` (via WebSocket using `graphql-ws`)
 *
 * The client uses Node 18+'s native `fetch()` for HTTP transport, avoiding
 * the ESM-only `graphql-request` library to maintain CJS compatibility.
 * All GraphQL errors are surfaced as thrown `Error` instances.
 *
 * @example
 * ```typescript
 * const client = new CliClient('http://localhost:8080/graphql', jwtToken);
 * const profile = await client.getMe();
 * console.log(profile.username);
 * ```
 */

import type {
  ChatResponse,
  UserProfile,
  RegisterResult,
  OperationNode,
} from './types';

/**
 * Unified GraphQL client for the Orasaka CLI.
 *
 * Communicates directly with the `orasaka-gateway` GraphQL endpoint
 * using JWT Bearer authentication. All methods are typed against
 * the shared interfaces defined in {@link module:orasaka-cli/types}.
 *
 * @remarks
 * This client does **not** use `graphql-request` because that library
 * is ESM-only and incompatible with the CLI's CommonJS module system.
 * Instead, all operations go through a private {@link CliClient.request}
 * method backed by native `fetch()`.
 */
export class CliClient {
  /**
   * Pre-built HTTP headers sent with every GraphQL request.
   * Includes `Content-Type: application/json` and the optional `Authorization` header.
   */
  private headers: Record<string, string>;

  /**
   * Creates a new instance of the Orasaka GraphQL client.
   *
   * @param endpoint - The full URL of the GraphQL endpoint
   *   (e.g., `http://localhost:8080/graphql`). Defaults to `http://localhost:8080/graphql`.
   * @param token - Optional JWT Bearer token. When provided, an `Authorization: Bearer <token>`
   *   header is included in every request. Omit for unauthenticated operations.
   */
  constructor(
    private endpoint: string = 'http://localhost:8080/graphql',
    private token?: string,
  ) {
    this.headers = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    };
  }

  /**
   * Executes a GraphQL request using the native `fetch()` API.
   *
   * This method handles:
   * 1. Serializing the query and variables into a JSON POST body.
   * 2. Checking the HTTP status code for transport-level errors.
   * 3. Parsing the GraphQL response and surfacing the first error (if any).
   *
   * @template T - The expected shape of the `data` field in the GraphQL response.
   * @param query - The GraphQL query or mutation string.
   * @param variables - Optional map of GraphQL variables to substitute into the operation.
   * @returns The parsed `data` field from the GraphQL response.
   * @throws {Error} If the HTTP response status is not OK (2xx).
   * @throws {Error} If the GraphQL response contains one or more errors.
   */
  private async request<T = any>(query: string, variables?: Record<string, unknown>): Promise<T> {
    const response = await fetch(this.endpoint, {
      method: 'POST',
      headers: this.headers,
      body: JSON.stringify({ query, variables }),
    });

    if (!response.ok) {
      throw new Error(`GraphQL request failed: HTTP ${response.status}`);
    }

    const json = (await response.json()) as { data?: T; errors?: Array<{ message: string }> };

    if (json.errors && json.errors.length > 0) {
      throw new Error(json.errors[0].message);
    }

    return json.data as T;
  }

  // ── Queries ──────────────────────────────────────────────────────────────

  /**
   * Fetches the currently authenticated user's profile and preferences.
   *
   * Executes the `me` GraphQL query against the gateway's identity layer.
   * Requires a valid JWT token to be set during construction.
   *
   * @returns The authenticated user's profile data including username,
   *   email, authorities, and preferences.
   * @throws {Error} If the user is not authenticated or the request fails.
   *
   * @example
   * ```typescript
   * const user = await client.getMe();
   * console.log(`Logged in as: ${user.username}`);
   * ```
   */
  async getMe(): Promise<UserProfile> {
    const query = `
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
    const data = await this.request<{ me: UserProfile }>(query);
    return data.me;
  }

  /**
   * Fetches the compiled Operation Graph for SDUI capability rendering.
   *
   * The Operation Graph defines which AI capabilities are currently available,
   * their polymorphic states (Active/Locked/Invisible), and how to invoke them.
   * This is the SDUI contract between the gateway and the CLI.
   *
   * @returns Array of {@link OperationNode} objects with their states
   *   and execution details.
   * @throws {Error} If the user is not authenticated or the request fails.
   *
   * @see {@link OperationNode} for the node structure.
   * @see {@link NodeState} for the polymorphic state variants.
   */
  async getOperationGraph(): Promise<OperationNode[]> {
    const query = `
      query GetOperationGraph {
        operationGraph {
          nodes {
            id
            label
            icon
            presentationContext
            state {
              type
              reason
              lockedAt
            }
            executionDetails {
              uriPath
              httpMethod
              payloadTemplate
            }
          }
        }
      }
    `;
    const data = await this.request<{ operationGraph: { nodes: OperationNode[] } }>(query);
    return data.operationGraph.nodes;
  }

  /**
   * Fetches an interception schema by its unique identifier.
   *
   * Interception schemas define dynamic forms that the engine requires
   * the user to fill out before proceeding (e.g., preference dialogs,
   * consent flows). The returned string is a raw JSON Schema.
   *
   * @param schemaId - The unique schema identifier assigned by the engine.
   * @returns The raw JSON schema string, or `null` if no schema exists for the given ID.
   * @throws {Error} If the request fails.
   */
  async getInterceptionSchema(schemaId: string): Promise<string | null> {
    const query = `
      query GetInterceptionSchema($schemaId: String!) {
        interceptionSchema(schemaId: $schemaId)
      }
    `;
    const data = await this.request<{ interceptionSchema: string | null }>(query, { schemaId });
    return data.interceptionSchema;
  }

  // ── Mutations ────────────────────────────────────────────────────────────

  /**
   * Executes a single-turn chat mutation.
   *
   * Sends a user prompt to the engine for synchronous (non-streaming)
   * processing and returns the full response at once.
   *
   * @param prompt - The user's natural language prompt.
   * @param conversationId - Optional conversation thread ID for session continuity.
   *   If omitted, the engine creates a new anonymous conversation.
   * @returns The {@link ChatResponse} with generated content and conversation ID.
   * @throws {Error} If the request fails or the engine returns an error.
   */
  async chat(prompt: string, conversationId?: string): Promise<ChatResponse> {
    const mutation = `
      mutation Chat($prompt: String!, $conversationId: String) {
        chat(prompt: $prompt, conversationId: $conversationId) {
          content
          conversationId
        }
      }
    `;
    const data = await this.request<{ chat: ChatResponse }>(mutation, { prompt, conversationId });
    return data.chat;
  }

  /**
   * Generates an image via the GraphQL `image` mutation.
   *
   * The engine delegates to the configured image generation provider
   * (e.g., `stable-diffusion.cpp` on port 8085) and returns the result
   * as an RFC 2397 Data URL (`data:image/png;base64,...`).
   *
   * @param prompt - The image generation prompt describing the desired output.
   * @returns A {@link ChatResponse} whose `content` field contains the Data URL.
   * @throws {Error} If the image generation provider is unavailable or returns an error.
   *
   * @example
   * ```typescript
   * const res = await client.generateImage("A cyberpunk cityscape at night");
   * // res.content → "data:image/png;base64,iVBORw0KGgo..."
   * ```
   */
  async generateImage(prompt: string): Promise<ChatResponse> {
    const mutation = `
      mutation GenerateImage($prompt: String!) {
        image(prompt: $prompt) {
          content
          conversationId
          metadata
        }
      }
    `;
    const data = await this.request<{ image: ChatResponse }>(mutation, { prompt });
    return data.image;
  }

  /**
   * Generates speech audio via the GraphQL `speech` mutation.
   *
   * The engine delegates to the configured TTS provider (e.g., OpenAI TTS API)
   * and returns the audio as an RFC 2397 Data URL (`data:audio/mp3;base64,...`).
   *
   * @param text - The text to convert to speech.
   * @returns A {@link ChatResponse} whose `content` field contains the audio Data URL.
   * @throws {Error} If the TTS provider is unavailable or returns an error.
   *
   * @example
   * ```typescript
   * const res = await client.generateSpeech("Hello, world!");
   * // res.content → "data:audio/mp3;base64,SUQzBAAAAAAAI..."
   * ```
   */
  async generateSpeech(text: string): Promise<ChatResponse> {
    const mutation = `
      mutation GenerateSpeech($prompt: String!) {
        speech(prompt: $prompt) {
          content
          conversationId
          metadata
        }
      }
    `;
    const data = await this.request<{ speech: ChatResponse }>(mutation, { prompt: text });
    return data.speech;
  }

  /**
   * Registers a new user via the GraphQL `register` mutation.
   *
   * Creates a new user account in the `orasaka-identity` module.
   * If email verification is enabled, the user must subsequently
   * call `orasaka verify <token>` to activate their account.
   *
   * @param username - The desired username (must be unique).
   * @param email - The email address (must be unique, used for verification).
   * @param password - The plaintext password (hashed server-side via BCrypt).
   * @param language - Optional BCP 47 language tag (e.g., `'en'`, `'fr'`, `'ja'`).
   * @returns A {@link RegisterResult} containing either the created user or an error message.
   * @throws {Error} If the request fails at the transport level.
   */
  async register(
    username: string,
    email: string,
    password: string,
    language?: string,
  ): Promise<RegisterResult> {
    const mutation = `
      mutation Register(
        $username: String!
        $email: String!
        $password: String!
        $language: String
      ) {
        register(
          username: $username
          email: $email
          password: $password
          language: $language
        ) {
          user {
            id
            username
            email
          }
          error
        }
      }
    `;
    const data = await this.request<{ register: RegisterResult }>(mutation, {
      username,
      email,
      password,
      language,
    });
    return data.register;
  }

  /**
   * Updates user preferences via the GraphQL `updatePreferences` mutation.
   *
   * The server performs an **atomic merge** of the provided key-value pairs
   * into the existing preferences map. No prior `GET` is required — this
   * is the ERR-200-compliant path that avoids redundant database roundtrips.
   *
   * @param preferences - The preference key-value pairs to merge.
   *   Existing keys are overwritten; new keys are added; omitted keys are preserved.
   * @returns The full updated {@link UserProfile} after the merge.
   * @throws {Error} If the request fails.
   *
   * @example
   * ```typescript
   * // Set theme to dark without fetching current preferences first
   * const updated = await client.updatePreferences({ theme: 'dark' });
   * ```
   */
  async updatePreferences(
    preferences: Record<string, unknown>,
  ): Promise<UserProfile> {
    const mutation = `
      mutation UpdatePrefs($prefs: Map!) {
        updatePreferences(preferences: $prefs) {
          id
          username
          email
          authorities
          preferences
        }
      }
    `;
    const data = await this.request<{ updatePreferences: UserProfile }>(mutation, { prefs: preferences });
    return data.updatePreferences;
  }

  /**
   * Resolves an active user interception by submitting form responses.
   *
   * Interceptions are dynamic forms triggered by the engine's pipeline
   * (e.g., consent dialogs, preference collection). The CLI resolves
   * them by submitting the user's responses against the interception schema.
   *
   * @param interceptionType - The interception type identifier (e.g., `'CONSENT'`, `'PREFERENCE'`).
   * @param schemaId - The schema identifier that defines the form structure.
   * @param responses - The user's responses as a key-value map matching the schema fields.
   * @returns `true` if the interception was successfully resolved.
   * @throws {Error} If the schema ID is invalid or the responses don't match the schema.
   */
  async resolveInterception(
    interceptionType: string,
    schemaId: string,
    responses: Record<string, unknown>,
  ): Promise<boolean> {
    const mutation = `
      mutation ResolveInterception(
        $interceptionType: String!
        $schemaId: String!
        $responses: Map!
      ) {
        resolveInterception(
          interceptionType: $interceptionType
          schemaId: $schemaId
          responses: $responses
        )
      }
    `;
    const data = await this.request<{ resolveInterception: boolean }>(mutation, {
      interceptionType,
      schemaId,
      responses,
    });
    return data.resolveInterception;
  }

  // ── Subscriptions ────────────────────────────────────────────────────────

  /**
   * Opens a WebSocket subscription for streaming chat tokens in real-time.
   *
   * Uses the `graphql-ws` library (dynamically imported to avoid CJS/ESM
   * issues at module load time) and the `ws` WebSocket implementation.
   * The WebSocket URL is derived by replacing the `http` scheme of the
   * endpoint with `ws`.
   *
   * @param prompt - The user's natural language prompt.
   * @param conversationId - Optional conversation thread ID for session continuity.
   * @param onNext - Callback invoked for each streamed {@link ChatResponse} token.
   *   Receives the `chatStream` payload with partial content.
   * @param onError - Callback invoked on subscription errors (network, auth, engine).
   * @param onComplete - Callback invoked when the stream terminates normally.
   *
   * @example
   * ```typescript
   * await client.chatStream(
   *   "Tell me a story",
   *   threadId,
   *   (data) => process.stdout.write(data.content),
   *   (err) => console.error(err),
   *   () => console.log('\nDone.'),
   * );
   * ```
   */
  async chatStream(
    prompt: string,
    conversationId?: string,
    onNext?: (data: any) => void,
    onError?: (error: any) => void,
    onComplete?: () => void,
  ): Promise<void> {
    const { createClient } = await import('graphql-ws');
    const WebSocket = await import('ws');

    /** WebSocket endpoint derived from the HTTP endpoint. */
    const wsEndpoint = this.endpoint.replace(/^http/, 'ws');

    const wsClient = createClient({
      url: wsEndpoint,
      webSocketImpl: WebSocket.default || WebSocket,
    });

    const query = `
      subscription ChatStream($prompt: String!, $conversationId: String) {
        chatStream(prompt: $prompt, conversationId: $conversationId) {
          content
          conversationId
        }
      }
    `;

    wsClient.subscribe(
      {
        query,
        variables: { prompt, conversationId },
      },
      {
        next: (data) => {
          if (onNext) onNext((data as any).data?.chatStream);
        },
        error: (err) => {
          if (onError) onError(err);
        },
        complete: () => {
          if (onComplete) onComplete();
        },
      },
    );
  }
}
