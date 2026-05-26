"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { useSession } from "next-auth/react";
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
import { InterceptionFormField } from "./InterceptionFormField";
import {
  useInterceptionSchema,
  useResolveInterception,
} from "@/features/auth/hooks/useInterception";

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
  const { locale, t } = useTranslation();
  const [formValues, setFormValues] = React.useState<Record<string, string>>(
    {},
  );
  const [errorMsg, setErrorMsg] = React.useState<string | null>(null);

  const { schema, isLoading, error } = useInterceptionSchema(schemaId);

  React.useEffect(() => {
    if (schema?.fields) {
      const defaults: Record<string, string> = {};
      schema.fields.forEach((field) => {
        defaults[field.name] = field.defaultValue || "";
      });
      Promise.resolve().then(() => setFormValues(defaults));
    }
  }, [schema]);

  const { resolve, isPending } = useResolveInterception({
    interceptionType,
    schemaId,
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
    onError: (msg) => {
      setErrorMsg(msg);
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
      setErrorMsg(t.interception.fieldRequired(missing.label));
      return;
    }
    resolve(formValues);
  };

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[300px] space-y-4">
        <Loader2 className="w-8 h-8 animate-spin text-emerald-500" />
        <p className="text-zinc-500 text-sm dark:text-zinc-400">
          {t.interception.loadingSchema}
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
            <CardTitle>{t.interception.loadingError}</CardTitle>
          </div>
          <CardDescription>{t.interception.loadingErrorDesc}</CardDescription>
        </CardHeader>
        <CardFooter>
          <Button
            variant="outline"
            onClick={() => window.location.reload()}
            className="w-full"
          >
            {t.interception.retry}
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
                disabled={isPending}
                locale={locale}
              />
            ))}
          </div>
        </CardContent>

        <CardFooter>
          <Button
            type="submit"
            disabled={isPending}
            className="w-full flex items-center justify-center space-x-2 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-medium shadow-lg hover:shadow-emerald-500/10 transition-all duration-300"
          >
            {isPending ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>{t.interception.resolving}</span>
              </>
            ) : (
              <span>{t.interception.submitPreferences}</span>
            )}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}
