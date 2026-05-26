/**
 * @file chat.api.test.ts
 * @description Tests for the chat API adapter.
 */

import { ChatApi } from "@/services/chat.api";
import { graphqlRequest } from "@/services/graphql-client";

jest.mock("@/services/graphql-client", () => ({
  graphqlRequest: jest.fn(),
}));

const mockedGraphql = graphqlRequest as jest.MockedFunction<
  typeof graphqlRequest
>;

describe("ChatApi", () => {
  beforeEach(() => {
    mockedGraphql.mockClear();
  });

  describe("sendMessage", () => {
    it("sends prompt and conversationId, returns chat response", async () => {
      const chatResponse = {
        content: "Hello! How can I help?",
        conversationId: "conv-456",
      };
      mockedGraphql.mockResolvedValueOnce({ chat: chatResponse });

      const result = await ChatApi.sendMessage("Hi", "conv-456");

      expect(result).toEqual(chatResponse);
      expect(mockedGraphql).toHaveBeenCalledWith(
        expect.stringContaining("mutation SendChat"),
        { prompt: "Hi", conversationId: "conv-456" },
      );
    });

    it("propagates GraphQL errors", async () => {
      mockedGraphql.mockRejectedValueOnce(new Error("Rate limited"));

      await expect(ChatApi.sendMessage("test", "conv-1")).rejects.toThrow(
        "Rate limited",
      );
    });
  });

  describe("generateImage", () => {
    it("sends prompt and returns base64 image content", async () => {
      mockedGraphql.mockResolvedValueOnce({
        image: { content: "base64-encoded-png" },
      });

      const result = await ChatApi.generateImage("A cyberpunk city");

      expect(result).toBe("base64-encoded-png");
      expect(mockedGraphql).toHaveBeenCalledWith(
        expect.stringContaining("mutation GenerateImage"),
        { prompt: "A cyberpunk city" },
      );
    });
  });

  describe("generateSpeech", () => {
    it("sends prompt and returns base64 audio content", async () => {
      mockedGraphql.mockResolvedValueOnce({
        speech: { content: "base64-encoded-mp3" },
      });

      const result = await ChatApi.generateSpeech("Hello world");

      expect(result).toBe("base64-encoded-mp3");
      expect(mockedGraphql).toHaveBeenCalledWith(
        expect.stringContaining("mutation GenerateSpeech"),
        { prompt: "Hello world" },
      );
    });
  });
});
