/**
 * @file middleware.ts
 * @description Next.js Edge Middleware for authentication gating.
 * Redirects unauthenticated users to /login for protected page routes.
 * Uses the next-auth session token cookie to determine auth status server-side,
 * avoiding skeleton flash on protected pages.
 */

import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/**
 * Route prefixes that do NOT require authentication.
 */
const PUBLIC_PREFIXES = [
  "/login",
  "/register",
  "/api/",        // All API routes (auth, BFF proxy, etc.)
  "/_next/",      // Next.js internals (static, image, RSC payloads, HMR)
  "/favicon",
  "/logo.svg",
  "/privacy",
  "/terms",
  "/contact",
  "/features",
];

/**
 * Checks whether a given pathname matches any public (unauthenticated) route prefix.
 */
function isPublicPath(pathname: string): boolean {
  return PUBLIC_PREFIXES.some((prefix) => pathname.startsWith(prefix));
}

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Allow public routes, API routes, and static assets through
  if (isPublicPath(pathname)) {
    return NextResponse.next();
  }

  // Check for next-auth session token (works for both secure and dev cookies)
  const sessionToken =
    request.cookies.get("next-auth.session-token")?.value ||
    request.cookies.get("__Secure-next-auth.session-token")?.value;

  if (!sessionToken) {
    const loginUrl = new URL("/login", request.url);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  /**
   * Only run middleware on page routes — exclude all static assets and internals.
   */
  matcher: [
    "/((?!_next/static|_next/image|_next/data|favicon|api/).*)",
  ],
};
