/**
 * @file auth.api.test.ts
 * @description Tests for the authentication API adapter.
 */

import { AuthApi } from "@/services/auth.api";
import { graphqlRequest } from "@/services/graphql-client";

jest.mock("@/services/graphql-client", () => ({
  graphqlRequest: jest.fn(),
}));

const mockFetch = jest.fn();
global.fetch = mockFetch;
const mockedGraphql = graphqlRequest as jest.MockedFunction<
  typeof graphqlRequest
>;

describe("AuthApi", () => {
  beforeEach(() => {
    mockFetch.mockClear();
    mockedGraphql.mockClear();
  });

  describe("register", () => {
    it("sends registration payload to /api/register", async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ id: "new-user-123" }),
      });

      const payload = {
        username: "testuser",
        email: "test@example.com",
        password: "securepass",
        language: "en",
      };

      const result = await AuthApi.register(payload);

      expect(mockFetch).toHaveBeenCalledWith("/api/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      expect(result).toEqual({ id: "new-user-123" });
    });

    it("throws error object on registration failure", async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 409,
        json: async () => ({ error: "Email already registered" }),
      });

      const payload = {
        username: "testuser",
        email: "existing@example.com",
        password: "securepass",
        language: "en",
      };

      try {
        await AuthApi.register(payload);
        fail("Expected error to be thrown");
      } catch (err: unknown) {
        const error = err as Error & { status: number };
        expect(error.status).toBe(409);
        expect(error.message).toBe("Email already registered");
      }
    });
  });

  describe("verifyEmail", () => {
    it("returns true when verification succeeds", async () => {
      mockedGraphql.mockResolvedValueOnce({ verifyEmail: true });

      const result = await AuthApi.verifyEmail("valid-token");

      expect(result).toBe(true);
      expect(mockedGraphql).toHaveBeenCalledWith(
        expect.stringContaining("mutation VerifyEmail"),
        { token: "valid-token" },
      );
    });

    it("trims whitespace from token", async () => {
      mockedGraphql.mockResolvedValueOnce({ verifyEmail: true });

      await AuthApi.verifyEmail("  token-with-spaces  ");

      expect(mockedGraphql).toHaveBeenCalledWith(expect.any(String), {
        token: "token-with-spaces",
      });
    });

    it("throws error when verification returns false", async () => {
      mockedGraphql.mockResolvedValueOnce({ verifyEmail: false });

      await expect(AuthApi.verifyEmail("invalid-token")).rejects.toThrow(
        "Invalid or expired verification token.",
      );
    });
  });
});
