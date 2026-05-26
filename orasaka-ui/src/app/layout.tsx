import type { Metadata } from "next";
import { Inter, JetBrains_Mono, Outfit } from "next/font/google";
import "./globals.css";
import { Providers } from "@/core/providers/Providers";
import { getServerSession } from "next-auth/next";
import { authOptions } from "@/app/api/auth/[...nextauth]/route";

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
    return "system";
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
      return preferences.theme || "system";
    }
  } catch (error) {
    console.error("Error fetching user theme from database:", error);
  }
  return "system";
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
  if (initialTheme === "dark") {
    initialClass = "dark";
  } else if (initialTheme === "custom") {
    initialClass = "custom";
  } else if (initialTheme === "cyberpunk") {
    initialClass = "cyberpunk";
  } else if (initialTheme === "solarized") {
    initialClass = "solarized";
  } else if (initialTheme === "light") {
    initialClass = "light";
  }

  return (
    <html lang="en" className={initialClass} suppressHydrationWarning>
      <head>
        <script
          dangerouslySetInnerHTML={{
            __html: `
              (function() {
                try {
                  var saved = localStorage.getItem('theme') || 'system';
                  var resolved = saved;
                  if (saved === 'system') {
                    resolved = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
                  }
                  document.documentElement.classList.remove('dark', 'light', 'custom', 'cyberpunk', 'solarized');
                  document.documentElement.classList.add(resolved);
                } catch (_) { /* localStorage unavailable in SSR — intentional no-op */ }
              })();
            `,
          }}
        />
      </head>
      <body
        className={`${inter.variable} ${jetbrainsMono.variable} ${outfit.variable} antialiased transition-colors duration-200`}
      >
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
