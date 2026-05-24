/**
 * @file useSettings.ts
 * @description Custom hook for fetching and updating user configurations via TanStack Query.
 * Interacts with the BFF GraphQL gateway endpoints to load or mutate preferences.
 *
 * State management: Employs TanStack Query to manage query states, loading states, and mutations.
 * Optimization: Performs optimistic query cache updates on successful preference mutations.
 */

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { OrasakaSettings } from "@/features/settings/types/settings.types";

/**
 * Fetches user preference configurations from the backend.
 * Uses a GraphQL query directed to the BFF API route.
 *
 * @async
 * @returns {Promise<OrasakaSettings>} A promise resolving to the user's parsed settings.
 * @throws {Error} If the HTTP fetch fails or GraphQL returns user errors.
 */
const fetchSettings = async (): Promise<OrasakaSettings> => {
  const query = `
    query GetMe {
      me {
        preferences
      }
    }
  `;

  const response = await fetch("/api/graphql", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ query }),
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch settings: ${response.statusText}`);
  }

  const result = await response.json();
  if (result.errors && result.errors.length > 0) {
    throw new Error(result.errors[0].message);
  }

  const preferences = result.data?.me?.preferences || {};
  return {
    language: preferences.language || "en",
    autoSave: preferences.autoSave !== undefined ? preferences.autoSave : true,
    aiPersona: preferences.aiPersona || "standard",
    themeName: preferences.themeName || "Orasaka",
    themeTagline: preferences.themeTagline || "Decoupled Intelligence",
    themeAccent: preferences.themeAccent || "zinc",
    themeLayout: preferences.themeLayout || "standard",
    tenantId: preferences.tenantId || "orasaka-default",
  };
};

/**
 * Submits partial user configurations to the backend settings DB resolver.
 *
 * @async
 * @param {Partial<OrasakaSettings>} settings - Partial preference keys to modify.
 * @returns {Promise<Record<string, any>>} The updated settings payload from the server.
 * @throws {Error} If mutation request returns invalid statuses or GraphQL validation errors.
 */
const updateSettings = async (
  settings: Partial<OrasakaSettings>,
): Promise<Partial<OrasakaSettings>> => {
  const query = `
    mutation UpdatePrefs($prefs: Map!) {
      updatePreferences(preferences: $prefs) {
        preferences
      }
    }
  `;

  const response = await fetch("/api/graphql", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      query,
      variables: { prefs: settings },
    }),
  });

  if (!response.ok) {
    throw new Error(`Failed to update settings: ${response.statusText}`);
  }

  const result = await response.json();
  if (result.errors && result.errors.length > 0) {
    throw new Error(result.errors[0].message);
  }

  return (
    (result.data?.updatePreferences?.preferences as Partial<OrasakaSettings>) ||
    {}
  );
};

/**
 * Custom React Hook to consume global application visual styles, layouts, and languages.
 * Provides caching and loading flags using TanStack Query underneath.
 *
 * @returns An object containing the query data, query loading state, mutator action, and mutator progress.
 */
export function useSettings() {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ["settings"],
    queryFn: fetchSettings,
  });

  const mutation = useMutation({
    mutationFn: updateSettings,
    onSuccess: (updatedPrefs) => {
      // Optimistically update the settings query cache
      queryClient.setQueryData(
        ["settings"],
        (old: OrasakaSettings | undefined) => {
          if (!old) return old;
          return {
            ...old,
            language: updatedPrefs.language || old.language,
            autoSave:
              updatedPrefs.autoSave !== undefined
                ? updatedPrefs.autoSave
                : old.autoSave,
            aiPersona: updatedPrefs.aiPersona || old.aiPersona,
            themeName: updatedPrefs.themeName || old.themeName,
            themeTagline: updatedPrefs.themeTagline || old.themeTagline,
            themeAccent: updatedPrefs.themeAccent || old.themeAccent,
            themeLayout: updatedPrefs.themeLayout || old.themeLayout,
            tenantId: updatedPrefs.tenantId || old.tenantId,
          };
        },
      );
    },
  });

  return {
    settings: query.data,
    isLoading: query.isLoading,
    updateSettings: mutation.mutate,
    isUpdating: mutation.isPending,
  };
}
