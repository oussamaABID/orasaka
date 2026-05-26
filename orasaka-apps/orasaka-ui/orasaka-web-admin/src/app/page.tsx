export default function AdminDashboard() {
  return (
    <main
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        minHeight: "100vh",
        flexDirection: "column",
        gap: "1rem",
      }}
    >
      <h1
        style={{
          fontSize: "2rem",
          fontWeight: 700,
          fontFamily: "'Outfit', system-ui, sans-serif",
          background: "linear-gradient(135deg, hsl(200 90% 55%), hsl(260 80% 65%))",
          WebkitBackgroundClip: "text",
          WebkitTextFillColor: "transparent",
        }}
      >
        Orasaka SecOps Console
      </h1>
      <p style={{ color: "var(--text-secondary)", fontSize: "0.95rem" }}>
        Administration &amp; Security Operations — UX-02 Roadmap
      </p>
    </main>
  );
}
