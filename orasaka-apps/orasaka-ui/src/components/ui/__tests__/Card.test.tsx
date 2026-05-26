import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
  CardFooter,
} from "@/components/ui/Card";

describe("Card", () => {
  it("renders with default className", () => {
    render(<Card data-testid="card">Content</Card>);
    const card = screen.getByTestId("card");
    expect(card).toBeInTheDocument();
    expect(card.className).toContain("rounded-xl");
    expect(card.className).toContain("shadow-sm");
  });

  it("merges custom className", () => {
    render(
      <Card className="my-custom" data-testid="card">
        Content
      </Card>,
    );
    const card = screen.getByTestId("card");
    expect(card.className).toContain("my-custom");
    expect(card.className).toContain("rounded-xl");
  });
});

describe("CardHeader", () => {
  it("renders children with spacing", () => {
    render(<CardHeader data-testid="header">Header</CardHeader>);
    const header = screen.getByTestId("header");
    expect(header).toBeInTheDocument();
    expect(header.className).toContain("p-6");
  });

  it("merges custom className", () => {
    render(
      <CardHeader className="extra" data-testid="header">
        Header
      </CardHeader>,
    );
    expect(screen.getByTestId("header").className).toContain("extra");
  });
});

describe("CardTitle", () => {
  it("renders as h3 with children", () => {
    render(<CardTitle>Title</CardTitle>);
    const heading = screen.getByRole("heading", { level: 3 });
    expect(heading).toBeInTheDocument();
    expect(heading).toHaveTextContent("Title");
  });

  it("applies default styling", () => {
    render(<CardTitle>Styled</CardTitle>);
    const heading = screen.getByRole("heading", { level: 3 });
    expect(heading.className).toContain("text-lg");
    expect(heading.className).toContain("font-semibold");
  });

  it("merges custom className", () => {
    render(<CardTitle className="custom-title">Title</CardTitle>);
    const heading = screen.getByRole("heading", { level: 3 });
    expect(heading.className).toContain("custom-title");
  });
});

describe("CardDescription", () => {
  it("renders as paragraph with muted text", () => {
    render(<CardDescription>Desc</CardDescription>);
    const p = screen.getByText("Desc");
    expect(p.tagName).toBe("P");
    expect(p.className).toContain("text-sm");
  });

  it("merges custom className", () => {
    render(<CardDescription className="extra">Desc</CardDescription>);
    expect(screen.getByText("Desc").className).toContain("extra");
  });
});

describe("CardContent", () => {
  it("renders with padding", () => {
    render(<CardContent data-testid="content">Body</CardContent>);
    const content = screen.getByTestId("content");
    expect(content).toBeInTheDocument();
    expect(content.className).toContain("p-6");
    expect(content.className).toContain("pt-0");
  });
});

describe("CardFooter", () => {
  it("renders with flex layout", () => {
    render(<CardFooter data-testid="footer">Footer</CardFooter>);
    const footer = screen.getByTestId("footer");
    expect(footer.className).toContain("flex");
    expect(footer.className).toContain("items-center");
  });
});
