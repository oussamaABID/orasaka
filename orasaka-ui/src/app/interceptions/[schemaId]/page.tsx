import { Metadata } from "next";
import { InterceptionForm } from "@/features/auth/components/InterceptionForm";

/**
 * Interception page metadata.
 */
export const metadata: Metadata = {
  title: "Required Action | Orasaka Workspace",
  description:
    "A required account setup or preference update form is pending completion.",
};

interface InterceptionPageProps {
  params: Promise<{ schemaId: string }>;
}

/**
 * Page routing component for active user interceptions.
 * Renders the dynamic layout form matching the target schema configuration.
 */
export default async function InterceptionPage({
  params,
}: InterceptionPageProps) {
  const { schemaId } = await params;

  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-6 bg-gradient-to-tr from-zinc-50 via-zinc-100 to-zinc-200 dark:from-zinc-950 dark:via-zinc-900 dark:to-zinc-950 transition-colors duration-300">
      <div className="w-full max-w-xl">
        <h1 className="sr-only">Required Action - {schemaId}</h1>
        <InterceptionForm schemaId={schemaId} />
      </div>
    </main>
  );
}
