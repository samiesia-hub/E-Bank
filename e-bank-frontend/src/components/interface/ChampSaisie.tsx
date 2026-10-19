import { clsx } from "clsx";
import type { InputHTMLAttributes } from "react";

// DESIGN.md: 40px height, 12px padding, 4px radius, focus ring rgba(55,111,178,0.2)
export interface PropsChampSaisie extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export function ChampSaisie({ label, error, hint, id, className, ...props }: PropsChampSaisie) {
  const inputId = id ?? props.name;
  return (
    <div className={clsx("flex flex-col gap-space-xs", className)}>
      {label && (
        <label htmlFor={inputId} className="font-label-md text-label-md text-on-surface">
          {label}
        </label>
      )}
      <input
        id={inputId}
        aria-invalid={!!error}
        aria-describedby={error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined}
        className={clsx(
          "h-10 rounded-lg border bg-surface-container-lowest px-3 font-body-md text-body-md text-navy placeholder:text-neutral-core/50",
          error
            ? "border-error"
            : "border-divider focus:border-primary-container focus:outline-2 focus:outline-[rgba(55,111,178,0.2)]"
        )}
        {...props}
      />
      {error ? (
        <p id={`${inputId}-error`} role="alert" className="font-body-sm text-body-sm text-error">{error}</p>
      ) : hint ? (
        <p id={`${inputId}-hint`} className="font-body-sm text-body-sm text-neutral-core">{hint}</p>
      ) : null}
    </div>
  );
}

export { ChampSaisie as Input, type PropsChampSaisie as InputProps }
