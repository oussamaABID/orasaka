/**
 * @file graphql-client.test.ts
 * @description Tests for the stateless GraphQL HTTP adapter.
 */

import { graphqlRequest } from "@/services/graphql-client";

// Mock global fetch
const mockFetch = jest.fn();
global.fetch = mockFetch;

describe("graphqlRequest", () => {
  beforeEach(() => {
    mockFetch.mockClear();
  });

  it("sends POST request to /api/graphql with query", async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ data: { me: { id: "1" } } }),
    });

    await graphqlRequest("query { me { id } }");

    expect(mockFetch).toHaveBeenCalledWith("/api/graphql", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        query: "query { me { id } }",
        variables: undefined,
      }),
    });
  });

  it("sends variables when provided", async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ data: { user: { name: "test" } } }),
    });

    await graphqlRequest("query GetUser($id: ID!) { user(id: $id) { name } }", {
      id: "123",
    });

    const body = JSON.parse(mockFetch.mock.calls[0][1].body);
    expect(body.variables).toEqual({ id: "123" });
  });

  it("returns typed data payload on success", async () => {
    const expected = { me: { preferences: { language: "en" } } };
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ data: expected }),
    });

    const result = await graphqlRequest<typeof expected>(
      "query { me { preferences } }",
    );
    expect(result).toEqual(expected);
  });

  it("throws error on non-ok HTTP response", async () => {
    mockFetch.mockResolvedValueOnce({
      ok: false,
      statusText: "Unauthorized",
    });

    await expect(graphqlRequest("query { me { id } }")).rejects.toThrow(
      "GraphQL request failed: Unauthorized",
    );
  });

  it("throws error when GraphQL response contains errors", async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        data: null,
        errors: [{ message: "Field 'me' not found" }],
      }),
    });

    await expect(graphqlRequest("query { me { id } }")).rejects.toThrow(
      "Field 'me' not found",
    );
  });

  it("returns data even if errors array is empty", async () => {
    const expected = { me: { id: "1" } };
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ data: expected, errors: [] }),
    });

    const result = await graphqlRequest<typeof expected>("query { me { id } }");
    expect(result).toEqual(expected);
  });
});
