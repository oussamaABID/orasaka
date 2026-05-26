/**
 * @file auth-options.ts
 * @description NextAuth configuration extracted from the routing tree per dependency-cruiser rule
 * `no-auth-config-in-routing`. Defines providers (OAuth + Credentials), JWT/Session callbacks,
 * and custom page routes for the Next.js BFF authentication layer.
 *
 * All direct user credentials authentication is delegated to the gateway and validated on the backend.
 *
 * @see {@link http://localhost:8080/api/v1/auth/login} - Upstream credentials auth endpoint.
 */

import type { NextAuthOptions } from "next-auth";
import GithubProvider from "next-auth/providers/github";
import GoogleProvider from "next-auth/providers/google";
import CredentialsProvider from "next-auth/providers/credentials";

/**
 * NextAuth options defining authentication providers, custom callbacks, pages routing,
 * and JWT session mapping for the Next.js application.
 */
export const authOptions: NextAuthOptions = {
  providers: [
    GithubProvider({
      clientId: process.env.GITHUB_ID || "",
      clientSecret: process.env.GITHUB_SECRET || "",
    }),
    GoogleProvider({
      clientId: process.env.GOOGLE_CLIENT_ID || "",
      clientSecret: process.env.GOOGLE_CLIENT_SECRET || "",
    }),
    CredentialsProvider({
      name: "Gateway",
      credentials: {
        email: {
          label: "Email",
          type: "email",
          placeholder: "user@orasaka.com",
        },
        password: { label: "Password", type: "password" },
      },
      async authorize(credentials) {
        if (!credentials?.email || !credentials?.password) {
          return null;
        }

        const gatewayUrl = process.env.GATEWAY_URL || "http://localhost:8080";

        try {
          const res = await fetch(`${gatewayUrl}/api/v1/auth/login`, {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
            },
            body: JSON.stringify({
              email: credentials.email,
              password: credentials.password,
            }),
          });

          if (!res.ok) {
            return null;
          }

          const data = await res.json(); // returns { token: "uuid", username: "name", email: "email", authorities: [...], active_interceptions: [...] }
          if (data && data.token) {
            const role =
              data.authorities && data.authorities.includes("ROLE_ADMIN")
                ? "admin"
                : "user";
            return {
              id: data.token,
              name: data.username,
              email: data.email || credentials.email,
              role: role,
              activeInterceptions: data.active_interceptions || [],
            };
          }
        } catch (error) {
          console.error(
            "NextAuth authorize error calling gateway login:",
            error,
          );
        }
        return null;
      },
    }),
  ],
  pages: {
    signIn: "/login",
  },
  callbacks: {
    async jwt({ token, user, account, trigger, session }) {
      if (user) {
        if (
          account &&
          (account.provider === "github" || account.provider === "google")
        ) {
          try {
            const gatewayUrl =
              process.env.GATEWAY_URL || "http://localhost:8080";
            const response = await fetch(`${gatewayUrl}/api/v1/auth/oauth`, {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({
                email: user.email,
                username: user.name || user.email?.split("@")[0],
              }),
            });
            if (response.ok) {
              const dbUser = await response.json();
              token.id = dbUser.token;
              token.role = "user";
              token.activeInterceptions = dbUser.active_interceptions || [];
            } else {
              console.error(
                "Failed to JIT provision OAuth user:",
                response.statusText,
              );
            }
          } catch (error) {
            console.error("Error during JIT provisioning:", error);
          }
        } else {
          token.role = user.role;
          token.id = user.id;
          token.activeInterceptions = user.activeInterceptions || [];
        }
      }
      // If we trigger an update of the session on the client side, sync activeInterceptions
      if (trigger === "update" && session?.activeInterceptions) {
        token.activeInterceptions = session.activeInterceptions;
      }
      return token;
    },
    async session({ session, token }) {
      if (session.user) {
        session.user.role = token.role;
        session.user.id = token.id;
        session.user.activeInterceptions = token.activeInterceptions || [];
      }
      return session;
    },
  },
  session: {
    strategy: "jwt",
  },
};
