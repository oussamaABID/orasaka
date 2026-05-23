/**
 * Settings object representing user preferences and theme customization.
 */
export interface OrasakaSettings {
  language: string;
  autoSave: boolean;
  aiPersona: "standard" | "concise" | "creative";
  themeName: string;
  themeTagline: string;
  themeAccent: "rose" | "emerald" | "amber" | "zinc";
  themeLayout: "standard" | "compact";
  tenantId: string;
}
