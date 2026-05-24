/**
 * @file translations.types.ts
 * @description Shared type definitions for the internationalization dictionary.
 */

export type Locale = "en" | "fr";

export interface TranslationDictionary {
  sidebar: {
    dashboard: string;
    chatSessions: string;
    logout: string;
    memoryBlocks: string;
    playground: string;
  };
  header: {
    profile: string;
    settings: string;
    logout: string;
  };
  dashboard: {
    welcome: string;
    overview: string;
    activeSessions: string;
    tokensUsed: string;
    memoryNodes: string;
    runningParallel: string;
    estimatedCumulative: string;
    contextSaved: string;
    quickActions: string;
    startNewChat: string;
    startNewChatDesc: string;
    manageProfile: string;
    manageProfileDesc: string;
    configureSettings: string;
    configureSettingsDesc: string;
    recentActivity: string;
    noRecentSessions: string;
    resumeSession: string;
    lastActive: string;
    loading: string;
  };
  chat: {
    sessionTitle: string;
    id: string;
    typeMessage: string;
    send: string;
    sending: string;
    typing: string;
    loadingMessages: string;
    noActiveConversation: string;
    memoryBlocks: string;
    newBlock: string;
    ai: string;
    user: string;
    connectionError: string;
  };
  profile: {
    title: string;
    subtitle: string;
    accountDetails: string;
    detailsDesc: string;
    userId: string;
    subTier: string;
    username: string;
    email: string;
    assignedAuth: string;
    noAuth: string;
    prefMetadata: string;
    prefDesc: string;
    enterpriseTier: string;
    premiumTier: string;
    freeTier: string;
    failedLoad: string;
    unauthError: string;
  };
  settings: {
    title: string;
    description: string;
    language: string;
    english: string;
    french: string;
    aiPersona: string;
    standardPersona: string;
    concisePersona: string;
    creativePersona: string;
    tenantBranding: string;
    appName: string;
    appNamePlaceholder: string;
    tagline: string;
    taglinePlaceholder: string;
    tenantId: string;
    tenantIdPlaceholder: string;
    colorAccent: string;
    zincAccent: string;
    roseAccent: string;
    emeraldAccent: string;
    amberAccent: string;
    layoutScale: string;
    standardLayout: string;
    compactLayout: string;
    saveTheme: string;
    saving: string;
  };
}
