/**
 * @file interception.api.ts
 * @description Stateless outbound adapter for interception schema and resolution operations.
 * Extracts network logic previously inlined inside `InterceptionForm.tsx`.
 */

import { graphqlRequest } from "./graphql-client";

// ── Types ────────────────────────────────────────────────────────────────────

export interface SchemaFieldOption {
  label: string;
  value: string;
}

export interface SchemaField {
  name: string;
  label: string;
  type: "text" | "select" | "textarea";
  required?: boolean;
  options?: SchemaFieldOption[];
  defaultValue?: string;
  placeholder?: string;
}

export interface SchemaDescriptor {
  title: string;
  description: string;
  fields: SchemaField[];
}

interface SchemaData {
  interceptionSchema: string;
}

interface ResolveData {
  resolveInterception: boolean;
}

// ── GraphQL Operations ──────────────────────────────────────────────────────

const FETCH_SCHEMA_QUERY = `
  query GetInterceptionSchema($schemaId: String!) {
    interceptionSchema(schemaId: $schemaId)
  }
`;

const RESOLVE_MUTATION = `
  mutation ResolveInterception(
    $interceptionType: String!
    $schemaId: String!
    $responses: Map!
  ) {
    resolveInterception(
      interceptionType: $interceptionType
      schemaId: $schemaId
      responses: $responses
    )
  }
`;

/**
 * Stateless adapter exposing interception-related network operations.
 */
export const InterceptionApi = {
  /**
   * Fetches a dynamic interception form schema from the BFF GraphQL proxy.
   *
   * @param schemaId - The schema identifier to resolve.
   * @returns The parsed schema descriptor with field definitions.
   */
  fetchSchema: async (schemaId: string): Promise<SchemaDescriptor> => {
    const data = await graphqlRequest<SchemaData>(FETCH_SCHEMA_QUERY, {
      schemaId,
    });

    const rawSchema = data.interceptionSchema;
    if (!rawSchema) throw new Error("No schema returned from server.");

    return JSON.parse(rawSchema) as SchemaDescriptor;
  },

  /**
   * Submits interception form responses to the BFF GraphQL proxy.
   *
   * @param interceptionType - The interception type identifier.
   * @param schemaId - The schema identifier.
   * @param responses - The form response key-value map.
   * @returns The resolved status from the server.
   */
  resolve: async (
    interceptionType: string,
    schemaId: string,
    responses: Record<string, string>,
  ): Promise<boolean> => {
    const data = await graphqlRequest<ResolveData>(RESOLVE_MUTATION, {
      interceptionType,
      schemaId,
      responses,
    });
    return data.resolveInterception;
  },
} as const;
