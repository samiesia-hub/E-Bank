import { clsx } from "clsx";

type VarianteBouton = "primary" | "access" | "outline" | "ghost" | "danger";

const variants: Record<VarianteBouton, string> = {
  // DESIGN.md: Primary Transactional #376FB2 (primary-container)
  primary:
    "bg-primary-container text-white hover:bg-[#2C5A92] active:bg-[#214470]",
  // DESIGN.md: Access / Secure Login #3B8B8B — login & wire confirmation only
  access: "bg-access-teal text-white hover:bg-[#307272] active:bg-[#275E5E]",
  // DESIGN.md: Secondary / Outline
  outline:
    "border border-primary-container text-primary-container hover:bg-surface-tint",
  ghost: "text-primary-container hover:bg-surface-tint",
  danger: "bg-error text-white hover:brightness-95",
};

export interface PropsBouton
  extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: VarianteBouton;
}

export function Bouton({
  variant = "primary",
  className,
  ...props
}: PropsBouton) {
  return (
    <button
      className={clsx(
        "inline-flex h-10 items-center justify-center gap-2 rounded-lg px-4 font-semibold text-sm transition-colors",
        "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-container",
        "disabled:cursor-not-allowed disabled:opacity-50",
        variants[variant],
        className
      )}
      {...props}
    />
  );
}


export { Bouton as Button, type PropsBouton as ButtonProps, type VarianteBouton as ButtonVariant }
