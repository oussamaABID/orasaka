import * as React from "react";
import { Input } from "@/components/ui/Input";

import { SchemaField } from "@/services/interception.api";

/**
 * Properties required by the {@link InterceptionFormField} component.
 */
interface InterceptionFormFieldProps {
  /** The field meta schema definition. */
  field: SchemaField;
  /** Active input control value state. */
  value: string;
  /** Callback fired when control value changes. */
  onChange: (value: string) => void;
  /** Flag disabling the form element controls. */
  disabled?: boolean;
  /** Active locale language code identifier. */
  locale: string;
}

/**
 * InterceptionFormField renders a single form control based on the SchemaField definition.
 *
 * @param props - Component React properties.
 * @param props.field - The field meta schema definition.
 * @param props.value - Active input control value state.
 * @param props.onChange - Callback fired when control value changes.
 * @param props.disabled - Flag disabling the form element controls.
 * @param props.locale - Active locale language code identifier.
 * @returns The React component representing a field control.
 */
export function InterceptionFormField({
  field,
  value,
  onChange,
  disabled = false,
  locale,
}: InterceptionFormFieldProps) {
  return (
    <div className="space-y-2">
      <label className="text-sm font-medium text-zinc-700 dark:text-zinc-300 flex items-center justify-between">
        <span>{field.label}</span>
        {field.required && (
          <span className="text-xs text-red-500 font-normal">
            {locale === "fr" ? "* requis" : "* required"}
          </span>
        )}
      </label>

      {field.type === "text" && (
        <Input
          type="text"
          placeholder={field.placeholder}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          disabled={disabled}
          className="w-full bg-white/50 dark:bg-zinc-900/30"
        />
      )}

      {field.type === "textarea" && (
        <textarea
          placeholder={field.placeholder}
          rows={4}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          disabled={disabled}
          className="flex w-full rounded-xl border border-zinc-200/80 bg-white/50 px-3 py-2 text-sm backdrop-blur-sm transition-all duration-200 placeholder:text-zinc-400 focus:outline-none focus:ring-2 focus:ring-zinc-300 dark:focus-visible:ring-zinc-800 disabled:cursor-not-allowed disabled:opacity-50 dark:border-zinc-800/60 dark:bg-zinc-900/30 dark:placeholder:text-zinc-500 focus:ring-emerald-500 dark:text-zinc-100"
        />
      )}

      {field.type === "select" && (
        <select
          value={value}
          onChange={(e) => onChange(e.target.value)}
          disabled={disabled}
          className="flex h-10 w-full rounded-xl border border-zinc-200/80 bg-white/50 px-3 py-2 text-sm backdrop-blur-sm transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-zinc-300 dark:focus-visible:ring-zinc-800 disabled:cursor-not-allowed disabled:opacity-50 dark:border-zinc-800/60 dark:bg-zinc-900/30 focus:ring-emerald-500 dark:text-zinc-100"
        >
          {field.options?.map((opt) => (
            <option
              key={opt.value}
              value={opt.value}
              className="bg-white dark:bg-zinc-950 text-zinc-900 dark:text-zinc-100"
            >
              {opt.label}
            </option>
          ))}
        </select>
      )}
    </div>
  );
}
