/**
 * @file route.ts
 * @description Next.js API route that acts as a server-side BFF (Backend-for-Frontend) proxy
 * for token-streaming chat requests. It avoids browser-side CORS issues, prevents exposing backend ports,
 * and secures credential storage by forwarding requests to the Gateway service.
 *
 * This endpoint runs strictly server-side and requires an active authenticated session.
 *
 * @see {@link http://localhost:8080/api/v1/chat/stream/{conversationId}} - Upstream gateway SSE endpoint.
 */

import { NextRequest } from "next/server";
import { getServerSession } from "next-auth/next";
import { authOptions } from "@/app/api/auth/[...nextauth]/route";

/**
 * Handles incoming SSE (Server-Sent Events) requests, validates the client session,
 * injects the user context ID as a Bearer token, and pipes the upstream gateway response stream
 * directly back to the frontend client without buffering.
 *
 * @param req - The NextRequest object representing the incoming HTTP request.
 * @param context - The context object containing route parameters.
 * @param context.params - A promise resolving to parameters containing the conversationId.
 * @returns A promise resolving to a Response object containing the active EventStream or an HTTP error code.
 */
export async function GET(
  req: NextRequest,
  { params }: { params: Promise<{ conversationId: string }> },
) {
  const { conversationId } = await params;
  const { searchParams } = new URL(req.url);
  const prompt = searchParams.get("prompt");

  if (!prompt) {
    return new Response("Missing prompt query parameter", { status: 400 });
  }

  const session = await getServerSession(authOptions);
  if (!session?.user || !session.user.id) {
    return new Response("Unauthorized: Missing active security context", {
      status: 401,
    });
  }

  const userId = session.user.id;
  const gatewayUrl = process.env.GATEWAY_URL || "http://localhost:8080";
  const targetUrl = `${gatewayUrl}/api/v1/chat/stream/${conversationId}?prompt=${encodeURIComponent(prompt)}`;

  try {
    const response = await fetch(targetUrl, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${userId}`,
        Accept: "text/event-stream",
      },
    });

    if (!response.ok) {
      return new Response(`Gateway stream error: ${response.statusText}`, {
        status: response.status,
      });
    }

    // Wrap the response body in a ReadableStream to stream chunks immediately without buffering
    const stream = new ReadableStream({
      async start(controller) {
        if (!response.body) {
          controller.close();
          return;
        }
        const reader = response.body.getReader();

        try {
          while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            controller.enqueue(value);
          }
        } catch (err) {
          controller.error(err);
        } finally {
          controller.close();
        }
      },
    });

    return new Response(stream, {
      headers: {
        "Content-Type": "text/event-stream",
        "Cache-Control": "no-cache, no-transform",
        Connection: "keep-alive",
        "X-Accel-Buffering": "no", // Prevent Nginx proxy buffering
      },
    });
  } catch (error: unknown) {
    console.error("SSE Streaming Proxy Error:", error);
    const message =
      error instanceof Error ? error.message : "BFF SSE Stream Proxy Error";
    return new Response(JSON.stringify({ error: message }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }
}
