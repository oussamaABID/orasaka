/**
 * @file interception.api.test.ts
 * @description Tests for the interception API adapter.
 */

import { InterceptionApi } from "@/services/interception.api";
import { graphqlRequest } from "@/services/graphql-client";

jest.mock("@/services/graphql-client", () => ({
  graphqlRequest: jest.fn(),
}));

const mockedGraphql = graphqlRequest as jest.MockedFunction<
  typeof graphqlRequest
>;

describe("InterceptionApi", () => {
  beforeEach(() => {
    mockedGraphql.mockClear();
  });

  describe("fetchSchema", () => {
    it("parses and returns schema descriptor", async () => {
      const schema = {
        title: "Onboarding",
        description: "Complete your profile",
        fields: [
          {
            name: "industry",
            label: "Industry",
            type: "select",
            required: true,
          },
        ],
      };
      mockedGraphql.mockResolvedValueOnce({
        interceptionSchema: JSON.stringify(schema),
      });

      const result = await InterceptionApi.fetchSchema("onboarding-v1");

      expect(result.title).toBe("Onboarding");
      expect(result.fields).toHaveLength(1);
      expect(result.fields[0].name).toBe("industry");
    });

    it("passes schemaId as variable", async () => {
      mockedGraphql.mockResolvedValueOnce({
        interceptionSchema: JSON.stringify({
          title: "t",
          description: "d",
          fields: [],
        }),
      });

      await InterceptionApi.fetchSchema("feedback-v2");

      expect(mockedGraphql).toHaveBeenCalledWith(
        expect.stringContaining("GetInterceptionSchema"),
        { schemaId: "feedback-v2" },
      );
    });

    it("throws when no schema returned", async () => {
      mockedGraphql.mockResolvedValueOnce({ interceptionSchema: null });

      await expect(InterceptionApi.fetchSchema("missing")).rejects.toThrow(
        "No schema returned from server.",
      );
    });

    it("throws on malformed JSON schema", async () => {
      mockedGraphql.mockResolvedValueOnce({
        interceptionSchema: "not-valid-json{",
      });

      await expect(InterceptionApi.fetchSchema("bad-json")).rejects.toThrow();
    });
  });

  describe("resolve", () => {
    it("submits responses and returns boolean", async () => {
      mockedGraphql.mockResolvedValueOnce({ resolveInterception: true });

      const result = await InterceptionApi.resolve(
        "onboarding",
        "onboarding-v1",
        { industry: "tech", role: "developer" },
      );

      expect(result).toBe(true);
      expect(mockedGraphql).toHaveBeenCalledWith(
        expect.stringContaining("ResolveInterception"),
        {
          interceptionType: "onboarding",
          schemaId: "onboarding-v1",
          responses: { industry: "tech", role: "developer" },
        },
      );
    });

    it("returns false when resolution fails", async () => {
      mockedGraphql.mockResolvedValueOnce({ resolveInterception: false });

      const result = await InterceptionApi.resolve("feedback", "fb-v1", {});

      expect(result).toBe(false);
    });
  });
});
