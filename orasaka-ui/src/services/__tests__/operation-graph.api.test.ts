/**
 * @file operation-graph.api.test.ts
 * @description Tests for the operation graph API adapter.
 */

import { OperationGraphApi } from "@/services/operation-graph.api";
import { graphqlRequest } from "@/services/graphql-client";

jest.mock("@/services/graphql-client", () => ({
  graphqlRequest: jest.fn(),
}));

const mockedGraphql = graphqlRequest as jest.MockedFunction<
  typeof graphqlRequest
>;

describe("OperationGraphApi", () => {
  beforeEach(() => {
    mockedGraphql.mockClear();
  });

  describe("fetchNodes", () => {
    it("returns operation nodes from GraphQL response", async () => {
      const nodes = [
        {
          id: "speech",
          label: "Speech Synthesis",
          icon: "🔊",
          presentationContext: "tts",
          state: { type: "ACTIVE" },
          executionDetails: {
            uriPath: "/v1/audio/speech",
            httpMethod: "POST",
          },
        },
        {
          id: "image",
          label: "Image Generation",
          icon: "🎨",
          presentationContext: "image",
          state: { type: "ACTIVE" },
          executionDetails: {
            uriPath: "/v1/images/generations",
            httpMethod: "POST",
          },
        },
      ];
      mockedGraphql.mockResolvedValueOnce({
        operationGraph: { nodes },
      });

      const result = await OperationGraphApi.fetchNodes();

      expect(result).toHaveLength(2);
      expect(result[0].id).toBe("speech");
      expect(result[1].id).toBe("image");
    });

    it("returns empty array when operationGraph is null", async () => {
      mockedGraphql.mockResolvedValueOnce({ operationGraph: null });

      const result = await OperationGraphApi.fetchNodes();

      expect(result).toEqual([]);
    });

    it("returns empty array when nodes array is missing", async () => {
      mockedGraphql.mockResolvedValueOnce({ operationGraph: {} });

      const result = await OperationGraphApi.fetchNodes();

      expect(result).toEqual([]);
    });

    it("calls graphqlRequest with the operation graph query", async () => {
      mockedGraphql.mockResolvedValueOnce({
        operationGraph: { nodes: [] },
      });

      await OperationGraphApi.fetchNodes();

      expect(mockedGraphql).toHaveBeenCalledWith(
        expect.stringContaining("query GetOperationGraph"),
      );
    });
  });
});
