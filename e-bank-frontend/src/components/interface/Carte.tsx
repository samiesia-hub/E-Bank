import { clsx } from "clsx";

// DESIGN.md: white bg, 1px #E5E8EE border, 4px radius, 24px padding, NO shadow
export function Carte({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={clsx(
        "rounded-lg border border-divider bg-surface-container-lowest p-space-lg",
        className
      )}
      {...props}
    />
  );
}

// Highlight panel: tinted bg + 3px left accent
export function PanneauAccent({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={clsx(
        "rounded-lg border border-divider border-l-[3px] border-l-primary-container bg-surface-tint p-space-lg",
        className
      )}
      {...props}
    />
  );
}

export { Carte as Card, PanneauAccent as HighlightPanel }
