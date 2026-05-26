import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import ConnectorCatalogue from "@/features/automation/ConnectorCatalogue";

describe("ConnectorCatalogue", () => {
  it("renders title", () => {
    render(<ConnectorCatalogue />);
    expect(screen.getByText("Integration Connectors")).toBeInTheDocument();
  });

  it("renders subtitle", () => {
    render(<ConnectorCatalogue />);
    expect(screen.getByText("Connect your enterprise tools for automated workflows")).toBeInTheDocument();
  });

  it("renders all connector names", () => {
    render(<ConnectorCatalogue />);
    expect(screen.getByText("Jira Cloud")).toBeInTheDocument();
    expect(screen.getByText("WhatsApp Business")).toBeInTheDocument();
    expect(screen.getByText("Messenger")).toBeInTheDocument();
    expect(screen.getByText("Slack")).toBeInTheDocument();
    expect(screen.getByText("Local CLI Agent")).toBeInTheDocument();
  });

  it("renders all connectors as inactive initially", () => {
    render(<ConnectorCatalogue />);
    const inactiveLabels = screen.getAllByText("Inactive");
    expect(inactiveLabels).toHaveLength(5);
  });

  it("renders connect buttons for all connectors", () => {
    render(<ConnectorCatalogue />);
    const connectBtns = screen.getAllByText("Connect");
    expect(connectBtns).toHaveLength(5);
  });

  it("toggles connector to connected", () => {
    render(<ConnectorCatalogue />);
    fireEvent.click(screen.getByLabelText("Toggle Jira Cloud connection"));
    expect(screen.getByText("Connected")).toBeInTheDocument();
    expect(screen.getByText("Disconnect")).toBeInTheDocument();
  });

  it("toggles back to disconnected", () => {
    render(<ConnectorCatalogue />);
    const btn = screen.getByLabelText("Toggle Slack connection");
    fireEvent.click(btn);
    expect(screen.getByText("Disconnect")).toBeInTheDocument();
    fireEvent.click(btn);
    expect(screen.queryByText("Disconnect")).toBeNull();
  });

  it("expands config panel on gear click", () => {
    render(<ConnectorCatalogue />);
    fireEvent.click(screen.getByLabelText("Configure Jira Cloud"));
    expect(screen.getByText("API Key")).toBeInTheDocument();
    expect(screen.getByText("Save Credentials")).toBeInTheDocument();
  });

  it("collapses config panel on second click", () => {
    render(<ConnectorCatalogue />);
    const configBtn = screen.getByLabelText("Configure Jira Cloud");
    fireEvent.click(configBtn);
    expect(screen.getByText("API Key")).toBeInTheDocument();
    fireEvent.click(configBtn);
    expect(screen.queryByText("API Key")).toBeNull();
  });

  it("renders connector descriptions", () => {
    render(<ConnectorCatalogue />);
    expect(screen.getByText(/Automated ticket creation/)).toBeInTheDocument();
    expect(screen.getByText(/Outbound notification streaming/)).toBeInTheDocument();
  });
});
