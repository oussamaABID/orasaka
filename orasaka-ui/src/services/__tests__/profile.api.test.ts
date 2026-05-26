/**
 * @file profile.api.test.ts
 * @description Tests for the profile API adapter.
 */

import { ProfileApi } from "@/services/profile.api";
import { graphqlRequest } from "@/services/graphql-client";

jest.mock("@/services/graphql-client", () => ({
  graphqlRequest: jest.fn(),
}));

const mockedGraphql = graphqlRequest as jest.MockedFunction<
  typeof graphqlRequest
>;

describe("ProfileApi", () => {
  beforeEach(() => {
    mockedGraphql.mockClear();
  });

  describe("fetch", () => {
    it("returns the user profile from GraphQL response", async () => {
      const profile = {
        id: "user-uuid-123",
        username: "orasaka_admin",
        email: "admin@orasaka.io",
        authorities: ["ROLE_ADMIN", "ROLE_USER"],
        preferences: { language: "en", theme: "dark" },
      };
      mockedGraphql.mockResolvedValueOnce({ me: profile });

      const result = await ProfileApi.fetch();

      expect(result).toEqual(profile);
      expect(result.id).toBe("user-uuid-123");
      expect(result.authorities).toContain("ROLE_ADMIN");
    });

    it("calls graphqlRequest with the profile query", async () => {
      mockedGraphql.mockResolvedValueOnce({
        me: {
          id: "1",
          username: "u",
          email: "e@e.com",
          authorities: [],
          preferences: {},
        },
      });

      await ProfileApi.fetch();

      expect(mockedGraphql).toHaveBeenCalledWith(
        expect.stringContaining("query GetProfile"),
      );
    });

    it("propagates GraphQL errors", async () => {
      mockedGraphql.mockRejectedValueOnce(new Error("Unauthorized"));

      await expect(ProfileApi.fetch()).rejects.toThrow("Unauthorized");
    });
  });
});
