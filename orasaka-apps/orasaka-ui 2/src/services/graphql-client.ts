/**
 * @file graphql-client.ts
 * @description Stateless GraphQL HTTP adapter.
 * Every GraphQL call in the frontend MUST flow through this module.
 * Direct `fetch("/api/graphql")` calls in hooks or components are prohibited.
 */

interface GraphQLResponse<T> {
  data: T;
  errors?: Array<{ message: string }>;
}

/**
 * Executes a typed GraphQL request against the BFF proxy endpoint.
 *
 * @template T - The expected shape of the `data` field in the GraphQL response.
 * @param query - The GraphQL query or mutation string.
 * @param variables - Optional variable map to bind to the operation.
 * @returns A promise resolving to the typed `data` payload.
 * @throws {Error} If the HTTP request fails or GraphQL returns user errors.
 */
export async function graphqlRequest<T>(
  query: string,
  variables?: Record<string, unknown>,
): Promise<T> {
  const response = await fetch("/api/graphql", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ query, variables }),
  });

  if (!response.ok) {
    throw new Error(`GraphQL request failed: ${response.statusText}`);
  }

  const result: GraphQLResponse<T> = await response.json();

  if (result.errors && result.errors.length > 0) {
    throw new Error(result.errors[0].message);
  }

  return result.data;
}
