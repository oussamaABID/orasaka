import ConnectorCatalogue from "@/features/automation/ConnectorCatalogue";
import LiveJobGrid from "@/features/automation/LiveJobGrid";
import "@/features/automation/automation.css";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Automations | Orasaka",
  description:
    "Enterprise automation hub — connect external tools, approve jobs, and monitor live task execution.",
};

/**
 * Automation dashboard page combining the Connector Catalogue and Live Job Grid.
 * This page follows the Jarvis 2026 HUD design language with glassmorphic cards
 * and ambient grid backgrounds.
 */
export default function AutomationPage() {
  return (
    <main className="automation-page ambient-grid">
      <div className="automation-page-container">
        <ConnectorCatalogue />
        <LiveJobGrid />
      </div>
    </main>
  );
}
