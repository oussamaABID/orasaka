/**
 * @file route.ts
 * @description Next.js API route that acts as a server-side BFF (Backend-for-Frontend) proxy
 * for GraphQL queries, mutations, and requests. It isolates the backend Orasaka Gateway from direct
 * browser access, prevents CORS issues, and injects authenticated user identities into the Bearer token header.
 *
 * This endpoint runs strictly server-side and requires an active authenticated session.
 *
 * @see {@link http://localhost:8080/graphql} - Upstream gateway GraphQL endpoint.
 */

import { NextResponse } from "next/server";
import { getServerSession } from "next-auth/next";
import { authOptions } from "@/app/api/auth/[...nextauth]/route";

/**
 * Handles incoming GraphQL POST requests, validates the client session, injects the user context ID
 * as a Bearer token, and proxies the query/mutation payload directly to the gateway GraphQL endpoint.
 *
 * @param req - The Request object representing the incoming HTTP POST request.
 * @returns A promise resolving to a NextResponse containing the GraphQL result or an error payload.
 */
export async function POST(req: Request) {
  const gatewayUrl = process.env.GATEWAY_URL || "http://localhost:8080";

  try {
    const body = await req.json();
    const query = body.query || "";
    const variables = body.variables || {};

    const isVerifyEmail =
      /mutation\s+\w*VerifyEmail/i.test(query) || query.includes("verifyEmail");

    if (isVerifyEmail) {
      const token = variables.token;
      if (!token) {
        return NextResponse.json(
          {
            errors: [
              {
                message: "Token variable is required for VerifyEmail mutation",
              },
            ],
          },
          { status: 400 },
        );
      }

      const response = await fetch(`${gatewayUrl}/api/v1/auth/verify`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ token }),
      });

      if (response.ok) {
        return NextResponse.json({
          data: {
            verifyEmail: true,
          },
        });
      } else {
        return NextResponse.json({
          data: {
            verifyEmail: false,
          },
          errors: [{ message: "Invalid or expired verification token" }],
        });
      }
    }

    // Standard GraphQL request proxy: requires an active user session
    const session = await getServerSession(authOptions);

    if (!session?.user || !session.user.id) {
      return NextResponse.json(
        {
          errors: [
            { message: "Unauthorized: Missing active security context" },
          ],
        },
        { status: 401 },
      );
    }

    const userId = session.user.id;

    const response = await fetch(`${gatewayUrl}/graphql`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${userId}`,
      },
      body: JSON.stringify(body),
    });

    const data = await response.json();
    return NextResponse.json(data, { status: response.status });
  } catch (error: unknown) {
    console.error("GraphQL Proxy Error:", error);
    const message =
      error instanceof Error ? error.message : "BFF GraphQL Proxy Error";
    return NextResponse.json({ errors: [{ message }] }, { status: 500 });
  }
}
