/**
 * @file translations.ts
 * @description Internationalization dictionary types and values for English and French.
 */

export type Locale = "en" | "fr";

export interface TranslationDictionary {
  sidebar: {
    dashboard: string;
    chatSessions: string;
    logout: string;
    memoryBlocks: string;
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

/**
 * Translation dictionary mappings for supported locales.
 */
export const translations: Record<Locale, TranslationDictionary> = {
  en: {
    sidebar: {
      dashboard: "Dashboard",
      chatSessions: "Chat Sessions",
      logout: "Log out",
      memoryBlocks: "Memory Blocks",
    },
    header: {
      profile: "Profile",
      settings: "Settings",
      logout: "Log out",
    },
    dashboard: {
      welcome: "Welcome back",
      overview: "Here's a quick overview of your Orasaka workspace.",
      activeSessions: "Active Sessions",
      tokensUsed: "Tokens Used",
      memoryNodes: "Memory Nodes",
      runningParallel: "Running in parallel",
      estimatedCumulative: "Estimated cumulative",
      contextSaved: "Context pieces saved",
      quickActions: "Quick Actions",
      startNewChat: "Start a New Chat",
      startNewChatDesc: "Initiate a secure, multi-tier conversation thread.",
      manageProfile: "Manage Profile",
      manageProfileDesc:
        "Review security identifiers, roles, and user context.",
      configureSettings: "System Settings",
      configureSettingsDesc:
        "Customize visual theme accents, language, and AI models.",
      recentActivity: "Recent Sessions",
      noRecentSessions: "No recent chat sessions found.",
      resumeSession: "Resume conversation",
      lastActive: "Active thread",
      loading: "Loading...",
    },
    chat: {
      sessionTitle: "Chat Session",
      id: "ID",
      typeMessage: "Type a message...",
      send: "Send",
      sending: "Sending...",
      typing: "Typing...",
      loadingMessages: "Loading messages...",
      noActiveConversation: "No active conversation.",
      memoryBlocks: "Memory Blocks",
      newBlock: "New Block",
      ai: "AI",
      user: "You",
      connectionError: "Connection lost.",
    },
    profile: {
      title: "Profile",
      subtitle:
        "Manage and review your security identifiers and preferences here.",
      accountDetails: "Account Details",
      detailsDesc:
        "Identities resolved directly from Postgres multi-tenant contexts.",
      userId: "User ID (UUID)",
      subTier: "Active Subscription Tier",
      username: "Username",
      email: "Registered Email",
      assignedAuth: "Assigned Authorities",
      noAuth: "No authorities assigned.",
      prefMetadata: "Preferences Metadata",
      prefDesc:
        "Dynamic configuration payload synchronized from user profile settings.",
      enterpriseTier: "Enterprise Tier",
      premiumTier: "Premium Tier",
      freeTier: "Free Tier",
      failedLoad: "Failed to load user profile",
      unauthError: "Unauthenticated or Gateway error",
    },
    settings: {
      title: "Global Application Settings",
      description:
        "Configure language parameters, model options, and dynamic theme properties.",
      language: "Language",
      english: "English",
      french: "Français",
      aiPersona: "AI Persona",
      standardPersona: "Standard Orasaka",
      concisePersona: "Concise & Direct",
      creativePersona: "Creative & Exploratory",
      tenantBranding: "Tenant Branding (Inversion of Control)",
      appName: "App Name (displayName)",
      appNamePlaceholder: "e.g. CinePulse, ChefPulse, FinTrack",
      tagline: "Tagline (tagline)",
      taglinePlaceholder: "Tagline displayed on application headers",
      tenantId: "Tenant ID (tenantId)",
      tenantIdPlaceholder: "e.g. tenant-uuid-string",
      colorAccent: "Color Accent (accentClass)",
      zincAccent: "Zinc (Muted Classic)",
      roseAccent: "Rose (Vibrant Red-Pink)",
      emeraldAccent: "Emerald (Sleek Green)",
      amberAccent: "Amber (Active Warm Gold)",
      layoutScale: "Layout Scale (layoutMode)",
      standardLayout: "Standard Spacing",
      compactLayout: "Compact (Higher Density)",
      saveTheme: "Save Dynamic Theme",
      saving: "Saving...",
    },
  },
  fr: {
    sidebar: {
      dashboard: "Tableau de bord",
      chatSessions: "Sessions de chat",
      logout: "Se déconnecter",
      memoryBlocks: "Blocs de mémoire",
    },
    header: {
      profile: "Profil",
      settings: "Paramètres",
      logout: "Déconnexion",
    },
    dashboard: {
      welcome: "Bon retour",
      overview: "Voici un aperçu rapide de votre espace de travail Orasaka.",
      activeSessions: "Sessions actives",
      tokensUsed: "Jetons utilisés",
      memoryNodes: "Nœuds de mémoire",
      runningParallel: "Actives en parallèle",
      estimatedCumulative: "Cumulatif estimé",
      contextSaved: "Éléments de contexte sauvegardés",
      quickActions: "Actions Rapides",
      startNewChat: "Nouveau Chat",
      startNewChatDesc:
        "Initier une session de conversation multi-niveaux sécurisée.",
      manageProfile: "Gérer le Profil",
      manageProfileDesc:
        "Vérifier vos identifiants de sécurité, rôles et contexte.",
      configureSettings: "Paramètres Système",
      configureSettingsDesc:
        "Personnaliser les accents de couleur, la langue et les modèles IA.",
      recentActivity: "Sessions Récentes",
      noRecentSessions: "Aucune session de chat récente trouvée.",
      resumeSession: "Reprendre la conversation",
      lastActive: "Discussion active",
      loading: "Chargement...",
    },
    chat: {
      sessionTitle: "Session de Chat",
      id: "ID",
      typeMessage: "Tapez votre message...",
      send: "Envoyer",
      sending: "Envoi...",
      typing: "Génération...",
      loadingMessages: "Chargement des messages...",
      noActiveConversation: "Aucune conversation active.",
      memoryBlocks: "Blocs de Mémoire",
      newBlock: "Nouveau Bloc",
      ai: "IA",
      user: "Vous",
      connectionError: "Connexion interrompue.",
    },
    profile: {
      title: "Profil",
      subtitle:
        "Gerez et examinez vos identifiants de sécurité et vos préférences ici.",
      accountDetails: "Détails du Compte",
      detailsDesc:
        "Identités résolues directement à partir des contextes multi-locataires Postgres.",
      userId: "ID Utilisateur (UUID)",
      subTier: "Niveau d'abonnement actif",
      username: "Nom d'utilisateur",
      email: "E-mail enregistré",
      assignedAuth: "Autorisations attribuées",
      noAuth: "Aucune autorisation attribuée.",
      prefMetadata: "Métadonnées de Préférences",
      prefDesc:
        "Données de configuration dynamique synchronisées depuis les paramètres du profil.",
      enterpriseTier: "Niveau Entreprise",
      premiumTier: "Niveau Premium",
      freeTier: "Niveau Gratuit",
      failedLoad: "Échec du chargement du profil utilisateur",
      unauthError: "Erreur d'authentification ou de passerelle",
    },
    settings: {
      title: "Paramètres Globaux de l'Application",
      description:
        "Configurez les paramètres de langue, les options de modèle IA et les thèmes dynamiques.",
      language: "Langue",
      english: "English",
      french: "Français",
      aiPersona: "Persona IA",
      standardPersona: "Orasaka Standard",
      concisePersona: "Concise & Directe",
      creativePersona: "Créative & Exploratoire",
      tenantBranding: "Marque Locataire (Inversion de Contrôle)",
      appName: "Nom de l'application (displayName)",
      appNamePlaceholder: "ex. CinePulse, ChefPulse, FinTrack",
      tagline: "Slogan (tagline)",
      taglinePlaceholder: "Slogan affiché dans les en-têtes",
      tenantId: "ID Locataire (tenantId)",
      tenantIdPlaceholder: "ex. chaine-uuid-locataire",
      colorAccent: "Accent de Couleur (accentClass)",
      zincAccent: "Zinc (Classique Mute)",
      roseAccent: "Rose (Vibrant Rouge-Rose)",
      emeraldAccent: "Émeraude (Sleek Vert)",
      amberAccent: "Ambre (Or Chaud Actif)",
      layoutScale: "Densité de Mise en Page (layoutMode)",
      standardLayout: "Espacement Standard",
      compactLayout: "Compact (Haute Densité)",
      saveTheme: "Enregistrer le Thème Dynamique",
      saving: "Enregistrement...",
    },
  },
};
