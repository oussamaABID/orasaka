/**
 * @file settings.api.ts
 * @description Stateless outbound adapter for user preference settings.
 * Extracts network logic previously inlined inside `useSettings.ts`.
 */

import { graphqlRequest } from "./graphql-client";
import type { Settings } from "@/features/settings/types/settings.types";

// ── GraphQL Operations ──────────────────────────────────────────────────────

const FETCH_SETTINGS_QUERY = `
  query GetMe {
    me {
      preferences
    }
  }
`;

const UPDATE_SETTINGS_MUTATION = `
  mutation UpdatePrefs($prefs: Map!) {
    updatePreferences(preferences: $prefs) {
      preferences
    }
  }
`;

// ── Types ────────────────────────────────────────────────────────────────────

interface MeData {
  me: { preferences: Record<string, unknown> };
}

interface UpdatePrefsData {
  updatePreferences: { preferences: Partial<Settings> };
}

/**
 * Stateless adapter exposing user settings network operations.
 */
export const SettingsApi = {
  /**
   * Fetches user preference configurations from the BFF GraphQL proxy.
   *
   * @returns The fully-hydrated settings object with defaults applied.
   */
  fetch: async (): Promise<Settings> => {
    const data = await graphqlRequest<MeData>(FETCH_SETTINGS_QUERY);
    const preferences = data.me?.preferences || {};
    return {
      language: (preferences.language as string) || "en",
      autoSave:
        preferences.autoSave !== undefined
          ? (preferences.autoSave as boolean)
          : true,
      aiPersona: (preferences.aiPersona as Settings["aiPersona"]) || "standard",
      themeName: (preferences.themeName as string) || "Orasaka",
      themeTagline:
        (preferences.themeTagline as string) || "Decoupled Intelligence",
      themeAccent:
        (preferences.themeAccent as Settings["themeAccent"]) || "zinc",
      themeLayout:
        (preferences.themeLayout as Settings["themeLayout"]) || "standard",
      theme: (preferences.theme as Settings["theme"]) || "system",
      tenantId: (preferences.tenantId as string) || "orasaka-default",
    };
  },

  /**
   * Submits partial user preference updates to the BFF GraphQL proxy.
   *
   * @param settings - The partial preference keys to modify.
   * @returns The updated preferences payload from the server.
   */
  update: async (settings: Partial<Settings>): Promise<Partial<Settings>> => {
    const data = await graphqlRequest<UpdatePrefsData>(
      UPDATE_SETTINGS_MUTATION,
      { prefs: settings },
    );
    return data.updatePreferences?.preferences ?? {};
  },
} as const;
