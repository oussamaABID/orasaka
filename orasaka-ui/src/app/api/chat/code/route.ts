import { NextRequest } from "next/server";
import { getServerSession } from "next-auth/next";
import { authOptions } from "@/app/api/auth/[...nextauth]/route";

export async function POST(req: NextRequest) {
  let body: Record<string, unknown>;
  try {
    body = (await req.json()) as Record<string, unknown>;
  } catch {
    return new Response("Invalid JSON payload", { status: 400 });
  }
  const prompt = body.prompt as string;
  const model = body.model as string | undefined;

  if (!prompt) {
    return new Response("Missing prompt parameter", { status: 400 });
  }

  const session = await getServerSession(authOptions);
  if (!session?.user || !session.user.id) {
    return new Response("Unauthorized: Missing active security context", {
      status: 401,
    });
  }

  const userId = session.user.id;
  const gatewayUrl = process.env.GATEWAY_URL || "http://localhost:8080";
  const targetUrl = `${gatewayUrl}/api/v1/chat/code`;

  try {
    const response = await fetch(targetUrl, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${userId}`,
        "Content-Type": "application/json",
        Accept: "text/event-stream",
      },
      body: JSON.stringify({ prompt, model }),
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
    console.error("SSE Code Streaming Proxy Error:", error);
    const message =
      error instanceof Error
        ? error.message
        : "BFF SSE Code Stream Proxy Error";
    return new Response(JSON.stringify({ error: message }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }
}
