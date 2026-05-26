import { ApiClient } from "./api-client";
import { chatStreamWs } from "./chat-stream.ws";
import type { ChatResponse } from "../types/api.types";
import { GATEWAY_URL } from "../env";

/** Parses SSE data payload and extracts the content value. */
function parseSseLine(raw: string): string | null {
  const trimmed = raw.trim();
  try {
    const parsed = JSON.parse(trimmed) as { content?: string };
    return parsed.content || null;
  } catch {
    return trimmed || null;
  }
}

/** Consumes an SSE ReadableStream using async iteration and dispatches parsed content. */
async function consumeSseStream(
  body: ReadableStream<Uint8Array>,
  onNext: (content: string) => void,
  onComplete: () => void,
  onError?: (error: Error) => void,
): Promise<void> {
  const decoder = new TextDecoder();
  let buffer = "";

  try {
    for await (const chunk of body) {
      buffer += decoder.decode(chunk, { stream: true });

      const packets = buffer.split("\n\n");
      buffer = packets.pop() || "";

      for (const packet of packets) {
        const lines = packet.split("\n");

        for (const line of lines) {
          if (line.startsWith("data: ")) {
            const rawContent = line.slice(6).trim();

            if (rawContent === "[DONE]") {
              onComplete();
              return;
            }

            const content = parseSseLine(rawContent);
            if (content) onNext(content);
          }
        }
      }
    }

    // Flush remaining buffer
    if (buffer.trim().startsWith("data: ")) {
      const content = parseSseLine(buffer.slice(6).trim());
      if (content) onNext(content);
    }

    onComplete();
  } catch (error) {
    const wrappedError = error instanceof Error ? error : new Error(String(error));
    if (onError) {
      onError(wrappedError);
    }
  }
}

export const ChatApi = {
  /**
   * Executes a single-turn chat mutation.
   */
  chat: async (prompt: string, conversationId?: string): Promise<ChatResponse> => {
    const mutation = `
      mutation Chat($prompt: String!, $conversationId: String) {
        chat(prompt: $prompt, conversationId: $conversationId) {
          content
          conversationId
        }
      }
    `;
    const data = await ApiClient.requestGql<{ chat: ChatResponse }>(mutation, {
      prompt,
      conversationId,
    });
    return data.chat;
  },

  /**
   * Generates an image via the GraphQL image mutation.
   */
  generateImage: async (prompt: string, model?: string): Promise<ChatResponse> => {
    const mutation = `
      mutation GenerateImage($prompt: String!, $model: String) {
        image(prompt: $prompt, model: $model) {
          content
          conversationId
          metadata
        }
      }
    `;
    const data = await ApiClient.requestGql<{ image: ChatResponse }>(mutation, { prompt, model });
    return data.image;
  },

  /**
   * Generates speech audio via the GraphQL speech mutation.
   */
  generateSpeech: async (text: string, model?: string, voice?: string): Promise<ChatResponse> => {
    const mutation = `
      mutation GenerateSpeech($prompt: String!, $model: String, $voice: String) {
        speech(prompt: $prompt, model: $model, voice: $voice) {
          content
          conversationId
          metadata
        }
      }
    `;
    const data = await ApiClient.requestGql<{ speech: ChatResponse }>(mutation, { prompt: text, model, voice });
    return data.speech;
  },

  /**
   * Opens a WebSocket subscription for streaming chat tokens in real-time.
   * Delegates to the extracted chat-stream.ws module.
   */
  chatStream: chatStreamWs,

  /**
   * Streams chat via REST SSE from the Gateway.
   */
  streamRest: async (
    uriPath: string,
    conversationId: string,
    prompt: string,
    token: string,
    onNext: (content: string) => void,
    onError: (error: Error) => void,
    onComplete: () => void,
  ): Promise<void> => {
    const url = `${GATEWAY_URL}${uriPath}/${conversationId}?prompt=${encodeURIComponent(prompt)}`;

    try {
      const response = await fetch(url, {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (!response.ok) {
        throw new Error(`HTTP stream failed: status ${response.status} ${response.statusText}`);
      }

      if (!response.body) {
        throw new Error("No response body available for streaming");
      }

      await consumeSseStream(response.body, onNext, onComplete, onError);
    } catch (err) {
      onError(err instanceof Error ? err : new Error(String(err)));
    }
  },

  /**
   * Streams feature-to-code output via REST SSE POST from the Gateway.
   */
  streamCodeRest: async (
    prompt: string,
    model: string | undefined,
    token: string,
    onNext: (content: string) => void,
    onError: (error: Error) => void,
    onComplete: () => void,
  ): Promise<void> => {
    const url = `${GATEWAY_URL}/api/v1/chat/code`;

    try {
      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ prompt, model }),
      });

      if (!response.ok) {
        throw new Error(`HTTP stream failed: status ${response.status} ${response.statusText}`);
      }

      if (!response.body) {
        throw new Error("No response body available for streaming");
      }

      await consumeSseStream(response.body, onNext, onComplete, onError);
    } catch (err) {
      onError(err instanceof Error ? err : new Error(String(err)));
    }
  },
} as const;

