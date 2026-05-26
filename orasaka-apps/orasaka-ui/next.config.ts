import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    const gatewayUrl = process.env.GATEWAY_URL || "http://localhost:8080";
    return [
      {
        source: "/uploads/:path*",
        destination: `${gatewayUrl}/uploads/:path*`,
      },
      {
        source: "/graphql",
        destination: `${gatewayUrl}/graphql`,
      },
    ];
  },
};

export default nextConfig;
