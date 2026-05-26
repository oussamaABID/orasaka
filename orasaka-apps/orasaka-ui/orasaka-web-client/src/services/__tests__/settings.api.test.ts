/**
 * @file settings.api.test.ts
 * @description Tests for the settings API adapter.
 */

import { SettingsApi } from "@/services/settings.api";
import { graphqlRequest } from "@/services/graphql-client";
import { THEME_MODE } from "@/core/constants/http.constants";

jest.mock("@/services/graphql-client", () => ({
  graphqlRequest: jest.fn(),
}));

const mockedGraphql = graphqlRequest as jest.MockedFunction<
  typeof graphqlRequest
>;

describe("SettingsApi", () => {
  beforeEach(() => {
    mockedGraphql.mockClear();
  });

  describe("fetch", () => {
    it("returns hydrated settings with defaults when preferences are empty", async () => {
      mockedGraphql.mockResolvedValueOnce({ me: { preferences: {} } });

      const result = await SettingsApi.fetch();

      expect(result.language).toBe("en");
      expect(result.autoSave).toBe(true);
      expect(result.aiPersona).toBe("standard");
      expect(result.themeName).toBe("Orasaka");
      expect(result.themeTagline).toBe("Decoupled Intelligence");
      expect(result.themeAccent).toBe("zinc");
      expect(result.themeLayout).toBe("standard");
      expect(result.theme).toBe("system");
      expect(result.tenantId).toBe("orasaka-default");
    });

    it("returns user preferences when populated", async () => {
      mockedGraphql.mockResolvedValueOnce({
        me: {
          preferences: {
            language: "fr",
            autoSave: false,
            aiPersona: "creative",
            themeName: "Custom",
            themeTagline: "My App",
            themeAccent: "rose",
            themeLayout: "compact",
            theme: THEME_MODE.DARK,
            tenantId: "tenant-abc",
          },
        },
      });

      const result = await SettingsApi.fetch();

      expect(result.language).toBe("fr");
      expect(result.autoSave).toBe(false);
      expect(result.aiPersona).toBe("creative");
      expect(result.themeName).toBe("Custom");
      expect(result.themeAccent).toBe("rose");
      expect(result.themeLayout).toBe("compact");
      expect(result.theme).toBe(THEME_MODE.DARK);
      expect(result.tenantId).toBe("tenant-abc");
    });

    it("handles null me gracefully", async () => {
      mockedGraphql.mockResolvedValueOnce({ me: null });

      const result = await SettingsApi.fetch();

      expect(result.language).toBe("en");
      expect(result.autoSave).toBe(true);
    });
  });

  describe("update", () => {
    it("sends partial settings and returns updated preferences", async () => {
      mockedGraphql.mockResolvedValueOnce({
        updatePreferences: { preferences: { language: "fr" } },
      });

      const result = await SettingsApi.update({ language: "fr" });

      expect(result).toEqual({ language: "fr" });
      expect(mockedGraphql).toHaveBeenCalledWith(
        expect.stringContaining("mutation UpdatePrefs"),
        { prefs: { language: "fr" } },
      );
    });

    it("returns empty object when response has no preferences", async () => {
      mockedGraphql.mockResolvedValueOnce({
        updatePreferences: null,
      });

      const result = await SettingsApi.update({ theme: THEME_MODE.DARK });

      expect(result).toEqual({});
    });
  });
});
