import Link from "next/link";
import { clsx } from "clsx";
import { PERIODES, type Periode } from "@/features/admin/periodes";

// Composant serveur : de simples liens, aucun état à gérer.
export function FiltrePeriode({ active }: { active: Periode }) {
    return (
        <nav
            aria-label="Période d'analyse"
            className="inline-flex gap-1 rounded-lg border border-divider bg-surface-container-lowest p-1"
        >
            {PERIODES.map(({ valeur, label }) => (
                <Link
                    key={valeur}
                    href={`/admin?periode=${valeur}`}
                    aria-current={valeur === active ? "true" : undefined}
                    className={clsx(
                        "flex h-8 items-center rounded-lg px-space-md text-label-md transition-colors",
                        valeur === active
                            ? "bg-primary-container font-semibold text-white"
                            : "text-neutral-core hover:bg-surface-tint"
                    )}
                >
                    {label}
                </Link>
            ))}
        </nav>
    );
}