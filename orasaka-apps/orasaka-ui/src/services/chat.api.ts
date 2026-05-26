/**
 * @file chat.api.ts
 * @description Stateless outbound adapter for chat mutations and media generation.
 * Extracts network logic previously inlined inside `useChatStream.ts` and `ChatWindow.tsx`.
 */

import { graphqlRequest } from "./graphql-client";
import type { ChatResponse } from "@/features/chat-session/types/chat.types";

// ── GraphQL Operations ──────────────────────────────────────────────────────

const SEND_CHAT_MUTATION = `
  mutation SendChat($prompt: String!, $conversationId: String) {
    chat(prompt: $prompt, conversationId: $conversationId) {
      content
      conversationId
    }
  }
`;

const GENERATE_IMAGE_MUTATION = `
  mutation GenerateImage($prompt: String!) {
    image(prompt: $prompt) { content }
  }
`;

const GENERATE_SPEECH_MUTATION = `
  mutation GenerateSpeech($prompt: String!) {
    speech(prompt: $prompt) { content }
  }
`;

// ── Types ────────────────────────────────────────────────────────────────────

interface SendChatData {
  chat: ChatResponse;
}

interface ImageData {
  image: { content: string };
}

interface SpeechData {
  speech: { content: string };
}

/**
 * Stateless adapter exposing chat and media generation network operations.
 */
export const ChatApi = {
  /**
   * Posts a chat message to the BFF GraphQL proxy.
   *
   * @param prompt - The user's text prompt.
   * @param conversationId - The target conversation thread identifier.
   * @returns The parsed chat response with content and conversation ID.
   */
  sendMessage: async (
    prompt: string,
    conversationId: string,
  ): Promise<ChatResponse> => {
    const data = await graphqlRequest<SendChatData>(SEND_CHAT_MUTATION, {
      prompt,
      conversationId,
    });
    return data.chat;
  },

  /**
   * Generates an image via the BFF GraphQL proxy.
   *
   * @param prompt - The image generation prompt.
   * @returns The base64-encoded image content string.
   */
  generateImage: async (prompt: string): Promise<string> => {
    const data = await graphqlRequest<ImageData>(GENERATE_IMAGE_MUTATION, {
      prompt,
    });
    return data.image.content;
  },

  /**
   * Generates speech audio via the BFF GraphQL proxy.
   *
   * @param prompt - The text-to-speech input.
   * @returns The base64-encoded audio content string.
   */
  generateSpeech: async (prompt: string): Promise<string> => {
    const data = await graphqlRequest<SpeechData>(GENERATE_SPEECH_MUTATION, {
      prompt,
    });
    return data.speech.content;
  },
} as const;
