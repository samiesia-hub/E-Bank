import { clsx } from "clsx";

// DESIGN.md badges: 4px radius, label-sm — yellow ONLY on dark is for nav,
// here pending uses dark text on soft yellow per mockup.
type BadgeTone = "info" | "success" | "pending" | "error" | "neutral";

const tones: Record<BadgeTone, string> = {
  info: "bg-[rgba(141,184,232,0.15)] text-navy",
  success: "bg-[rgba(59,139,139,0.12)] text-access-teal",
  pending: "bg-[rgba(247,201,72,0.2)] text-[#7A5E0B]",
  error: "bg-error-container text-error",
  neutral: "bg-surface-tint text-neutral-core",
};

export function Badge({
  tone = "neutral",
  className,
  ...props
}: React.HTMLAttributes<HTMLSpanElement> & { tone?: BadgeTone }) {
  return (
    <span
      className={clsx(
        "inline-flex items-center rounded-lg px-2 py-1 font-label-sm text-label-sm",
        tones[tone],
        className
      )}
      {...props}
    />
  );
}
