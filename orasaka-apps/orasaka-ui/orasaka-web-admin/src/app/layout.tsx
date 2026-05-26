import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Orasaka SecOps Console",
  description: "Orasaka Administration & Security Operations Console",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
