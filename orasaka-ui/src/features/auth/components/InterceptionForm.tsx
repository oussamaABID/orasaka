"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { useSession } from "next-auth/react";
import { useQuery, useMutation } from "@tanstack/react-query";
import { Button } from "@/components/ui/Button";
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
  CardFooter,
} from "@/components/ui/Card";
import { Loader2, AlertCircle } from "lucide-react";
import { useTranslation } from "@/core/context/LocaleContext";
import { InterceptionFormField, SchemaField } from "./InterceptionFormField";

interface SchemaDescriptor {
  title: string;
  description: string;
  fields: SchemaField[];
}

interface InterceptionFormProps {
  schemaId: string;
  interceptionType?: string;
}

export function InterceptionForm({
  schemaId,
  interceptionType = schemaId,
}: InterceptionFormProps) {
  const router = useRouter();
  const { data: session, update } = useSession();
  const { locale } = useTranslation();
  const [formValues, setFormValues] = React.useState<Record<string, string>>(
    {},
  );
  const [errorMsg, setErrorMsg] = React.useState<string | null>(null);

  const {
    data: schema,
    isLoading,
    error,
  } = useQuery<SchemaDescriptor>({
    queryKey: ["interception-schema", schemaId],
    queryFn: async () => {
      const response = await fetch("/api/graphql", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          query: `query GetInterceptionSchema($schemaId: String!) { interceptionSchema(schemaId: $schemaId) }`,
          variables: { schemaId },
        }),
      });
      if (!response.ok) {
        throw new Error(
          `Failed to load interception schema: ${response.statusText}`,
        );
      }
      const resBody = await response.json();
      if (resBody.errors?.length > 0) {
        throw new Error(resBody.errors[0].message);
      }
      const rawSchema = resBody.data?.interceptionSchema;
      if (!rawSchema) throw new Error("No schema returned from server.");
      return JSON.parse(rawSchema);
    },
    staleTime: 5 * 60 * 1000,
  });

  React.useEffect(() => {
    if (schema?.fields) {
      const defaults: Record<string, string> = {};
      schema.fields.forEach((field) => {
        defaults[field.name] = field.defaultValue || "";
      });
      Promise.resolve().then(() => setFormValues(defaults));
    }
  }, [schema]);

  const resolveMutation = useMutation({
    mutationFn: async (responses: Record<string, string>) => {
      const response = await fetch("/api/graphql", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          query: `mutation ResolveInterception($interceptionType: String!, $schemaId: String!, $responses: Map!) {
            resolveInterception(interceptionType: $interceptionType, schemaId: $schemaId, responses: $responses)
          }`,
          variables: { interceptionType, schemaId, responses },
        }),
      });
      if (!response.ok) {
        throw new Error(`Failed to submit responses: ${response.statusText}`);
      }
      const resBody = await response.json();
      if (resBody.errors?.length > 0) {
        throw new Error(resBody.errors[0].message);
      }
      return resBody.data?.resolveInterception;
    },
    onSuccess: async () => {
      if (session?.user) {
        const updated = (session.user.activeInterceptions || []).filter(
          (id) => id !== schemaId,
        );
        await update({ activeInterceptions: updated });
        router.push("/");
        router.refresh();
      }
    },
    onError: (err: Error) => {
      setErrorMsg(err.message || "An error occurred. Please try again.");
    },
  });

  const handleInputChange = (fieldName: string, value: string) => {
    setFormValues((prev) => ({ ...prev, [fieldName]: value }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    const missing = schema?.fields?.find(
      (f) => f.required && !formValues[f.name]?.trim(),
    );
    if (missing) {
      setErrorMsg(
        locale === "fr"
          ? `Le champ "${missing.label}" est requis.`
          : `Field "${missing.label}" is required.`,
      );
      return;
    }
    resolveMutation.mutate(formValues);
  };

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[300px] space-y-4">
        <Loader2 className="w-8 h-8 animate-spin text-emerald-500" />
        <p className="text-zinc-500 text-sm dark:text-zinc-400">
          {locale === "fr"
            ? "Chargement du formulaire..."
            : "Loading form schema..."}
        </p>
      </div>
    );
  }

  if (error || !schema) {
    return (
      <Card className="max-w-md mx-auto border-red-500/30 bg-red-500/5 backdrop-blur-md">
        <CardHeader>
          <div className="flex items-center space-x-2 text-red-500">
            <AlertCircle className="w-6 h-6" />
            <CardTitle>
              {locale === "fr" ? "Erreur de Chargement" : "Loading Error"}
            </CardTitle>
          </div>
          <CardDescription>
            {locale === "fr"
              ? "Impossible de charger le formulaire de validation. Veuillez rafraîchir la page."
              : "Could not resolve the interception form schema. Please try refreshing."}
          </CardDescription>
        </CardHeader>
        <CardFooter>
          <Button
            variant="outline"
            onClick={() => window.location.reload()}
            className="w-full"
          >
            {locale === "fr" ? "Recharger" : "Retry"}
          </Button>
        </CardFooter>
      </Card>
    );
  }

  return (
    <Card className="max-w-xl mx-auto border-zinc-200/80 bg-white/70 shadow-2xl dark:border-zinc-800/60 dark:bg-zinc-950/60 backdrop-blur-xl transition-all duration-300">
      <form onSubmit={handleSubmit}>
        <CardHeader className="space-y-2">
          <CardTitle className="text-2xl font-bold tracking-tight bg-gradient-to-r from-zinc-900 to-zinc-600 dark:from-zinc-50 dark:to-zinc-400 bg-clip-text text-transparent">
            {schema.title}
          </CardTitle>
          <CardDescription className="text-zinc-500 dark:text-zinc-400">
            {schema.description}
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-6">
          {errorMsg && (
            <div className="flex items-start space-x-2 rounded-xl border border-red-500/20 bg-red-500/5 p-3.5 text-sm text-red-600 dark:text-red-400 animate-in fade-in slide-in-from-top-1">
              <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5" />
              <span>{errorMsg}</span>
            </div>
          )}

          <div className="space-y-4">
            {schema.fields.map((field) => (
              <InterceptionFormField
                key={field.name}
                field={field}
                value={formValues[field.name] || ""}
                onChange={(val) => handleInputChange(field.name, val)}
                disabled={resolveMutation.isPending}
                locale={locale}
              />
            ))}
          </div>
        </CardContent>

        <CardFooter>
          <Button
            type="submit"
            disabled={resolveMutation.isPending}
            className="w-full flex items-center justify-center space-x-2 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-medium shadow-lg hover:shadow-emerald-500/10 transition-all duration-300"
          >
            {resolveMutation.isPending ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>
                  {locale === "fr" ? "Validation..." : "Resolving..."}
                </span>
              </>
            ) : (
              <span>
                {locale === "fr"
                  ? "Soumettre les Préférences"
                  : "Submit Preferences"}
              </span>
            )}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}
