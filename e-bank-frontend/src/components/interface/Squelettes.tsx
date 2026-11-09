/** Bloc de chargement animé (shimmer). */
export function SqueletteLigne({ className = "" }: { className?: string }) {
  return (
    <div
      aria-hidden="true"
      className={`animate-pulse rounded bg-surface-container ${className}`}
    />
  );
}

/** Carte skeleton pour les comptes. */
export function SqueletteCarteCompte() {
  return (
    <div className="rounded-xl border border-divider bg-surface-container-lowest p-space-lg" aria-hidden="true">
      <div className="flex items-start justify-between">
        <SqueletteLigne className="h-10 w-10 rounded-lg" />
        <SqueletteLigne className="h-5 w-16 rounded-full" />
      </div>
      <SqueletteLigne className="mt-space-lg h-4 w-28" />
      <SqueletteLigne className="mt-space-xs h-7 w-36" />
      <SqueletteLigne className="mt-space-md h-3 w-40" />
    </div>
  );
}

/** Ligne skeleton pour les transactions. */
export function SqueletteTransaction() {
  return (
    <li className="flex items-center gap-space-md px-space-lg py-space-md" aria-hidden="true">
      <SqueletteLigne className="h-10 w-10 shrink-0 rounded-full" />
      <div className="flex-1 space-y-space-xs">
        <SqueletteLigne className="h-4 w-40" />
        <SqueletteLigne className="h-3 w-24" />
      </div>
      <SqueletteLigne className="h-4 w-20 shrink-0" />
    </li>
  );
}

