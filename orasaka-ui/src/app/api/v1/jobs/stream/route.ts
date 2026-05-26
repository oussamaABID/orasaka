import { NextRequest } from "next/server";
import { getServerSession } from "next-auth/next";
import { authOptions } from "@/app/api/auth/[...nextauth]/route";

/**
 * Next.js API route acting as a server-side BFF proxy for job status SSE stream.
 * Connects to the upstream gateway SSE endpoint injecting the user session ID as access token.
 */
export async function GET(_req: NextRequest) {
  const session = await getServerSession(authOptions);
  if (!session?.user || !session.user.id) {
    return new Response("Unauthorized: Missing active security context", {
      status: 401,
    });
  }

  const userId = session.user.id;
  const gatewayUrl = process.env.GATEWAY_URL || "http://localhost:8080";
  const targetUrl = `${gatewayUrl}/api/v1/jobs/stream?access_token=${userId}`;

  try {
    const response = await fetch(targetUrl, {
      method: "GET",
      headers: {
        Accept: "text/event-stream",
      },
    });

    if (!response.ok) {
      return new Response(`Gateway stream error: ${response.statusText}`, {
        status: response.status,
      });
    }

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
        "X-Accel-Buffering": "no",
      },
    });
  } catch (error: unknown) {
    console.error("SSE Jobs Streaming Proxy Error:", error);
    const message =
      error instanceof Error
        ? error.message
        : "BFF SSE Jobs Stream Proxy Error";
    return new Response(JSON.stringify({ error: message }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }
}
