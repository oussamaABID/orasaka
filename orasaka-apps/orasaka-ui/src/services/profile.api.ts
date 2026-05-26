/**
 * @file profile.api.ts
 * @description Stateless outbound adapter for user profile data.
 * Extracts network logic previously inlined inside `ProfileView.tsx`.
 */

import { graphqlRequest } from "./graphql-client";

// ── Types ────────────────────────────────────────────────────────────────────

export interface UserProfile {
  id: string;
  username: string;
  email: string;
  authorities: string[];
  preferences: Record<string, unknown>;
}

interface MeProfileData {
  me: UserProfile;
}

// ── GraphQL Operations ──────────────────────────────────────────────────────

const PROFILE_QUERY = `
  query GetProfile {
    me {
      id
      username
      email
      authorities
      preferences
    }
  }
`;

/**
 * Stateless adapter exposing user profile network operations.
 */
export const ProfileApi = {
  /**
   * Fetches the authenticated user's full profile from the BFF GraphQL proxy.
   *
   * @returns The user profile including identity, authorities, and preferences.
   */
  fetch: async (): Promise<UserProfile> => {
    const data = await graphqlRequest<MeProfileData>(PROFILE_QUERY);
    return data.me;
  },
} as const;
