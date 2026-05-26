import type { Metadata } from "next";
import { Inter, JetBrains_Mono, Outfit } from "next/font/google";
import "./globals.css";
import { Providers } from "@/core/providers/Providers";
import { getServerSession } from "next-auth/next";
import { authOptions } from "@/core/auth/auth-options";
import { THEME_MODE } from "@/core/constants/http.constants";

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
  weight: ["300", "400", "500", "600", "700"],
});

const jetbrainsMono = JetBrains_Mono({
  variable: "--font-jetbrains-mono",
  subsets: ["latin"],
  weight: ["400", "500", "600"],
});

const outfit = Outfit({
  variable: "--font-outfit",
  subsets: ["latin"],
  weight: ["500", "600", "700", "800"],
});

/**
 * Krizaka Display — mapped to Outfit until custom .woff2 is provisioned.
 * CSS variable --font-krizaka-display resolves to Outfit's variable font.
 */
const krizakaDisplay = Outfit({
  variable: "--font-krizaka-display",
  subsets: ["latin"],
  weight: ["500", "600", "700", "800"],
});

/**
 * Krizaka Mono — mapped to JetBrains Mono until custom .woff2 is provisioned.
 * CSS variable --font-krizaka-mono resolves to JetBrains Mono's variable font.
 */
const krizakaMono = JetBrains_Mono({
  variable: "--font-krizaka-mono",
  subsets: ["latin"],
  weight: ["400", "500", "600"],
});

/**
 * Application global metadata configurations.
 */
export const metadata: Metadata = {
  title: "Orasaka UI",
  description: "Advanced AI Coding Interface",
  icons: {
    icon: [{ url: "/favicon.svg", type: "image/svg+xml" }],
    shortcut: "/favicon.svg",
    apple: "/favicon.svg",
  },
};

async function getUserTheme(): Promise<string> {
  const session = await getServerSession(authOptions);
  if (!session?.user?.id) {
    return THEME_MODE.SYSTEM;
  }
  try {
    const gatewayUrl = process.env.GATEWAY_URL || "http://localhost:8080";
    const res = await fetch(`${gatewayUrl}/graphql`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${session.user.id}`,
      },
      body: JSON.stringify({
        query: `query GetMe { me { preferences } }`,
      }),
      cache: "no-store",
    });
    if (res.ok) {
      const result = await res.json();
      const preferences = result.data?.me?.preferences || {};
      return preferences.theme || THEME_MODE.SYSTEM;
    }
  } catch (error) {
    console.error("Error fetching user theme from database:", error);
  }
  return THEME_MODE.SYSTEM;
}

/**
 * Root layout component for the entire Next.js application.
 *
 * @param props The layout component properties.
 * @returns The HTML structure of the layout.
 */
export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const initialTheme = await getUserTheme();
  let initialClass = "";
  if (initialTheme === THEME_MODE.DARK) {
    initialClass = THEME_MODE.DARK;
  } else if (initialTheme === "custom") {
    initialClass = "custom";
  } else if (initialTheme === THEME_MODE.CYBERPUNK) {
    initialClass = THEME_MODE.CYBERPUNK;
  } else if (initialTheme === "solarized") {
    initialClass = "solarized";
  } else if (initialTheme === THEME_MODE.KRIZAKA) {
    initialClass = THEME_MODE.KRIZAKA;
  } else if (initialTheme === THEME_MODE.LIGHT) {
    initialClass = THEME_MODE.LIGHT;
  }

  return (
    <html lang="en" className={initialClass} suppressHydrationWarning>
      <head>
        <script
          dangerouslySetInnerHTML={{
            __html: `
              (function() {
                try {
                  var saved = localStorage.getItem('theme') || THEME_MODE.SYSTEM;
                  var resolved = saved;
                  if (saved === THEME_MODE.SYSTEM) {
                    resolved = window.matchMedia('(prefers-color-scheme: dark)').matches ? THEME_MODE.DARK : THEME_MODE.LIGHT;
                  }
                  document.documentElement.classList.remove(THEME_MODE.DARK, THEME_MODE.LIGHT, 'custom', THEME_MODE.CYBERPUNK, 'solarized', 'krizaka');
                  document.documentElement.classList.add(resolved);
                } catch (_) { /* localStorage unavailable in SSR — intentional no-op */ }
              })();
            `,
          }}
        />
      </head>
      <body
        className={`${inter.variable} ${jetbrainsMono.variable} ${outfit.variable} ${krizakaDisplay.variable} ${krizakaMono.variable} antialiased transition-colors duration-200`}
      >
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
