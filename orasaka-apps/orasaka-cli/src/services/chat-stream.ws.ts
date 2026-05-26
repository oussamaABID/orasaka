/**
 * @file chat-stream.ws.ts
 * @description WebSocket-based GraphQL subscription for real-time chat streaming.
 * Uses dynamic imports for `graphql-ws` and `ws` to support tree-shaking
 * and avoid bundling WebSocket libraries when not needed.
 *
 * NOTE: This module is excluded from coverage because Jest CJS mode
 * cannot mock dynamic ESM imports inside isolated modules.
 */

import type { ChatResponse } from "../types/api.types";
import { GATEWAY_URL } from "../env";

/**
 * Opens a WebSocket subscription for streaming chat tokens in real-time.
 */
export async function chatStreamWs(
  prompt: string,
  conversationId: string,
  onNext: (data: ChatResponse) => void,
  onError: (error: unknown) => void,
  onComplete: () => void,
): Promise<void> {
  const { createClient } = await import("graphql-ws");
  const WebSocket = await import("ws");

  const wsEndpoint = GATEWAY_URL.replace(/^http/, "ws") + "/graphql";

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
        const payload = data.data as { chatStream?: ChatResponse } | null | undefined;
        if (payload?.chatStream) {
          onNext(payload.chatStream);
        }
      },
      error: (err) => {
        onError(err);
      },
      complete: () => {
        onComplete();
      },
    },
  );
}
