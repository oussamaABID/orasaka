/**
 * @file route.ts
 * @description Catch-all API Route Handler that acts as a server-side BFF (Backend-for-Frontend) proxy
 * for all REST endpoints under /api/v1/*.
 *
 * This handler validates the client session, injects the user's ID as a Bearer token,
 * and proxies the request to the upstream Orasaka Gateway on port 8080.
 */

import { NextResponse } from "next/server";
import { getServerSession } from "next-auth/next";
import { authOptions } from "@/app/api/auth/[...nextauth]/route";

async function proxyRequest(req: Request, segments: string[], method: string) {
  const gatewayUrl = process.env.GATEWAY_URL || "http://localhost:8080";

  try {
    const session = await getServerSession(authOptions);
    console.log(
      "[BFF Proxy Debug] session:",
      JSON.stringify(session),
      "segments:",
      segments,
      "headers:",
      req.headers.get("cookie"),
    );
    if (!session?.user || !session.user.id) {
      return NextResponse.json(
        { error: "Unauthorized: Missing active security context" },
        { status: 401 },
      );
    }

    const userId = session.user.id;
    const pathStr = segments.join("/");
    const { search } = new URL(req.url);
    const targetUrl = `${gatewayUrl}/api/v1/${pathStr}${search}`;

    let body: BodyInit | undefined = undefined;
    if (["POST", "PUT", "PATCH"].includes(method)) {
      try {
        const reqContentType = req.headers.get("content-type") || "";
        if (reqContentType.includes("multipart/form-data")) {
          body = await req.arrayBuffer();
        } else {
          body = await req.text();
        }
      } catch {
        // Request has no body or reading failed
      }
    }

    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      Authorization: `Bearer ${userId}`,
    };

    // Forward incoming content type if present
    const reqContentType = req.headers.get("content-type");
    if (reqContentType) {
      headers["Content-Type"] = reqContentType;
    }

    const response = await fetch(targetUrl, {
      method,
      headers,
      body,
    });

    const contentType = response.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
      const data = await response.json();
      return NextResponse.json(data, { status: response.status });
    } else {
      const data = await response.text();
      return new NextResponse(data, {
        status: response.status,
        headers: { "Content-Type": contentType },
      });
    }
  } catch (error: unknown) {
    console.error(
      `[BFF Proxy] Error proxying ${method} to /api/v1/${segments.join("/")}:`,
      error,
    );
    const message =
      error instanceof Error ? error.message : "BFF API Proxy Error";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

export async function POST(
  req: Request,
  { params }: { params: Promise<{ path: string[] }> },
) {
  const resolvedParams = await params;
  return proxyRequest(req, resolvedParams.path, "POST");
}

export async function GET(
  req: Request,
  { params }: { params: Promise<{ path: string[] }> },
) {
  const resolvedParams = await params;
  return proxyRequest(req, resolvedParams.path, "GET");
}

export async function PUT(
  req: Request,
  { params }: { params: Promise<{ path: string[] }> },
) {
  const resolvedParams = await params;
  return proxyRequest(req, resolvedParams.path, "PUT");
}

export async function DELETE(
  req: Request,
  { params }: { params: Promise<{ path: string[] }> },
) {
  const resolvedParams = await params;
  return proxyRequest(req, resolvedParams.path, "DELETE");
}

export async function PATCH(
  req: Request,
  { params }: { params: Promise<{ path: string[] }> },
) {
  const resolvedParams = await params;
  return proxyRequest(req, resolvedParams.path, "PATCH");
}
