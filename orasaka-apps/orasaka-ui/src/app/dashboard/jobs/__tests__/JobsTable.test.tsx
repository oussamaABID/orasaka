import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import { JobsTable } from "@/app/dashboard/jobs/JobsTable";

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      jobs: {
        colId: "ID",
        colFeature: "Feature",
        colStatus: "Status",
        colCreated: "Created",
        colDuration: "Duration",
        colActions: "Actions",
        noJobs: "No jobs found",
        page: "Page",
        of: "of",
        rows: "rows",
        prev: "Previous",
        next: "Next",
        showing: "Showing",
        to: "to",
        entries: "entries",
      },
    },
    locale: "en",
  }),
}));

jest.mock("@/app/dashboard/jobs/JobRow", () => ({
  JobRow: ({ job }: { job: { id: string } }) => (
    <tr data-testid={`job-${job.id}`}><td>{job.id}</td></tr>
  ),
}));

jest.mock("@/app/dashboard/jobs/JobKpiBar", () => ({
  JobKpiBar: () => <div data-testid="kpi-bar" />,
}));

jest.mock("@/components/ui/Button", () => ({
  Button: ({ children, ...props }: { children: React.ReactNode } & React.ButtonHTMLAttributes<HTMLButtonElement>) => (
    <button {...props}>{children}</button>
  ),
}));

const defaultProps = {
  jobs: [
    { id: "j1", featureKey: "chat.text", status: "COMPLETED", createdAt: "2026-01-01T00:00:00Z" },
    { id: "j2", featureKey: "media.image", status: "FAILED", createdAt: "2026-01-01T00:01:00Z" },
  ] as never[],
  loading: false,
  totalPages: 2,
  totalElements: 20,
  currentPage: 0,
  pageSize: 10,
  onPageChange: jest.fn(),
  onPageSizeChange: jest.fn(),
  onOpenModal: jest.fn(),
  copiedId: null,
  onCopy: jest.fn(),
};

describe("JobsTable", () => {
  it("renders job rows", () => {
    render(<JobsTable {...defaultProps} />);
    expect(screen.getByTestId("job-j1")).toBeInTheDocument();
    expect(screen.getByTestId("job-j2")).toBeInTheDocument();
  });

  it("renders KPI bars for each job", () => {
    render(<JobsTable {...defaultProps} />);
    const bars = screen.getAllByTestId("kpi-bar");
    expect(bars).toHaveLength(2);
  });

  it("renders no jobs message when empty", () => {
    render(<JobsTable {...defaultProps} jobs={[]} />);
    expect(screen.getByText("No jobs found")).toBeInTheDocument();
  });
});
