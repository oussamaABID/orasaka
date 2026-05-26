import { NextResponse } from "next/server";

/**
 * BFF proxy for self-service user registration.
 * Forwards the payload to the gateway's public register endpoint and
 * relays the response (201 / 400 / 409) back to the browser.
 *
 * No Authorization header is required — the gateway endpoint is public.
 */
export async function POST(req: Request) {
  const gatewayUrl = process.env.GATEWAY_URL || "http://localhost:8080";

  try {
    const body = await req.json();

    const response = await fetch(`${gatewayUrl}/api/v1/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

    const data = await response.json();
    return NextResponse.json(data, { status: response.status });
  } catch (error: unknown) {
    console.error("[BFF] Register proxy error:", error);
    const message =
      error instanceof Error
        ? error.message
        : "Registration failed — please try again.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
